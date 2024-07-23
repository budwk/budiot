package com.budwk.app.network.modbus;

import com.budwk.app.access.constants.TopicConstant;
import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.network.DeviceGateway;
import com.budwk.app.access.network.config.DeviceGatewayConfiguration;
import com.budwk.app.access.protocol.message.codec.TcpMessage;
import com.intelligt.modbus.examples.SimpleSlaveTCP;
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.data.ModbusHoldingRegisters;
import com.intelligt.modbus.jlibmodbus.slave.ModbusSlave;
import com.intelligt.modbus.jlibmodbus.slave.ModbusSlaveFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import io.netty.util.NetUtil;
import lombok.extern.slf4j.Slf4j;
import org.nutz.lang.Strings;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * ExampleRTUOverTCP
 */
@Slf4j
public class ModbusSlaveDeviceGateway implements DeviceGateway {


    private String instanceId;
    private final MessageTransfer messageTransfer;
    private final DeviceGatewayConfiguration configuration;

    public ModbusSlaveDeviceGateway(DeviceGatewayConfiguration configuration, MessageTransfer messageTransfer) {
        this.configuration = configuration;
        this.messageTransfer = messageTransfer;
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.MODBUS;
    }

    @Override
    public void start() {
        Modbus.log().addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                log.info(record.getLevel().getName() + ": " + record.getMessage());
            }

            @Override
            public void flush() {
                //do nothing
            }

            @Override
            public void close() throws SecurityException {
                //do nothing
            }
        });
        Modbus.setLogLevel(Modbus.LogLevel.LEVEL_DEBUG);
        try {
            String host = Strings.sNull(configuration.getProperties().get("host"), "127.0.0.1");
            int port = Integer.parseInt(Strings.sNull(configuration.getProperties().get("port"), "9008"));
            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName(host));
            tcpParameters.setKeepAlive(true);
            tcpParameters.setPort(port);
            ModbusSlave slave;
            slave = ModbusSlaveFactory.createModbusSlaveTCP(tcpParameters);
            slave.setReadTimeout(0); // if not set default timeout is 1000ms, I think this must be set to 0 (infinitive timeout)

            SimpleSlaveTCP.MyOwnDataHolder dh = new SimpleSlaveTCP.MyOwnDataHolder();
            dh.addEventListener(new SimpleSlaveTCP.ModbusEventListener() {
                @Override
                public void onWriteToSingleCoil(int address, boolean value) {
                    System.out.print("onWriteToSingleCoil: address " + address + ", value " + value);
                }

                @Override
                public void onWriteToMultipleCoils(int address, int quantity, boolean[] values) {
                    System.out.print("onWriteToMultipleCoils: address " + address + ", quantity " + quantity);
                }

                @Override
                public void onWriteToSingleHoldingRegister(int address, int value) {
                    System.out.print("onWriteToSingleHoldingRegister: address " + address + ", value " + value);
                }

                @Override
                public void onWriteToMultipleHoldingRegisters(int address, int quantity, int[] values) {
                    System.out.print("onWriteToMultipleHoldingRegisters: address " + address + ", quantity " + quantity);
                }
            });

            slave.setDataHolder(dh);
            ModbusHoldingRegisters hr = new ModbusHoldingRegisters(10);
            hr.set(0, 12345);
            slave.getDataHolder().setHoldingRegisters(hr);
            slave.setServerAddress(1);
            slave.listen();
            log.info("Modbus Slave TCP started at {}:{}", host, port);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private TcpMessage newTcpMessage(byte[] bytes) {
        TcpMessage tcpMessage = new TcpMessage(bytes, configuration.getProtocolCode());
        tcpMessage.setPayloadType(Strings.sBlank(configuration.getProperties().get("payloadType"), "hex"));
        return tcpMessage;
    }

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
