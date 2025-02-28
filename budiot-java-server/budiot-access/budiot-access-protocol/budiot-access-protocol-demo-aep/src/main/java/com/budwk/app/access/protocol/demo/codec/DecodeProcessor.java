package com.budwk.app.access.protocol.demo.codec;

import com.budwk.app.access.protocol.codec.CacheStore;
import com.budwk.app.access.protocol.codec.DeviceOperator;
import com.budwk.app.access.protocol.codec.context.DecodeContext;
import com.budwk.app.access.protocol.codec.exception.MessageCodecException;
import com.budwk.app.access.protocol.codec.result.DecodeResult;
import com.budwk.app.access.protocol.codec.result.DefaultDecodeResult;
import com.budwk.app.access.protocol.codec.result.DefaultResponseResult;
import com.budwk.app.access.protocol.demo.AepDemoProtocol;
import com.budwk.app.access.protocol.demo.constants.CacheKeyConstant;
import com.budwk.app.access.protocol.enums.MeaningState;
import com.budwk.app.access.protocol.message.DeviceDataMessage;
import com.budwk.app.access.protocol.message.DeviceMessage;
import com.budwk.app.access.protocol.message.DeviceResponseMessage;
import com.budwk.app.access.protocol.message.codec.EncodedMessage;
import com.budwk.app.access.protocol.message.codec.TcpMessage;
import com.budwk.app.access.protocol.utils.ByteConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
public abstract class DecodeProcessor {
    private final DecodeContext context;
    private final EncodedMessage message;
    private DeviceOperator deviceOperator;

    public DecodeProcessor(DecodeContext context) {
        this.context = context;
        this.message = context.getMessage();
    }

    public DecodeResult process() {
        NutMap reportData = NutMap.WRAP(new String(message.getPayload()));
        String IMEI = reportData.getString("IMEI");
        deviceOperator = context.getDeviceByField("imei", IMEI);
        if (Lang.isEmpty(deviceOperator)) {
            throw new MessageCodecException("IMEI" + IMEI + "设备不存在");
        }
        NutMap data = reportData.getAs("payload", NutMap.class);
        String serviceData = data.getString("APPdata");
        byte[] payload = Base64.getDecoder().decode(serviceData);
        log.info("[金卡NB]设备上报的报文为: {}", ByteConvertUtil.bytesToHex(payload));
        NutMap nutMap = NutMap.NEW();//JinkaNbParseUtil.JK_wlw_jb(payload);
        String deviceNo = nutMap.getString("meterIdString");
        String dataString = nutMap.getString("dataString");
        int dataTye = nutMap.getInt("dataType");
        switch (dataTye) {
            case 99://注册帧
                this.replyToDeviceRegEnd();
                DeviceDataMessage reportMessage = new DeviceDataMessage();
                reportMessage.setMessageId("register");
                reportMessage.setDeviceId(deviceOperator.getDeviceId());
                reportMessage.setTimestamp(System.currentTimeMillis());
                DefaultDecodeResult decodeReportResult = new DefaultDecodeResult(deviceOperator.getDeviceId(), List.of(reportMessage), false, MeaningState.REGISTER.text());
                return decodeReportResult;
            case 1://数据上报
                //;分割的多条数据上报
                String[] dataStrings = Strings.splitIgnoreBlank(dataString, ";");
                List<DeviceMessage> deviceMessages = new ArrayList<>();
                for (String string : dataStrings) {
                   //todo
                }
                return new DefaultDecodeResult(deviceOperator.getDeviceId(), deviceMessages, true, MeaningState.ACTIVE_REPORT.text());
//            case 2://普通关阀
//                return processCmdRespValve("VALVE_CONTROL", "PTClose", dataString, deviceOperator.getDeviceId());
//            case 3://强制关阀
//                return processCmdRespValve("VALVE_CONTROL", "QZClose", dataString, deviceOperator.getDeviceId());
//            case 4://协议开阀
//                return processCmdRespValve("VALVE_CONTROL", "XYOpen", dataString, deviceOperator.getDeviceId());

        }
        return null;
    }

    private DecodeResult processCmdRespValve(String method, String type, String data, String deviceId) {
        CacheStore cacheStore = context.getCacheStore(AepDemoProtocol.PROTOCOL_CODE + ":" + deviceId);
        String commandId = null;
        NutMap map = NutMap.NEW();//JinkaNbParseUtil.parseStatusData(data);
        DeviceResponseMessage responseMessage = new DeviceResponseMessage();
        responseMessage.setDeviceId(deviceId);
        responseMessage.setCommandCode(method);
        responseMessage.setSuccess(true);
        responseMessage.setTimestamp(System.currentTimeMillis());
        responseMessage.setProperties(map);
        responseMessage.addProperty("type", type);
        if (null != cacheStore) {
            commandId = cacheStore.get(CacheKeyConstant.CMD_SEND_ID + ":" + responseMessage.getCommandCode(), String.class);
        }
        DefaultResponseResult cmdRespResult = new DefaultResponseResult("commandId", responseMessage, MeaningState.VALVE_CONTROL.text());
        cmdRespResult.setCommandCode(method);
        return cmdRespResult;
    }

    public void replyToDeviceRegEnd() {
        String replyString = "JKGKJ_LINK_OK";
        byte[] replyBytes = ByteConvertUtil.ascii2Bytes(replyString);
        log.info("[金卡NB]发送消息 ==> {}, hex ==> {}", replyString, ByteConvertUtil.bytesToHex(replyBytes));
        replyToDevice(replyBytes,MeaningState.REGISTER_BACK.text(), deviceOperator);
    }

    public void replyToDeviceHex(String hex, String meaning) {
        byte[] replyBytes = ByteConvertUtil.hexToBytes(hex);
        log.info("[金卡NB]发送消息 ==> hex ==> {}", hex);
        TcpMessage tcpMessage = new TcpMessage(replyBytes, AepDemoProtocol.PROTOCOL_CODE);
        context.send(tcpMessage);
    }

    public void replyToDevice(byte[] replyBytes,String meaning, DeviceOperator deviceOperator) {
        TcpMessage tcpMessage = new TcpMessage(replyBytes, AepDemoProtocol.PROTOCOL_CODE);
        tcpMessage.setMeaning(meaning);
        context.send(tcpMessage);
    }
}
