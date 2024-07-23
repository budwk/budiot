package com.budwk.app.access.network.modbus;

import com.budwk.app.access.constants.TopicConstant;
import com.budwk.app.access.enums.TransportType;
import com.budwk.app.access.message.MessageTransfer;
import com.budwk.app.access.network.DeviceGateway;
import com.budwk.app.access.network.config.DeviceGatewayConfiguration;
import com.budwk.app.access.protocol.message.codec.TcpMessage;
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.msg.request.ReadHoldingRegistersRequest;
import com.intelligt.modbus.jlibmodbus.msg.response.ReadHoldingRegistersResponse;
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
public class ModbusMasterDeviceGateway implements DeviceGateway {


    private String instanceId;
    private final MessageTransfer messageTransfer;
    private final DeviceGatewayConfiguration configuration;

    public ModbusMasterDeviceGateway(DeviceGatewayConfiguration configuration, MessageTransfer messageTransfer) {
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
            int port = Integer.parseInt(Strings.sNull(configuration.getProperties().get("port"), "9009"));
            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName(host));
            tcpParameters.setKeepAlive(true);
            tcpParameters.setPort(port);

            //if you would like to set connection parameters separately,
            // you should use another method: createModbusMasterTCP(String host, int port, boolean keepAlive);
            ModbusMaster m = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            Modbus.setAutoIncrementTransactionId(true);

            int slaveId = 1;
            int offset = 0;
            int quantity = 10;

            try {
                // since 1.2.8
                if (!m.isConnected()) {
                    m.connect();
                    log.info("Modbus Master TCP started at {}:{}", host, port);

                }

                // at next string we receive ten registers from a slave with id of 1 at offset of 0.
                int[] registerValues = m.readHoldingRegisters(slaveId, offset, quantity);

                for (int value : registerValues) {
                    log.info("Address: " + offset++ + ", Value: " + value);
                }
                // also since 1.2.8.4 you can create your own request and process it with the master
                offset = 0;
                ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest();
                request.setServerAddress(1);
                request.setStartAddress(offset);
                request.setTransactionId(0);
                ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) m.processRequest(request);
                // you can get either int[] containing register values or byte[] containing raw bytes.
                for (int value : response.getRegisters()) {
                    log.info("Address: " + offset++ + ", Value: " + value);
                }
            } catch (ModbusProtocolException e) {
                e.printStackTrace();
            } catch (ModbusNumberException e) {
                e.printStackTrace();
            } catch (ModbusIOException e) {
                e.printStackTrace();
            }

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
