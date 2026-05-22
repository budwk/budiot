package com.budwk.sp.sys.services.impl;

import com.budwk.sp.msg.dto.MsgVerifyCodeCheckDTO;
import com.budwk.sp.msg.dto.MsgVerifyCodeSendDTO;
import com.budwk.sp.msg.providers.IMsgVerifyCodeProvider;
import com.budwk.sp.starter.cache.service.WkCacheService;
import com.budwk.sp.starter.common.constant.GlobalConstant;
import com.budwk.sp.starter.common.exception.BaseException;
import com.budwk.sp.sys.entity.Sys_user;
import com.budwk.sp.sys.entity.Sys_user_security;
import com.budwk.sp.sys.services.SysUserSecurityService;
import com.budwk.sp.sys.services.SysUserService;
import com.budwk.sp.sys.services.AuthValidateService;
import com.budwk.sp.starter.common.constant.RedisConstant;
import com.budwk.sp.starter.common.result.Result;
import com.wf.captcha.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.nutz.lang.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现
 *
 * @author wizzer@qq.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthValidateServiceImpl implements AuthValidateService {
    @Autowired
    private WkCacheService wkCacheService;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysUserSecurityService sysUserSecurityService;
    @DubboReference(interfaceClass = IMsgVerifyCodeProvider.class, check = false, lazy = true, retries = 0)
    private IMsgVerifyCodeProvider msgVerifyCodeProvider;

    private static final String CAPTCHA_PREFIX = RedisConstant.UCENTER_CAPTCHA;
    private static final String SMSCODE_PREFIX = RedisConstant.UCENTER_SMSCODE;
    private static final String EMAILCODE_PREFIX = RedisConstant.UCENTER_EMAILCODE;

    @Override
    public Result<Map<String, Object>> getCaptcha() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        Sys_user_security security = sysUserSecurityService.getWithCache();

        if (security != null && Boolean.TRUE.equals(security.getCaptchaHasEnabled())) {
            String text;
            String base64;
            int captchaType = security.getCaptchaType() != null ? security.getCaptchaType() : 0;

            switch (captchaType) {
                case 1:
                    SpecCaptcha captcha1 = new SpecCaptcha(120, 40, 4);
                    captcha1.setCharType(2); // 纯数字
                    text = captcha1.text();
                    base64 = captcha1.toBase64();
                    break;
                case 2:
                    SpecCaptcha captcha2 = new SpecCaptcha(120, 40, 4);
                    captcha2.setCharType(3); // 纯字母
                    text = captcha2.text();
                    base64 = captcha2.toBase64();
                    break;
                case 3:
                    SpecCaptcha captcha3 = new SpecCaptcha(120, 40, 4);
                    captcha3.setCharType(1); // 字母数字混合
                    text = captcha3.text();
                    base64 = captcha3.toBase64();
                    break;
                case 4:
                    ChineseCaptcha captcha4 = new ChineseCaptcha(120, 40, 4);
                    text = captcha4.text();
                    base64 = captcha4.toBase64();
                    break;
                case 11:
                    GifCaptcha captcha11 = new GifCaptcha(120, 40, 4);
                    captcha11.setCharType(2);
                    text = captcha11.text();
                    base64 = captcha11.toBase64();
                    break;
                case 22:
                    GifCaptcha captcha22 = new GifCaptcha(120, 40, 4);
                    captcha22.setCharType(3);
                    text = captcha22.text();
                    base64 = captcha22.toBase64();
                    break;
                case 33:
                    GifCaptcha captcha33 = new GifCaptcha(120, 40, 4);
                    captcha33.setCharType(1);
                    text = captcha33.text();
                    base64 = captcha33.toBase64();
                    break;
                case 0:
                default:
                    // ArithmeticCaptcha 依赖 Nashorn ScriptEngine, Java 15+ 已移除
                    // 自行实现算术验证码
                    String[] arithmeticResult = generateArithmeticCaptcha();
                    text = arithmeticResult[0];
                    base64 = arithmeticResult[1];
                    break;
            }

            wkCacheService.setCache(CAPTCHA_PREFIX + uuid, text, 180, TimeUnit.SECONDS);

            Map<String, Object> data = new HashMap<>();
            data.put("key", uuid);
            data.put("code", base64);
            data.put("captchaHasEnabled", true);
            return Result.data(data);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("key", "");
        data.put("code", "");
        data.put("captchaHasEnabled", false);
        return Result.data(data);
    }

    @Override
    public void getSmsCode(String mobile, String type, String tenantId) throws BaseException {
        if (msgVerifyCodeProvider == null) {
            throw new BaseException("短信服务不可用");
        }
        MsgVerifyCodeSendDTO dto = new MsgVerifyCodeSendDTO();
        dto.setTenantId(tenantId);
        dto.setReceiver(mobile);
        dto.setBizScene(type);
        dto.setLength(4);
        msgVerifyCodeProvider.sendSmsCode(dto);
    }

    @Override
    public void getEmailCode(String subject, String loginname, String email,String tenantId) throws BaseException {
        if (msgVerifyCodeProvider == null) {
            throw new BaseException("邮件服务不可用");
        }
        MsgVerifyCodeSendDTO dto = new MsgVerifyCodeSendDTO();
        dto.setTenantId(tenantId);
        dto.setReceiver(email);
        dto.setLoginname(loginname);
        dto.setSubject(subject);
        dto.setLength(6);
        msgVerifyCodeProvider.sendEmailCode(dto);
    }

    @Override
    public void checkCode(String key, String code) throws BaseException {
        String codeFromRedis = Strings.sNull(wkCacheService.getCache(CAPTCHA_PREFIX + key));

        if (!StringUtils.hasText(code)) {
            throw new BaseException("请输入验证码");
        }
        if (!StringUtils.hasText(codeFromRedis)) {
            throw new BaseException("验证码已过期");
        }
        if (!code.equalsIgnoreCase(codeFromRedis)) {
            throw new BaseException("验证码不正确");
        }

        wkCacheService.deleteCache(CAPTCHA_PREFIX + key);
    }

    @Override
    public void checkSMSCode(String mobile, String code,String tenantId) throws BaseException {
        if (msgVerifyCodeProvider == null) {
            throw new BaseException("短信服务不可用");
        }
        MsgVerifyCodeCheckDTO dto = new MsgVerifyCodeCheckDTO();
        dto.setTenantId(tenantId);
        dto.setReceiver(mobile);
        dto.setCode(code);
        msgVerifyCodeProvider.checkCode(dto);
    }

    @Override
    public void checkEmailCode(String loginname, String code,String tenantId) throws BaseException {
        Sys_user user = sysUserService.getUserByLoginname(loginname);
        if (user == null || Strings.isBlank(user.getEmail())) {
            throw new BaseException("用户邮箱不存在");
        }
        if (msgVerifyCodeProvider == null) {
            throw new BaseException("邮件服务不可用");
        }
        MsgVerifyCodeCheckDTO dto = new MsgVerifyCodeCheckDTO();
        dto.setTenantId(tenantId);
        dto.setReceiver(user.getEmail());
        dto.setCode(code);
        msgVerifyCodeProvider.checkCode(dto);
    }

    private String generateNumberCode(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 生成算术验证码(替代 ArithmeticCaptcha, 兼容 Java 15+)
     *
     * @return [0]=计算结果, [1]=base64图片
     */
    private String[] generateArithmeticCaptcha() {
        Random random = new Random();
        int a = random.nextInt(20) + 1;
        int b = random.nextInt(20) + 1;
        String[] ops = {"+", "-", "×"};
        String op = ops[random.nextInt(3)];
        int result;
        switch (op) {
            case "-":
                if (a < b) { int t = a; a = b; b = t; }
                result = a - b;
                break;
            case "×":
                a = random.nextInt(9) + 1;
                b = random.nextInt(9) + 1;
                result = a * b;
                break;
            default:
                result = a + b;
                break;
        }
        String expression = a + " " + op + " " + b + " = ?";
        String text = String.valueOf(result);

        int width = 120, height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 背景
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, width, height);
        // 干扰线
        for (int i = 0; i < 4; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
        }
        // 文字
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.setColor(new Color(40 + random.nextInt(60), 40 + random.nextInt(60), 40 + random.nextInt(60)));
        FontMetrics fm = g.getFontMetrics();
        int x = (width - fm.stringWidth(expression)) / 2;
        int y = (height - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(expression, x, y);
        g.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
            return new String[]{text, base64};
        } catch (Exception e) {
            log.error("生成算术验证码失败", e);
            return new String[]{"0", ""};
        }
    }
}
