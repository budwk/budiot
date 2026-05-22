package com.budwk.sp.device.test.tcp;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TCP 表具模拟器。
 *
 * <p>默认行为：
 * <ul>
 *     <li>每 60 秒上报一次表具读数</li>
 *     <li>每 300 秒上报一次事件状态</li>
 *     <li>地址域使用 7 字节设备编号（14 位十六进制）</li>
 * </ul>
 *
 * <p>运行示例：
 * <pre>
 * java -jar wk-device-test-2.0.0-SNAPSHOT.jar \
 *   --host=127.0.0.1 \
 *   --port=7001 \
 *   --device-code=01020304050607 \
 *   --report-interval-seconds=60 \
 *   --event-interval-seconds=300
 * </pre>
 */
public class TcpClientDemo {
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final byte FRAME_START = 0x68;
    private static final byte FRAME_END = 0x16;
    private static final byte CMD_REPORT_DATA = (byte) 0x91;
    private static final byte CMD_REPORT_EVENT = (byte) 0x92;
    private static final String IDENTIFIER_CURRENT_GAS = "901F";
    private static final String IDENTIFIER_HISTORY_GAS = "902F";

    private final DemoConfig config;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final AtomicLong currentGasCenti = new AtomicLong();
    private final AtomicLong historyGasCenti = new AtomicLong();
    private final AtomicInteger eventBits = new AtomicInteger(0x01);
    private volatile Socket socket;

    public TcpClientDemo(DemoConfig config) {
        this.config = config;
        this.currentGasCenti.set(toCenti(config.initialReading()));
        this.historyGasCenti.set(toCenti(config.initialReading()));
    }

    public static void main(String[] args) {
        DemoConfig config = DemoConfig.fromArgs(args);
        TcpClientDemo demo = new TcpClientDemo(config);
        demo.start();
    }

    private void start() {
        log("启动 TCP 表具模拟器: " + config);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "tcp-client-demo-shutdown"));
        scheduler.scheduleAtFixedRate(
                this::safeSendReading,
                1,
                Math.max(config.reportIntervalSeconds(), 1),
                TimeUnit.SECONDS
        );
        scheduler.scheduleAtFixedRate(
                this::safeSendEvent,
                5,
                Math.max(config.eventIntervalSeconds(), 1),
                TimeUnit.SECONDS
        );
    }

    private void stop() {
        scheduler.shutdownNow();
        closeSocket();
        log("TCP 表具模拟器已停止");
    }

    private void safeSendReading() {
        try {
            sendReadingFrame();
        } catch (Exception e) {
            log("读数上报失败: " + e.getMessage());
            closeSocket();
        }
    }

    private void safeSendEvent() {
        try {
            sendEventFrame();
        } catch (Exception e) {
            log("事件上报失败: " + e.getMessage());
            closeSocket();
        }
    }

    private synchronized void sendReadingFrame() throws IOException {
        long nextCurrent = currentGasCenti.addAndGet(toCenti(config.readingStep()));
        long nextHistory = historyGasCenti.addAndGet(toCenti(config.historyStep()));
        byte[] dataDomain = concat(
                hexToBytes(IDENTIFIER_CURRENT_GAS),
                toLittleEndianBcd(nextCurrent, 4),
                hexToBytes(IDENTIFIER_HISTORY_GAS),
                toLittleEndianBcd(nextHistory, 4)
        );
        byte[] frame = buildFrame(CMD_REPORT_DATA, dataDomain);
        sendFrame(frame, "周期读数", Map.of(
                "currentGas", centiToDecimal(nextCurrent),
                "historyGas", centiToDecimal(nextHistory)
        ));
    }

    private synchronized void sendEventFrame() throws IOException {
        int bits = eventBits.getAndUpdate(current -> current == 0x01 ? 0x02 : 0x01);
        byte[] frame = buildFrame(CMD_REPORT_EVENT, new byte[]{(byte) bits});
        sendFrame(frame, "事件上报", Map.of(
                "eventBits", String.format("0x%02X", bits),
                "eventName", bits == 0x01 ? "magnetic_interference" : "low_battery"
        ));
    }

    private void sendFrame(byte[] frame, String scene, Map<String, Object> extra) throws IOException {
        Socket activeSocket = ensureConnected();
        OutputStream outputStream = activeSocket.getOutputStream();
        outputStream.write(frame);
        outputStream.flush();

        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("scene", scene);
        logData.put("host", config.host());
        logData.put("port", config.port());
        logData.put("deviceCode", config.deviceCode());
        logData.put("hex", bytesToHex(frame));
        logData.putAll(extra);
        log("发送成功 " + logData);
    }

    private Socket ensureConnected() throws IOException {
        if (socket != null && socket.isConnected() && !socket.isClosed()) {
            return socket;
        }
        closeSocket();
        Socket newSocket = new Socket();
        newSocket.connect(new InetSocketAddress(config.host(), config.port()), config.connectTimeoutMs());
        socket = newSocket;
        log("TCP 已连接 " + config.host() + ":" + config.port());
        return newSocket;
    }

    private void closeSocket() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            } finally {
                socket = null;
            }
        }
    }

    private byte[] buildFrame(byte command, byte[] dataDomain) {
        byte[] address = reverse(hexToBytes(config.deviceCode()));
        byte[] frame = new byte[1 + address.length + 1 + 1 + dataDomain.length + 1 + 1];
        int index = 0;
        frame[index++] = FRAME_START;
        System.arraycopy(address, 0, frame, index, address.length);
        index += address.length;
        frame[index++] = command;
        frame[index++] = (byte) (dataDomain.length & 0xFF);
        System.arraycopy(dataDomain, 0, frame, index, dataDomain.length);
        index += dataDomain.length;
        frame[index++] = checksum(frame, 0, index);
        frame[index] = FRAME_END;
        return frame;
    }

    private static long toCenti(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    private static BigDecimal centiToDecimal(long value) {
        return BigDecimal.valueOf(value, 2);
    }

    private static byte checksum(byte[] data, int from, int toExclusive) {
        int sum = 0;
        for (int i = from; i < toExclusive; i++) {
            sum += data[i] & 0xFF;
        }
        return (byte) (sum & 0xFF);
    }

    private static byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] array : arrays) {
            total += array.length;
        }
        byte[] merged = new byte[total];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, merged, offset, array.length);
            offset += array.length;
        }
        return merged;
    }

    private static byte[] reverse(byte[] source) {
        byte[] reversed = new byte[source.length];
        for (int i = 0; i < source.length; i++) {
            reversed[i] = source[source.length - 1 - i];
        }
        return reversed;
    }

    private static byte[] toLittleEndianBcd(long value, int byteLength) {
        String digits = Long.toString(Math.max(value, 0));
        int expectedLength = byteLength * 2;
        if (digits.length() > expectedLength) {
            throw new IllegalArgumentException("BCD 数值超出 " + byteLength + " 字节范围: " + value);
        }
        String padded = "0".repeat(expectedLength - digits.length()) + digits;
        byte[] bytes = new byte[byteLength];
        for (int i = 0; i < byteLength; i++) {
            int start = padded.length() - ((i + 1) * 2);
            int high = Character.digit(padded.charAt(start), 10);
            int low = Character.digit(padded.charAt(start + 1), 10);
            bytes[i] = (byte) ((high << 4) | low);
        }
        return bytes;
    }

    private static byte[] hexToBytes(String hex) {
        String normalized = Objects.requireNonNull(hex, "hex").replaceAll("\\s+", "").toUpperCase();
        if ((normalized.length() & 1) == 1) {
            throw new IllegalArgumentException("十六进制长度必须为偶数: " + hex);
        }
        byte[] bytes = new byte[normalized.length() / 2];
        for (int i = 0; i < normalized.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(normalized.substring(i, i + 2), 16);
        }
        return bytes;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02X", value & 0xFF));
        }
        return builder.toString();
    }

    private static void log(String message) {
        System.out.printf("[%s] %s%n", LocalDateTime.now().format(TS_FORMATTER), message);
    }

    record DemoConfig(
            String host,
            int port,
            String deviceCode,
            int reportIntervalSeconds,
            int eventIntervalSeconds,
            int connectTimeoutMs,
            BigDecimal initialReading,
            BigDecimal readingStep,
            BigDecimal historyStep
    ) {
        static DemoConfig fromArgs(String[] args) {
            Map<String, String> values = new LinkedHashMap<>();
            for (String arg : args) {
                if (arg == null || !arg.startsWith("--") || !arg.contains("=")) {
                    continue;
                }
                int index = arg.indexOf('=');
                values.put(arg.substring(2, index), arg.substring(index + 1));
            }
            String host = values.getOrDefault("host", "127.0.0.1");
            int port = Integer.parseInt(values.getOrDefault("port", "7701"));
            String deviceCode = values.getOrDefault("device-code", "01020304050607")
                    .replaceAll("\\s+", "")
                    .toUpperCase();
            if (!deviceCode.matches("[0-9A-F]{14}")) {
                throw new IllegalArgumentException("device-code 必须是 14 位十六进制，例如 01020304050607");
            }
            return new DemoConfig(
                    host,
                    port,
                    deviceCode,
                    Integer.parseInt(values.getOrDefault("report-interval-seconds", "60")),
                    Integer.parseInt(values.getOrDefault("event-interval-seconds", "300")),
                    Integer.parseInt(values.getOrDefault("connect-timeout-ms", "3000")),
                    new BigDecimal(values.getOrDefault("initial-reading", "12345.67")),
                    new BigDecimal(values.getOrDefault("reading-step", "0.12")),
                    new BigDecimal(values.getOrDefault("history-step", "0.08"))
            );
        }
    }
}
