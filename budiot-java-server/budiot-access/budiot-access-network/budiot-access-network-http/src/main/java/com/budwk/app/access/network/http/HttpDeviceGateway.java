package com.budwk.app.access.network.http;

import com.budwk.app.access.constants.TopicConstant;
import com.budwk.app.access.message.Message;
import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.network.DeviceGateway;
import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.network.config.DeviceGatewayConfiguration;
import com.budwk.app.access.protocol.message.codec.EncodedMessage;
import com.budwk.app.access.protocol.message.codec.HttpMessage;
import com.budwk.starter.rocketmq.enums.ConsumeMode;
import com.budwk.starter.rocketmq.enums.MessageModel;
import io.netty.util.NetUtil;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import org.nutz.castor.Castors;
import org.nutz.lang.Strings;
import org.nutz.lang.util.NutMap;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP 网关
 */
@Slf4j
public class HttpDeviceGateway implements DeviceGateway {


    private static final Vertx vertx = Vertx.vertx();

    /**
     * 存储的路由上下文
     */
    private static final Map<String, RoutingContext> rtxs = new ConcurrentHashMap<>();
    /**
     * 存储的处理器。
     */
    private static final Map<String, Handler<RoutingContext>> handlers = new ConcurrentHashMap<>();

    private String instanceId;
    private final MessageTransfer messageTransfer;
    private final DeviceGatewayConfiguration configuration;
    private final NutMap properties;

    public HttpDeviceGateway(DeviceGatewayConfiguration configuration, MessageTransfer messageTransfer) {
        this.messageTransfer = messageTransfer;
        this.configuration = configuration;
        this.properties = NutMap.WRAP(configuration.getProperties());
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.HTTP;
    }

    @Override
    public void start() {
        // 启动一个 HTTP 服务
        String platform = properties.getString("platform", "osd");
        // 从配置文件里获取配置的路径
        String path = properties.getString("path", String.format("/%s/%s/*", configuration.getId(), platform));
        // 添加到处理器中去。
        // 这里的处理方法其实都是一个，之所以用 Map 存储而不是用 List 只存 Path 是为了方便通过 path 获取
        handlers.put(path, this::messageHandle);
        this.createServer()
                .requestHandler(buildRouter())
                .listen()
                .onSuccess(event -> log.info("http server started at {}", event.actualPort()))
                .onFailure(throwable -> log.error("http server error", throwable));

    }

    private Router buildRouter() {
        Router router = Router.router(vertx);
        // 路由
        router.post("/*")
                .handler(BodyHandler.create()) // 解析请求体
                .handler(rtx -> { // 请求的处理
                    // 获取请求路径
                    String path = rtx.request().uri();
                    // 根据路径匹配处理器。如果是用 List存的 path，这里就是判断 List 中是否存在这个 path
                    // 如果存在直接调用 messageHandle
                    Handler<RoutingContext> handler = handlers.get(path);
                    if (null != handler) {
                        handler.handle(rtx);
                    } else {
                        rtx.response().setStatusCode(404);
                        rtx.end("NOT FOUND");
                    }
                });

        return router;
    }

    /**
     * 处理收到的数据，很简单，就是包装成 HttpMessage 推送到 Protocol 去
     *
     * @param rtx 路由上下文
     */
    private void messageHandle(RoutingContext rtx) {
        // 接入的平台
        String platform = properties.getString("platform", "other");
        // 是否等待响应
        boolean waitResponse = properties.getBoolean("waitResponse", false);
        // 等待的超时时间
        int waitTimeout = properties.getInt("waitTimeout", 30);
        log.debug("request path {}", rtx.request().uri());
        if (waitResponse) {
            // 如果需要等待，那就挂起请求。记录下这个请求的路由上下文
            String rtxId = Integer.toHexString(rtx.hashCode());
            rtxs.put(rtxId, rtx);
            startCmdListener();
            rtx.put("rtxId", rtxId);
            // We send a error response after timeout
            long tid = rtx.vertx().setTimer(waitTimeout * 1000L, t -> {
                rtx.fail(504);
                rtxs.remove(rtxId);
            });

            rtx.addBodyEndHandler(event -> {
                rtxs.remove(rtxId);
                rtx.vertx().cancelTimer(tid);
            });
        }
        // 包装成 HttpMessage
        HttpMessage httpMessage = new HttpMessage(configuration.getProtocolCode());
        httpMessage.setUrl(rtx.request().uri());
        httpMessage.setPath(rtx.request().path());
        httpMessage.setMessageId(rtx.get("rtxId"));
        MultiMap headers = rtx.request().headers();
        Map<String, String> hd = new HashMap<>();
        headers.forEach(hd::put);
        httpMessage.setHeader(hd);
        hd = new HashMap<>();
        rtx.queryParams().forEach(hd::put);
        httpMessage.setQuery(hd);
        Buffer body = rtx.getBody();
        if (null != body) {
            httpMessage.setPayload(body.getBytes());
        }
        httpMessage.setPlatform(platform);
        Message<HttpMessage> message =
                new Message<>(TopicConstant.DEVICE_DATA_UP, httpMessage);
        message.addHeader("sessionId", rtx.get("rtxId"));
        if (waitResponse) {
            // 如果要等待响应，那么不要结束请求，增加回复的地址
            message.setFrom(getReplyAddress());
//            rtx.next();
        } else {
            rtx.end("OK");
        }
        // 发送到 Protocol
        messageTransfer.publish(message);
    }

    private HttpServer createServer() {

        HttpServerOptions options = new HttpServerOptions();
        options.setHost(properties.getString("host", "0.0.0.0"));
        options.setPort(properties.getInt("port", 0));
        return vertx.createHttpServer(options);
    }


    /**
     * 下行的指令监听
     */
    private void startCmdListener() {
        // 那些挂起的请求，如果有后续响应，就会到这里来
        messageTransfer.subscribe(this.configuration.getId(), getReplyAddress(), "*", MessageModel.BROADCASTING, ConsumeMode.ORDERLY, message -> {
            // 获取会话信息，将数据发送给设备
            Object body = message.getBody();
            EncodedMessage encodedMessage = Castors.me().castTo(body, EncodedMessage.class);
            byte[] bytes = encodedMessage.getPayload();
            RoutingContext rtx = rtxs.get(message.getHeader("sessionId"));
            NutMap result = NutMap.NEW()
                    .setv("result", 0)
                    .setv("deviceId", message.getHeader("deviceId"))
                    .setv("commandId", message.getHeader("commandId"));
            log.debug("发送指令 commandId: {}", message.getHeader("commandId"));
            if (null != rtx) {
                if (rtx.response().closed()) {
                    result.setv("result", -1).setv("msg", "未找到设备会话信息");
                    replyCmdSendResult(message.getFrom(), result, message.getHeaders());
                    return;
                }
                rtx.response().end(Buffer.buffer(bytes))
                        .onSuccess(event -> replyCmdSendResult(message.getFrom(), result, message.getHeaders()))
                        .onFailure(event -> replyCmdSendResult(message.getFrom(), result.setv("result", -1).setv("msg", "发送数据到设备失败"), message.getHeaders()));
            }
        });
    }


    /**
     * 告诉发指令给我的“地址”，指令发送的结果。相当于回信
     *
     * @param replyAddress 回信地址
     * @param result       结果
     * @param headers      头
     */
    private void replyCmdSendResult(String replyAddress, NutMap result, Map<String, String> headers) {
        if (Strings.isBlank(replyAddress)) {
            return;
        }
        Message<NutMap> reply = new Message<>(replyAddress, result);
        reply.setSender(getInstanceId());
        reply.getHeaders().putAll(headers);
        messageTransfer.publish(reply);
    }


    /**
     * 获取回复地址
     *
     * @return
     */
    private String getReplyAddress() {
        return String.format(this.configuration.getId() + "_%s_%s", TopicConstant.DEVICE_CMD_DOWN, getInstanceId());
    }

    public String getInstanceId() {
        if (Strings.isNotBlank(instanceId)) {
            return instanceId;
        }
        instanceId = Strings.sBlank(this.configuration.getInstanceId());
        if (Strings.isBlank(instanceId)) {
            String id = configuration.getId() + "_" + NetUtil.LOCALHOST + ManagementFactory.getRuntimeMXBean().getName();
            instanceId = Integer.toHexString(id.hashCode());
        }
        return instanceId;
    }
}
