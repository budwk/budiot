package com.budwk.sp.device.support;

public final class DeviceProtocolScriptSamples {
    private DeviceProtocolScriptSamples() {
    }

    public static String sampleScript() {
        return """
                /**
                 * 物联网设备协议解析脚本 (GraalJS)
                 * 约定：
                 * 1. Java 传入变量名：'input' (类型 Map<String, Object>)
                 * 2. 脚本最后一行返回执行结果对象
                 */

                const Utils = {
                    // 字节数组转十六进制字符串
                    bytesToHex: (bytes) => Array.from(bytes).map(b => (b & 0xFF).toString(16).padStart(2, '0')).join('').toUpperCase(),

                    // 十六进制字符串转字节数组
                    hexToBytes: (hex) => new Uint8Array(hex.match(/.{1,2}/g).map(byte => parseInt(byte, 16))),

                    // BCD 转十进制 (处理小端序，例如 [0x56, 0x34] -> 3456)
                    bcdToDecimal: (bytes) => {
                        let res = 0;
                        for (let i = bytes.length - 1; i >= 0; i--) {
                            let high = (bytes[i] >> 4) & 0x0F;
                            let low = bytes[i] & 0x0F;
                            res = res * 100 + (high * 10 + low);
                        }
                        return res;
                    },

                    // 计算校验和
                    calculateCS: (data) => data.reduce((a, b) => a + b, 0) & 0xFF
                };

                 /**
                  * 协议解析逻辑封装
                  */
                 const ProtocolHandler = {

                    // --- 1. 上行解析 (数据上报 + 事件) ---
                    decode: function(dataMap) {
                        // 获取原始字节流 (假设 Java 传入字段名为 payload)
                        const rawPayload = Array.from(dataMap.get("payload"));

                        // 如果有解密需求，此处调用注入的加密工具
                        // const decrypted = CryptoUtil.decrypt(Utils.bytesToHex(rawPayload));
                        // const payload = Array.from(Utils.hexToBytes(decrypted));
                        const payload = rawPayload;

                        const result = {
                            properties: [], // 存储属性数据
                            events: [],     // 存储事件/告警
                            replies: []     // 存储自动回复报文
                        };

                        const cmdCode = payload[8]; // 命令码
                        const dataLen = payload[9]; // 数据域长度
                        const dataDomain = payload.slice(10, 10 + dataLen);

                        // A. 处理数据上报 (多条数据)
                        if (cmdCode === 0x91) {
                            const itemSize = 6; // 假设每条数据标识符2b + 数据4b
                            for (let i = 0; i + itemSize <= dataLen; i += itemSize) {
                                const item = dataDomain.slice(i, i + itemSize);
                                const id = Utils.bytesToHex(item.slice(0, 2));
                                const val = Utils.bcdToDecimal(item.slice(2, 6)) / 100.0;

                                let identifier = "unknown";
                                if (id === "901F") identifier = "current_gas";      // 当前气量
                                if (id === "902F") identifier = "history_gas";      // 历史气量

                                result.properties.push({
                                    identifier: identifier,
                                    value: val,
                                    time: Date.now()
                                });
                            }
                        }

                        // B. 处理事件解析 (告警状态位)
                        // 假设第10字节是状态位
                        const statusByte = payload[10];
                        if ((statusByte & 0x01) === 0x01) {
                            result.events.push({
                                identifier: "magnetic_interference",
                                value: 1,
                                time: Date.now(),
                                msg: "检测到强磁干扰"
                            });
                        }
                        if ((statusByte & 0x02) === 0x02) {
                            result.events.push({
                                identifier: "low_battery",
                                value: 1,
                                time: Date.now(),
                                msg: "电池电压过低"
                            });
                        }

                        // C. 自动生成应答报文 (ACK)
                        result.replies.push({
                            payload: "6801020304050607810016" // 示例回复
                        });

                        return result;
                    },

                    // --- 2. 下行构建 (指令下发) ---
                    encode: function(dataMap) {
                        // dataMap 中包含下发的业务参数
                        const method = dataMap.get("method");
                        const addr = dataMap.get("address"); // 示例地址 "01020304050607"

                        let frame = [0x68];
                        frame.push(...Array.from(Utils.hexToBytes(addr)).reverse()); // 地址域

                        let ctrlCode = 0x00;
                        let dataDomain = [];

                        if (method === "CLOSE_VALVE") {
                            ctrlCode = 0x04;
                            dataDomain = [0x17, 0x90, 0x55]; // 关阀指令
                        } else if (method === "READ_DATA") {
                            ctrlCode = 0x01;
                            dataDomain = [0x1F, 0x90];       // 读气量指令
                        }

                        frame.push(ctrlCode, dataDomain.length);
                        frame.push(...dataDomain);
                        frame.push(Utils.calculateCS(frame), 0x16);

                        // 返回包含构建好的字节流
                        return {
                            properties: [],
                            events: [],
                            replies: [{
                                payload: Utils.bytesToHex(frame)
                            }]
                        };
                     }
                 };

                 /**
                  * 网关侧身份预解析入口（可选）。
                  *
                  * 用途：
                  * 1. 在 gateway 收到原始报文后，先于正式 decode 执行
                  * 2. 用于从不同协议帧中提取设备身份标识，帮助系统在入站阶段直接定位设备
                  *
                  * 返回约定：
                  * - identityType: DEVICE_CODE / IMEI / ICCID
                  * - identityValue: 对应的身份值
                  * - 也可以直接返回 deviceCode / imei / iccid 字段，平台会自动归一化
                  */
                 function resolveIdentity(input) {
                     const payload = Array.from(input.get("payload"));
                     if (!payload || payload.length < 8 || payload[0] !== 0x68) {
                         return null;
                     }

                     // 示例协议：第 1~7 字节为倒序地址域，反转后得到 14 位设备编号
                     const addressBytes = payload.slice(1, 8).reverse();
                     const deviceCode = Utils.bytesToHex(addressBytes);

                     return {
                         identityType: "DEVICE_CODE",
                         identityValue: deviceCode,
                         deviceCode: deviceCode,
                         productKey: input.get("productKey") || ""
                     };
                 }

                 /**
                  * 脚本执行主入口
                  */
                function run(input) {
                    try {
                        // 区分是上行数据包还是下行指令构建
                        // 假设 Java 在 input 中传入 "direction": "U" 或 "D"
                        const direction = input.get("direction");

                        if (direction === "U") {
                            return ProtocolHandler.decode(input);
                        } else {
                            return ProtocolHandler.encode(input);
                        }
                    } catch (e) {
                        // 返回异常信息，适配 DTO
                        return {
                            properties: [],
                            events: [{ identifier: "script_error", value: e.message, time: Date.now() }],
                            replies: []
                        };
                    }
                }

                // 必须执行函数并返回结果给 Java context.eval()
                run(input);
                """;
    }

    public static String sampleInputJson() {
        return """
                {
                  "messageId": "debug-msg-001",
                  "tenantId": "tenant-demo",
                  "productId": "product-demo",
                  "productKey": "thermo01",
                  "productName": "温湿度终端",
                  "deviceId": "device-demo",
                  "deviceCode": "DEV-1001",
                  "gatewayNodeId": "gateway-node-01",
                  "protocolId": "protocol-demo",
                  "protocolCode": "hex-demo",
                  "direction": "U",
                  "receivedAt": 1760000000000,
                  "headers": {
                    "networkProtocol": "TCP"
                  },
                  "networkProtocol": "TCP",
                  "endpoint": "127.0.0.1:9000",
                  "sourceIp": "127.0.0.1",
                  "payload": "68010203040506079106901F1234902F56780116",
                  "payloadText": "68010203040506079106901F1234902F56780116"
                }
                """;
    }
}
