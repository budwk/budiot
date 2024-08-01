<template>
    <div style="padding: 0 5px">
        <el-row :gutter="10" class="mb8">
            <el-col :span="24" style="text-align: right">
                <div style="display: inline-flex">
                    <el-form :model="tableData" ref="queryRef" :inline="true" label-width="100px">
                        <el-form-item label="平台版本号：" class="label-font-weight">
                            <el-tag type="info">
                                {{ tableData.version }}
                            </el-tag>
                        </el-form-item>
                        <el-form-item label="是否启用">
                            <el-switch
                                v-permission="['iot.device.product.dtuparam']"
                                v-model="tableData.enabled"
                                inline-prompt
                                active-text="启用"
                                inactive-text="未启用"
                                @change="enabledChange"
                            />
                        </el-form-item>
                    </el-form>
                    <el-button plain type="success" @click="save" v-permission="['iot.device.product.dtuparam']">保存配置 </el-button>
                    <el-button plain type="info" @click="resetFrom" v-permission="['iot.device.product.dtuparam']">重置表单 </el-button>
                    <el-button plain type="primary" @click="exportTxt" v-permission="['iot.device.product.dtuparam']">导出配置 </el-button>
                    <el-button plain type="primary" v-permission="['iot.device.product.dtuparam']">导入配置 </el-button>
                </div>
            </el-col>
        </el-row>
        <el-row>
            <el-form
                ref="createRef"
                :model="formData"
                :rules="formRules"
                label-width="220px"
                label-position="left"
                style="width: 100%; padding: 0 5px"
            >
                <el-tabs v-model="activeName" tabPosition="left" style="width: 100%">
                    <el-tab-pane label="基本信息" name="0" style="padding: 0 20px">
                        <el-form-item label="模式：" prop="passon">
                            <el-radio-group v-model="formData.passon" @change="passonChange">
                                <el-radio :value="1">透传</el-radio>
                                <el-radio :value="0">单片机控制</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="是否加设备识别码IMEI：" prop="plate" v-if="formData.passon == 1">
                            <el-radio-group v-model="formData.plate">
                                <el-radio :value="0">不加</el-radio>
                                <el-radio :value="1">加</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="报文转换（bin -- hex）：" prop="convert">
                            <el-radio-group v-model="formData.convert">
                                <el-radio :value="1">转换</el-radio>
                                <el-radio :value="0">不换</el-radio>
                            </el-radio-group>
                            <span class="tip">提示: 如果启用数据流模板，这里选择“不换”</span>
                        </el-form-item>
                        <el-form-item label="首次登陆服务器发送注册信息：" prop="reg" class="label-font-weight">
                            <el-radio-group v-model="register.type" @change="registerChange">
                                <el-radio :value="1">发送{csq:rssi,imei:imei,iccid:iccid,ver:Version}</el-radio>
                                <el-radio :value="2">发送HEX报文"13,12345,12345"</el-radio>
                                <el-radio :value="0">不发</el-radio>
                                <el-radio :value="3">自定义</el-radio>
                                <el-radio :value="4">顺序生成</el-radio>
                            </el-radio-group>
                            <div v-if="register.type == 3" >
                                自定义：
                                <el-input v-model="register.data" style="width: 150px" />
                            </div>
                            <div v-if="register.type == 4" >
                                前缀：
                                <el-input v-model="register.prefix" style="width: 150px" />
                                后缀：
                                <el-input v-model="register.postfix" style="width: 150px" />
                            </div>
                        </el-form-item>
                        <el-form-item label="每分钟最大串口流量(Byte)：" prop="flow">
                            <el-input v-model="formData.flow" style="width: 150px" />
                            <span class="tip">提示: 空为不启用</span>
                        </el-form-item>
                        <el-form-item label="是否启用自动更新：" prop="fota">
                            <el-radio-group v-model="formData.fota">
                                <el-radio :value="0">否</el-radio>
                                <el-radio :value="1">是</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="串口分帧超时：" prop="uartReadTime">
                            <el-input-number v-model="formData.uartReadTime" :min="10" :max="2000" />
                            <span class="tip">提示: 单位ms 默认25ms，范围10-2000</span>
                        </el-form-item>
                        <el-form-item label="电源模式：" prop="pwrmod">
                            <el-radio-group v-model="formData.pwrmod">
                                <el-radio value="normal">正常</el-radio>
                                <el-radio value="energy">节能</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="配置密码：" prop="password">
                            <el-input v-model="formData.password" style="width: 150px" />
                            <span class="tip">提示: 只允许包含字母数字_</span>
                        </el-form-item>
                        <el-form-item label="网络分帧超时：" prop="netReadTimes">
                            <el-input-number v-model="formData.netReadTime" :min="10" :max="2000" />
                            <span class="tip">提示: 单位ms 默认0ms，范围10-2000</span>
                        </el-form-item>
                        <el-form-item label="日志输出：" prop="nolog">
                            <el-radio-group v-model="formData.nolog">
                                <el-radio value="0">关闭</el-radio>
                                <el-radio value="1">开启</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="是否打开RNDIS：" prop="isRndis">
                            <el-radio-group v-model="formData.isRndis">
                                <el-radio value="0">关闭</el-radio>
                                <el-radio value="1">开启</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="是否打开ipv6：" prop="isipv6">
                            <el-radio-group v-model="formData.isipv6">
                                <el-radio value="0">关闭</el-radio>
                                <el-radio value="1">开启</el-radio>
                            </el-radio-group>
                            <span class="tip">提示: 目前仅780E/700E/600E/780EX/780EG支持该选项，724/720系列/820/600ug等模组不支持</span>
                        </el-form-item>
                        <el-form-item label="是否守护全部网络通道：" prop="webProtect">
                            <el-radio-group v-model="formData.webProtect">
                                <el-radio value="0">个别</el-radio>
                                <el-radio value="1">全部</el-radio>
                            </el-radio-group>
                            <span class="tip">提示: 服务器连接断开时5分钟会重启，守护全部通道有一个断开就重启，守护个别通道在个别通道断开会重启</span>
                        </el-form-item>
                    </el-tab-pane>
                    <el-tab-pane label="串口参数" name="1" style="padding: 0 20px">
                        <el-tabs v-model="activeComm" style="width: 100%">
                            <el-tab-pane v-for="(obj, ind) in formData.uconf" :label="'串口' + (ind + 1)" :name="ind" :key="'comm_' + ind">
                                <el-radio-group v-model="commValues[ind]" @change="commChange">
                                    <el-radio :value="1">启用</el-radio>
                                    <el-radio :value="0">不启用</el-radio>
                                </el-radio-group>
                                <div  v-if="commValues[ind] == 1" style="margin-top: 10px;">
                                    <el-form-item label="波特率：">
                                        <el-select v-model="formData.uconf[ind][1]" placeholder="请选择" style="width: 150px">
                                            <el-option label="300" value="300" />
                                            <el-option label="600" value="600" />
                                            <el-option label="1200" value="1200" />
                                            <el-option label="2400" value="2400" />
                                            <el-option label="4800" value="4800" />
                                            <el-option label="9600" value="9600" />
                                            <el-option label="14400" value="14400" />
                                            <el-option label="19200" value="19200" />
                                            <el-option label="28800" value="28800" />
                                            <el-option label="38400" value="38400" />
                                            <el-option label="57600" value="57600" />
                                            <el-option label="115200" value="115200" />
                                            <el-option label="230400" value="230400" />
                                            <el-option label="460800" value="460800" />
                                            <el-option label="921600" value="921600" />
                                        </el-select>
                                        <span class="tip">提示: 单位bps</span>
                                    
                                    </el-form-item>
                                    <el-form-item label="数据位：">
                                        <el-radio-group v-model="formData.uconf[ind][2]">
                                            <el-radio :value="8">8</el-radio>
                                            <el-radio :value="7">7</el-radio>
                                        </el-radio-group>
                                    </el-form-item>
                                    <el-form-item label="校验位：">
                                        <el-radio-group v-model="formData.uconf[ind][3]">
                                            <el-radio :value="0">uart.PAR_EVEN</el-radio>
                                            <el-radio :value="1">uart.PAR_ODD</el-radio>
                                            <el-radio :value="2">uart.PAR_NONE</el-radio>
                                        </el-radio-group>
                                    </el-form-item>
                                    <el-form-item label="停止位：">
                                        <el-radio-group v-model="formData.uconf[ind][4]">
                                            <el-radio :value="0">1</el-radio>
                                            <el-radio :value="2">2</el-radio>
                                        </el-radio-group>
                                    </el-form-item>
                                    <el-form-item label="485DIR：">
                                        <el-select v-model="formData.uconf[ind][5]" placeholder="请选择" style="width: 150px">
                                            <el-option v-for="(n,i) in numberArr" :label="n" :value="n" :key="i"/>
                                        </el-select>
                                        <span class="tip">提示: 485方向控制GPIO</span>
                                    </el-form-item>
                                    <el-form-item label="485oe转向延迟时间：">
                                        <el-input v-model="formData.uconf[ind][6]" style="width: 150px" />
                                        <span class="tip">提示: 单位US</span>
                                    </el-form-item>
                                </div>
                            </el-tab-pane>
                        </el-tabs>
                    </el-tab-pane>
                    <el-tab-pane label="网络通道" name="2" style="padding: 0 20px">
                        <el-tabs v-model="activeNet" style="width: 100%">
                            <el-tab-pane v-for="(obj, ind) in formData.conf" :label="'通道' + (ind + 1)" :name="ind" :key="'net_' + ind">
                                <el-radio-group v-model="netValues[ind].enabled" @change="netChange">
                                    <el-radio :value="1">启用</el-radio>
                                    <el-radio :value="0">不启用</el-radio>
                                </el-radio-group>
                                <div v-if="netValues[ind].enabled == 1" style="margin-top: 10px;">
                                    <el-form-item label="通道类型：">
                                        <el-radio-group v-model="netValues[ind].type" @change="netTypeChange(ind)">
                                            <el-radio value="http">HTTP</el-radio>
                                            <el-radio value="socket">SOCKET</el-radio>
                                            <el-radio value="mqtt">MQTT</el-radio>
                                            <el-radio value="onenet">OneNET</el-radio>
                                            <el-radio value="aliyun">阿里云</el-radio>
                                            <el-radio value="bduyun">百度云</el-radio>
                                            <el-radio value="txuny">腾讯云</el-radio>
                                            <el-radio value="txyunnew">腾讯云(新)</el-radio>
                                            <el-radio value="onenetnew">OneNET(新)</el-radio>
                                            <el-radio value="thingscloud">LuatCloud</el-radio>
                                        </el-radio-group>
                                    </el-form-item>
                                </div>
                                <div v-if="netValues[ind].type == 'http'" style="margin-top: 10px;">
                                    <el-form-item label="HTTP绑定串口ID：">
                                        <el-radio-group v-model="formData.conf[ind][1]">
                                            <el-radio value="1">1</el-radio>
                                            <el-radio value="2">2</el-radio>
                                        </el-radio-group>
                                    </el-form-item>
                                    <el-form-item label="method：">
                                        <el-radio-group v-model="formData.conf[ind][2]">
                                            <el-radio value="get">get</el-radio>
                                            <el-radio value="post">post</el-radio>
                                        </el-radio-group>
                                    </el-form-item>
                                    <el-form-item label="url：">
                                        <el-input v-model="formData.conf[ind][3]" style="width: 380px" />
                                        <span class="tip">提示: HTTP请求的地址和参数，参数需要自己urllencode处理</span>
                                    </el-form-item>
                                    <el-form-item label="timeout：">
                                        <el-input v-model="formData.conf[ind][4]" style="width: 150px" />
                                        <span class="tip">提示: HTTP请求最长等待时间，超过这个时间，HTTP将返回</span>
                                    </el-form-item>
                                    <el-form-item label="请求类型：">
                                        <el-radio-group v-model="formData.conf[ind][5]">
                                            <el-radio value="1">body</el-radio>
                                            <el-radio value="0">param</el-radio>
                                        </el-radio-group>
                                        <span class="tip">提示: 串口数据类型，默认body</span>
                                    </el-form-item>
                                    <el-form-item label="type：">
                                        <el-radio-group v-model="formData.conf[ind][6]">
                                            <el-radio value="1">urlencode</el-radio>
                                            <el-radio value="2">json</el-radio>
                                            <el-radio value="3">stream</el-radio>
                                        </el-radio-group>
                                        <span class="tip">提示: body的提交类型</span>
                                    </el-form-item>
                                    <el-form-item label="basic：">
                                        <el-input v-model="formData.conf[ind][7]" style="width: 150px" />
                                        <span class="tip">提示: HTTP的BASIC验证，注意账号密码之间用 : 连接</span>
                                    </el-form-item>
                                    <el-form-item label="head：">
                                        <el-input v-model="formData.conf[ind][8]" style="width: 150px" />
                                        <span class="tip">提示: HTTP请求报文头部字符串</span>
                                    </el-form-item>
                                    <el-form-item label="返回数据过滤：" class="label-font-weight">
                                        <el-checkbox v-model="formData.conf[ind][9]" :true-value="1" :false-value="0">code</el-checkbox>
                                        <el-checkbox v-model="formData.conf[ind][10]" :true-value="1" :false-value="0">head</el-checkbox>
                                        <el-checkbox v-model="formData.conf[ind][11]" :true-value="1" :false-value="0">body</el-checkbox>
                                    </el-form-item>
                                </div>
                                <div v-if="netValues[ind].type == 'socket'" style="margin-top: 10px;">
                                    <el-form-item label="协议：">
                                        <el-radio-group v-model="formData.conf[ind][0]">
                                            <el-radio value="tcp">TCP协议</el-radio>
                                            <el-radio value="udp">UDP协议</el-radio>
                                        </el-radio-group>
                                    </el-form-item>
                                    <el-form-item label="心跳包：" class="label-font-weight">
                                        <el-radio-group v-model="netValues[ind].socket.heartbeat">
                                            <el-radio :value="0">自定义</el-radio>
                                            <el-radio :value="1">顺序生成</el-radio>
                                        </el-radio-group>
                                        <div v-if="netValues[ind].socket.heartbeat == 0" style="margin-left: 20px;">
                                            <el-input v-model="netValues[ind].socket.data" style="width: 150px" />
                                        </div>
                                        <div v-if="netValues[ind].socket.heartbeat == 1" style="margin-left: 20px;">
                                            前缀： <el-input v-model="netValues[ind].socket.prefix" style="width: 150px" />
                                            后缀： <el-input v-model="netValues[ind].socket.postfix" style="width: 150px" />
                                        </div>
                                    </el-form-item>
                                    <el-form-item label="心跳间隔时间：">
                                        <el-input-number v-model="formData.conf[ind][2]" :min="0"></el-input-number>
                                        <span class="tip">提示: 单位秒，0为关闭心跳包，建议60-300</span>
                                    </el-form-item>
                                    <el-form-item label="socket的地址或域名：">
                                        <el-input v-model="formData.conf[ind][3]" style="width: 280px"></el-input>
                                    </el-form-item>
                                    <el-form-item label="socket服务器的端口号：">
                                        <el-input-number v-model="formData.conf[ind][4]" :min="1" style="width: 150px"></el-input-number>
                                        <span class="tip">提示: 端口号范围：1~65536</span>
                                    </el-form-item>
                                    <el-form-item label="通道捆绑的串口ID：">
                                        <el-radio-group v-model="formData.conf[ind][5]">
                                            <el-radio :value="1">1</el-radio>
                                            <el-radio :value="2">2</el-radio>
                                            <el-radio :value="3">3</el-radio>
                                        </el-radio-group>
                                    </el-form-item>
                                    <el-form-item label="被动上报间隔：">
                                        <el-input v-model="formData.conf[ind][6]" style="width: 150px"></el-input>
                                        <span class="tip">提示: 单位秒，非被动模式留空 范围：1~65535</span>
                                    </el-form-item>
                                    <el-form-item label="被动采集间隔：">
                                        <el-input v-model="formData.conf[ind][7]" style="width: 150px"></el-input>
                                        <span class="tip">提示: 单位秒，非被动模式留空 范围：1~15</span>
                                    </el-form-item>
                                    <el-form-item label="被动采集间隔：">
                                        <el-input v-model="formData.conf[ind][8]" :min="1" style="width: 150px"></el-input>
                                        <span class="tip">提示: 单位秒，主动采集任务间隔时间，配合自动采集任务使用</span>
                                    </el-form-item>
                                    <el-form-item label="SSL：">
                                        <el-radio-group v-model="formData.conf[ind][9]">
                                            <el-radio value="ssl">启用</el-radio>
                                            <el-radio value="">不启用</el-radio>
                                        </el-radio-group>
                                    </el-form-item>
                                </div>
                                <div v-if="netValues[ind].type == 'mqtt'" style="margin-top: 10px;">
                                    <el-form-item label="MQTT心跳包的间隔：">
                                        <el-input-number v-model="formData.conf[ind][1]" style="width: 150px"></el-input-number>
                                        <span class="tip">提示: 单位秒，默认300</span>
                                    </el-form-item>
                                    <el-form-item label="自动采集任务间隔时间：">
                                        <el-input-number v-model="formData.conf[ind][2]" style="width: 150px"></el-input-number>
                                        <span class="tip">提示: 单位秒，MQTT接收超时时间,配合自动采集任务使用</span>
                                    </el-form-item>
                                    <el-form-item label="MQTT的地址或域名：">
                                        <el-input v-model="formData.conf[ind][3]" style="width: 280px"></el-input>
                                    </el-form-item>
                                    <el-form-item label="MQTT服务器的端口号：">
                                        <el-input-number v-model="formData.conf[ind][4]" :min="1" style="width: 150px"></el-input-number>
                                        <span class="tip">提示: 端口号范围：1~65536</span>
                                    </el-form-item>
                                    <el-form-item label="MQTT的登陆账号：">
                                        <el-input v-model="formData.conf[ind][5]" style="width: 280px"></el-input>
                                    </el-form-item>
                                    <el-form-item label="MQTT的登陆密码：">
                                        <el-input v-model="formData.conf[ind][6]" style="width: 280px"></el-input>
                                    </el-form-item>
                                    <el-form-item label="MQTT保存会话标志位：">
                                        <el-radio-group v-model="formData.conf[ind][7]">
                                            <el-radio :value="0">持久会话</el-radio>
                                            <el-radio :value="1">离线自动销毁</el-radio>
                                        </el-radio-group>
                                    </el-form-item>
                                    <el-form-item label="订阅消息主题：">
                                        <el-input v-model="formData.conf[ind][8]" style="width: 280px"></el-input>
                                    </el-form-item>
                                    <el-form-item label="发布消息主题：">
                                        <el-input v-model="formData.conf[ind][9]" style="width: 280px"></el-input>
                                    </el-form-item>
                                    <el-form-item label="MQTT的QOS级别：">
                                        <el-radio-group v-model="formData.conf[ind][10]">
                                            <el-radio :value="0">0</el-radio>
                                            <el-radio :value="1">1</el-radio>
                                            <el-radio :value="2">2</el-radio>
                                        </el-radio-group>
                                    </el-form-item>
                                    <el-form-item label="MQTT的publish参数retain：">
                                        <el-radio-group v-model="formData.conf[ind][11]">
                                            <el-radio :value="0">0</el-radio>
                                            <el-radio :value="1">1</el-radio>
                                        </el-radio-group>
                                    </el-form-item>
                                    <el-form-item label="MQTT通道捆绑的串口ID：">
                                        <el-radio-group v-model="formData.conf[ind][12]">
                                            <el-radio :value="1">1</el-radio>
                                            <el-radio :value="2">2</el-radio>
                                            <el-radio :value="3">3</el-radio>
                                        </el-radio-group>
                                    </el-form-item>
                                    <el-form-item label="ID是否添加默认IMEI：">
                                        <el-radio-group v-model="formData.conf[ind][17]">
                                            <el-radio value="">是</el-radio>
                                            <el-radio value="1">否</el-radio>
                                        </el-radio-group>
                                        <span class="tip">提示: 默认添加IMEI，选是的格式是IMEI+内容，选否则自定义内容，不填则默认IMEI</span>
                                    </el-form-item>
                                    <el-form-item label="接收mqtt信息是否显示主题 ：">
                                        <el-radio-group v-model="formData.conf[ind][18]">
                                            <el-radio value="">是</el-radio>
                                            <el-radio value="1">否</el-radio>
                                        </el-radio-group>
                                        <span class="tip">提示: 默认不显示主题,显示格式为[+MSUB: topic,len,message]</span>
                                    </el-form-item>
                                    <el-form-item label="客户端ID：">
                                        <el-input v-model="formData.conf[ind][13]" style="width: 280px"></el-input>
                                        <span class="tip">提示: 不填系统用IMEI做客户端ID</span>
                                    </el-form-item>
                                    <el-form-item label="主题添加IMEI：">
                                        <el-radio-group v-model="formData.conf[ind][14]">
                                            <el-radio value="">是</el-radio>
                                            <el-radio value="1">否</el-radio>
                                        </el-radio-group>
                                        <span class="tip">提示: 默认添加（/自定义主题/IMEI）</span>
                                    </el-form-item>
                                    <el-form-item label="transport：">
                                        <el-radio-group v-model="formData.conf[ind][15]">
                                            <el-radio value="tcp">tcp</el-radio>
                                            <el-radio value="tcp_ssl">tcp_ssl</el-radio>
                                        </el-radio-group>
                                    </el-form-item>
                                    <el-form-item label="MQTT的遗嘱：">
                                        <el-input v-model="formData.conf[ind][16]" style="width: 280px"></el-input>
                                        <span class="tip">提示: 可不填</span>
                                    </el-form-item>
                                </div>
                                <div v-if="netValues[ind].type == 'onenet'" style="margin-top: 10px;">
                                    <el-form-item label="协议：">
                                        <el-radio-group v-model="netValues[ind].onenet.type" @change="netTypeChange(ind)">
                                            <el-radio value="DTU">DTU协议</el-radio>
                                            <el-radio value="MQTT">MQTT协议</el-radio>
                                            <el-radio value="MODBUS">MODBUS协议</el-radio>
                                        </el-radio-group>
                                    </el-form-item>
                                    <div v-if="formData.conf[ind][1]=='DTU'">
                                        <el-form-item label="心跳包：" class="label-font-weight">
                                            <el-radio-group v-model="netValues[ind].onenet.heartbeat">
                                                <el-radio :value="0">自定义</el-radio>
                                                <el-radio :value="1">顺序生成</el-radio>
                                            </el-radio-group>
                                            <div v-if="netValues[ind].onenet.heartbeat == 0" style="margin-left: 20px;">
                                                <el-input v-model="netValues[ind].onenet.data" style="width: 150px" />
                                            </div>
                                            <div v-if="netValues[ind].onenet.heartbeat == 1" style="margin-left: 20px;">
                                                前缀： <el-input v-model="netValues[ind].onenet.prefix" style="width: 150px" />
                                                后缀： <el-input v-model="netValues[ind].onenet.postfix" style="width: 150px" />
                                            </div>
                                        </el-form-item>
                                        <el-form-item label="链接保活时间：">
                                            <el-input-number v-model="formData.conf[ind][3]" :min="60" :max="1800" style="width: 150px"></el-input-number>
                                            <span class="tip">提示: 单位秒，范围60~1800</span>
                                        </el-form-item>
                                        <el-form-item label="onenet的地址或域名：">
                                            <el-input v-model="formData.conf[ind][4]" style="width: 150px"></el-input>
                                        </el-form-item>
                                        <el-form-item label="onenet服务器的端口号：">
                                            <el-input v-model="formData.conf[ind][5]" style="width: 150px"></el-input>
                                            <span class="tip">提示: 端口号范围：1~65536</span>
                                        </el-form-item>
                                        <el-form-item label="正式生产环境注册码：">
                                            <el-input v-model="formData.conf[ind][6]" style="width: 150px"></el-input>
                                        </el-form-item>
                                        <el-form-item label="产品ID：">
                                            <el-input v-model="formData.conf[ind][7]" style="width: 150px"></el-input>
                                        </el-form-item>
                                        <el-form-item label="解析脚本名称：">
                                            <el-input v-model="formData.conf[ind][8]" style="width: 150px"></el-input>
                                        </el-form-item>
                                        <el-form-item label="DTU通道捆绑的串口ID：">
                                            <el-radio-group v-model="formData.conf[ind][9]">
                                                <el-radio :value="1">1</el-radio>
                                                <el-radio :value="2">2</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                    </div>
                                    <div v-if="formData.conf[ind][1]=='MQTT'">
                                        <el-form-item label="链接保活时间：">
                                            <el-input v-model="formData.conf[ind][2]" style="width: 150px"></el-input>
                                            <span class="tip">提示: 60–1800 只接受数字</span>
                                        </el-form-item>
                                        <el-form-item label="自动采集任务间隔：">
                                            <el-input v-model="formData.conf[ind][3]" style="width: 150px"></el-input>
                                            <span class="tip">提示: 单位秒，MQTT接收超时时间，配合自动采集任务使用</span>
                                        </el-form-item>
                                        <el-form-item label="onenet的地址或域名：">
                                            <el-input v-model="formData.conf[ind][4]" style="width: 150px"></el-input>
                                        </el-form-item>
                                        <el-form-item label="onenet服务器的端口号：">
                                            <el-input v-model="formData.conf[ind][5]" style="width: 150px"></el-input>
                                            <span class="tip">提示: 端口号范围：1~65536</span>
                                        </el-form-item>
                                        <el-form-item label="正式生产环境注册码：">
                                            <el-input v-model="formData.conf[ind][6]" style="width: 150px"></el-input>
                                        </el-form-item>
                                        <el-form-item label="产品ID：">
                                            <el-input v-model="formData.conf[ind][7]" style="width: 150px"></el-input>
                                        </el-form-item>
                                        <el-form-item label="数据格式：">
                                            <el-radio-group v-model="formData.conf[ind][8]">
                                                <el-radio :value="1">1</el-radio>
                                                <el-radio :value="3">3</el-radio>
                                                <el-radio :value="4">4</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item label="MQTT保存会话标志位：">
                                            <el-radio-group v-model="formData.conf[ind][9]">
                                                <el-radio :value="0">持久会话</el-radio>
                                                <el-radio :value="1">离线自动销毁</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item label="MQTT的QOS级别：">
                                            <el-radio-group v-model="formData.conf[ind][10]">
                                                <el-radio :value="0">0</el-radio>
                                                <el-radio :value="1">1</el-radio>
                                                <el-radio :value="2">2</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item label="MQTT的publish参数retain：">
                                            <el-radio-group v-model="formData.conf[ind][11]">
                                                <el-radio :value="0">0</el-radio>
                                                <el-radio :value="1">1</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item label="MQTT通道捆绑的串口ID：">
                                            <el-radio-group v-model="formData.conf[ind][12]">
                                                <el-radio :value="1">1</el-radio>
                                                <el-radio :value="2">2</el-radio>
                                                <el-radio :value="3">3</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                    </div>
                                    <div v-if="formData.conf[ind][1]=='MODBUS'">
                                        <el-form-item label="链接保活时间：">
                                            <el-input v-model="formData.conf[ind][2]" style="width: 150px"></el-input>
                                            <span class="tip">提示: 60–300 只接受数字</span>
                                        </el-form-item>
                                        <el-form-item label="Master-APIkey：">
                                            <el-input v-model="formData.conf[ind][3]" style="width: 150px"></el-input>
                                            <span class="tip">提示: 产品的Master-APIkey</span>
                                        </el-form-item>
                                        <el-form-item label="产品ID：">
                                            <el-input v-model="formData.conf[ind][4]" style="width: 150px"></el-input>
                                        </el-form-item>
                                        <el-form-item label="串口通道：">
                                            <el-radio-group v-model="formData.conf[ind][5]">
                                                <el-radio :value="1">1</el-radio>
                                                <el-radio :value="2">2</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                    </div>
                                </div>
                                <div v-if="netValues[ind].type == 'aliyun'" style="margin-top: 10px;">
                                    <el-form-item label="注册类型：">
                                        <el-radio-group v-model="netValues[ind].aliyun.type" @change="netTypeChange(ind)">
                                            <el-radio value="auto">自动注册</el-radio>
                                            <el-radio value="otok">一型一密</el-radio>
                                            <el-radio value="omok">一机一密</el-radio>
                                        </el-radio-group>
                                    </el-form-item>
                                    <el-form-item label="链接保活时间：">
                                        <el-input-number v-model="formData.conf[ind][2]" :min="60" :max="1800" style="width: 150px"></el-input-number>
                                        <span class="tip">提示: 60–1800 只接受数字</span>
                                    </el-form-item>
                                    <el-form-item label="自动采集任务间隔：">
                                        <el-input-number v-model="formData.conf[ind][3]" style="width: 150px"></el-input-number>
                                        <span class="tip">提示: 单位秒，MQTT接收超时时间，配合自动采集任务使用</span>
                                    </el-form-item>
                                    <el-form-item label="地域代码(RegionID)：">
                                        <el-input v-model="formData.conf[ind][4]" style="width: 150px"></el-input>
                                    </el-form-item>
                                    <el-form-item label="ProductKey：">
                                        <el-input v-model="formData.conf[ind][5]" style="width: 150px"></el-input>
                                    </el-form-item>
                                    <div v-if="formData.conf[ind][1]=='auto'">
                                        <el-form-item label="AccessKey ID：">
                                            <el-input v-model="formData.conf[ind][6]" style="width: 150px"></el-input>
                                        </el-form-item>
                                        <el-form-item label="Access Key Secret：">
                                            <el-input v-model="formData.conf[ind][7]" style="width: 150px"></el-input>
                                        </el-form-item>
                                        <el-form-item label="产品版本类型：">
                                            <el-radio-group v-model="formData.conf[ind][8]">
                                                <el-radio value="basic">基础版</el-radio>
                                                <el-radio value="plus">企业版</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item label="MQTT保存会话标志位：">
                                            <el-radio-group v-model="formData.conf[ind][9]">
                                                <el-radio :value="0">持久会话</el-radio>
                                                <el-radio :value="1">离线自动销毁</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item label="MQTT的QOS级别：">
                                            <el-radio-group v-model="formData.conf[ind][10]">
                                                <el-radio :value="0">0</el-radio>
                                                <el-radio :value="1">1</el-radio>
                                                <el-radio :value="2">2</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item label="MQTT通道捆绑的串口ID：">
                                            <el-radio-group v-model="formData.conf[ind][11]">
                                                <el-radio :value="1">1</el-radio>
                                                <el-radio :value="2">2</el-radio>
                                                <el-radio :value="3">3</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item label="订阅主题：">
                                            <el-input v-model="formData.conf[ind][12]" style="width: 150px"></el-input>
                                        </el-form-item>
                                        <el-form-item label="发布主题：">
                                            <el-input v-model="formData.conf[ind][13]" style="width: 150px"></el-input>
                                        </el-form-item>
                                    </div>
                                    <div v-if="formData.conf[ind][1]=='otok'">
                                        <el-form-item label="ProductSecret：">
                                            <el-input v-model="formData.conf[ind][6]" style="width: 150px"></el-input>
                                        </el-form-item>
                                        <el-form-item label="实例ID：">
                                            <el-input v-model="formData.conf[ind][13]" style="width: 150px"></el-input>
                                            <span class="tip">提示: 使用企业版必须填写企业版对应的实例id，公共版不需要填写</span>
                                        </el-form-item>
                                        <el-form-item label="产品版本类型：">
                                            <el-radio-group v-model="formData.conf[ind][7]">
                                                <el-radio value="basic">基础版</el-radio>
                                                <el-radio value="plus">企业版</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item label="MQTT保存会话标志位：">
                                            <el-radio-group v-model="formData.conf[ind][8]">
                                                <el-radio :value="0">持久会话</el-radio>
                                                <el-radio :value="1">离线自动销毁</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item label="MQTT的QOS级别：">
                                            <el-radio-group v-model="formData.conf[ind][9]">
                                                <el-radio :value="0">0</el-radio>
                                                <el-radio :value="1">1</el-radio>
                                                <el-radio :value="2">2</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item label="MQTT通道捆绑的串口ID：">
                                            <el-radio-group v-model="formData.conf[ind][10]">
                                                <el-radio :value="1">1</el-radio>
                                                <el-radio :value="2">2</el-radio>
                                                <el-radio :value="3">3</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item label="订阅主题：">
                                            <el-input v-model="formData.conf[ind][11]" style="width: 150px"></el-input>
                                        </el-form-item>
                                        <el-form-item label="发布主题：">
                                            <el-input v-model="formData.conf[ind][12]" style="width: 150px"></el-input>
                                        </el-form-item>
                                    </div>
                                    <div v-if="formData.conf[ind][1]=='omok'">
                                        <el-form-item label="DeviceSecret：">
                                            <el-input v-model="formData.conf[ind][6]" style="width: 150px"></el-input>
                                        </el-form-item>
                                        <el-form-item label="DeviceName：">
                                            <el-input v-model="formData.conf[ind][7]" style="width: 150px"></el-input>
                                        </el-form-item>
                                        <el-form-item label="实例ID：">
                                            <el-input v-model="formData.conf[ind][14]" style="width: 150px"></el-input>
                                            <span class="tip">提示: 使用企业版必须填写企业版对应的实例id，公共版不需要填写</span>
                                        </el-form-item>
                                        <el-form-item label="产品版本类型：">
                                            <el-radio-group v-model="formData.conf[ind][8]">
                                                <el-radio value="basic">基础版</el-radio>
                                                <el-radio value="plus">企业版</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item label="MQTT保存会话标志位：">
                                            <el-radio-group v-model="formData.conf[ind][9]">
                                                <el-radio :value="0">持久会话</el-radio>
                                                <el-radio :value="1">离线自动销毁</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item label="MQTT的QOS级别：">
                                            <el-radio-group v-model="formData.conf[ind][10]">
                                                <el-radio :value="0">0</el-radio>
                                                <el-radio :value="1">1</el-radio>
                                                <el-radio :value="2">2</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item label="MQTT通道捆绑的串口ID：">
                                            <el-radio-group v-model="formData.conf[ind][11]">
                                                <el-radio :value="1">1</el-radio>
                                                <el-radio :value="2">2</el-radio>
                                                <el-radio :value="3">3</el-radio>
                                            </el-radio-group>
                                        </el-form-item>
                                        <el-form-item label="订阅主题：">
                                            <el-input v-model="formData.conf[ind][12]" style="width: 150px"></el-input>
                                        </el-form-item>
                                        <el-form-item label="发布主题：">
                                            <el-input v-model="formData.conf[ind][13]" style="width: 150px"></el-input>
                                        </el-form-item>
                                    </div>
                                </div>
                            </el-tab-pane>
                        </el-tabs>
                    </el-tab-pane>
                    <el-tab-pane label="预置信息" name="3" style="padding: 0 20px">预置信息</el-tab-pane>
                    <el-tab-pane label="GPIO" name="4" style="padding: 0 20px">GPIO</el-tab-pane>
                    <el-tab-pane label="GPS" name="5" style="padding: 0 20px">GPS</el-tab-pane>
                    <el-tab-pane label="数据流" name="6" style="padding: 0 20px">数据流</el-tab-pane>
                    <el-tab-pane label="预警" name="7" style="padding: 0 20px">预警</el-tab-pane>
                    <el-tab-pane label="任务" name="8" style="padding: 0 20px">任务</el-tab-pane>
                </el-tabs>
            </el-form>
        </el-row>

        <el-dialog title="导出参数" v-model="showExport" width="45%" :close-on-click-modal="false">
            <el-form>
                <el-form-item>
                    <el-input type="textarea" rows="10" v-model="config" />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="showExport = false">关 闭</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>
<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, toRefs } from 'vue'
import modal from '/@/utils/modal'
import { ElForm, ElTable, ElUpload } from 'element-plus'
import { useRoute } from 'vue-router'
import { getDtuInfo, doDtuEnabled, doDtuSave } from '/@/api/platform/iot/product'

const route = useRoute()
const id = route.params.id as string

const createRef = ref<InstanceType<typeof ElForm>>()
const showExport = ref(false)
const activeName = ref('0')
const activeComm = ref(0)
const commValues = ref({
    0: 0,
    1: 0,
    2: 0,
})
const activeNet = ref(0)
// 通道中间temp数据
const netValues = ref({
    0: {enabled:0,type:'',socket: {heartbeat:0,data:'0x00',prefix:'',postfix:''},onenet: {type: 'DTU',heartbeat:0,data:'',prefix:'',postfix:''},aliyun:{type:'auto'}},
    1: {enabled:0,type:'',socket: {heartbeat:0,data:'0x00',prefix:'',postfix:''},onenet: {type: 'DTU',heartbeat:0,data:'',prefix:'',postfix:''},aliyun:{type:'auto'}},
    2: {enabled:0,type:'',socket: {heartbeat:0,data:'0x00',prefix:'',postfix:''},onenet: {type: 'DTU',heartbeat:0,data:'',prefix:'',postfix:''},aliyun:{type:'auto'}},
    3: {enabled:0,type:'',socket: {heartbeat:0,data:'0x00',prefix:'',postfix:''},onenet: {type: 'DTU',heartbeat:0,data:'',prefix:'',postfix:''},aliyun:{type:'auto'}},
    4: {enabled:0,type:'',socket: {heartbeat:0,data:'0x00',prefix:'',postfix:''},onenet: {type: 'DTU',heartbeat:0,data:'',prefix:'',postfix:''},aliyun:{type:'auto'}},
    5: {enabled:0,type:'',socket: {heartbeat:0,data:'0x00',prefix:'',postfix:''},onenet: {type: 'DTU',heartbeat:0,data:'',prefix:'',postfix:''},aliyun:{type:'auto'}},
    6: {enabled:0,type:'',socket: {heartbeat:0,data:'0x00',prefix:'',postfix:''},onenet: {type: 'DTU',heartbeat:0,data:'',prefix:'',postfix:''},aliyun:{type:'auto'}},
})
const register = ref({
    type: 0,
    data: '',
    prefix: '',
    postfix: '',
})
const numberArr = ref(['','disable'])
const gpioArr = ref([])
for (var i=0;i<=128;i++){
    numberArr.value.push('pio'+i)
    gpioArr.value.push('pio'+i)
}

const tableData = ref({
    version: 0,
    enabled: false,
    config: '',
})
const config = ref('')

const data = reactive({
    formData: {
        fota: 0,
        uartReadTime: 25,
        flow: '',
        param_ver: '',
        pwrmod: 'normal',
        password: '',
        netReadTime: 0,
        passon: 1,
        nolog: '1',
        isRndis: '0',
        isipv6: '0',
        webProtect: '1',
        plate: 0,
        protectContent: [0, 0, 0, 0, 0, 0, 0],
        register: {type:0,data:'',prefix:'',postfix:''},
        reg: 0,
        convert: 0,
        uconf: [[], [], []],
        conf: [[], [], [], [], [], [], []],
        preset: { number: '', delay: '', smsword: '' },
        apn: ['', '', ''],
        cmds: [[], [], []],
        pins: [],
        gps: { pio: [], fun: [] },
        upprot: ['', '', '', '', '', '', ''],
        dwprot: ['', '', '', '', '', '', ''],
        warn: { adc0: [], adc1: [], vbatt: [], gpio: [] },
    },
    formRules: {
        name: [{ required: true, message: '固件名称不能为空', trigger: ['blur', 'change'] }],
    },
})

const { formData, formRules } = toRefs(data)

// 重置表单
const resetFrom = () => {
    commValues.value = {
        0: 0,
        1: 0,
        2: 0,
    }
    netValues.value = {
        0: {enabled:0,type:'',socket: {heartbeat:0,data:'0x00',prefix:'',postfix:''},onenet: {type: 'DTU',heartbeat:0,data:'',prefix:'',postfix:''},aliyun:{type:'auto'}},
        1: {enabled:0,type:'',socket: {heartbeat:0,data:'0x00',prefix:'',postfix:''},onenet: {type: 'DTU',heartbeat:0,data:'',prefix:'',postfix:''},aliyun:{type:'auto'}},
        2: {enabled:0,type:'',socket: {heartbeat:0,data:'0x00',prefix:'',postfix:''},onenet: {type: 'DTU',heartbeat:0,data:'',prefix:'',postfix:''},aliyun:{type:'auto'}},
        3: {enabled:0,type:'',socket: {heartbeat:0,data:'0x00',prefix:'',postfix:''},onenet: {type: 'DTU',heartbeat:0,data:'',prefix:'',postfix:''},aliyun:{type:'auto'}},
        4: {enabled:0,type:'',socket: {heartbeat:0,data:'0x00',prefix:'',postfix:''},onenet: {type: 'DTU',heartbeat:0,data:'',prefix:'',postfix:''},aliyun:{type:'auto'}},
        5: {enabled:0,type:'',socket: {heartbeat:0,data:'0x00',prefix:'',postfix:''},onenet: {type: 'DTU',heartbeat:0,data:'',prefix:'',postfix:''},aliyun:{type:'auto'}},
        6: {enabled:0,type:'',socket: {heartbeat:0,data:'0x00',prefix:'',postfix:''},onenet: {type: 'DTU',heartbeat:0,data:'',prefix:'',postfix:''},aliyun:{type:'auto'}},
    }
    formData.value = {
        fota: 0,
        uartReadTime: 25,
        flow: '',
        param_ver: '',
        pwrmod: 'normal',
        password: '',
        netReadTime: 0,
        passon: 1,
        nolog: '1',
        isRndis: '0',
        isipv6: '0',
        webProtect: '1',
        plate: 0,
        protectContent: [0, 0, 0, 0, 0, 0, 0],
        register: {type:0,data:'',prefix:'',postfix:''},
        reg: 0,
        convert: 0,
        uconf: [[], [], []],
        conf: [[], [], [], [], [], [], []],
        preset: { number: '', delay: '', smsword: '' },
        apn: ['', '', ''],
        cmds: [[], [], []],
        pins: [],
        gps: { pio: [], fun: [] },
        upprot: ['', '', '', '', '', '', ''],
        dwprot: ['', '', '', '', '', '', ''],
        warn: { adc0: [], adc1: [], vbatt: [], gpio: [] },
    }
    createRef?.value?.resetFields()
}

// 透传模式
const passonChange = (val: number) => {
    formData.value.plate = 0
}

// 串口启用
const commChange = (val: number) => {
    if (val == 1) {
        formData.value.uconf[activeComm.value] = [activeComm.value+1,"115200",8,2,0,"",0]
    } else {
        formData.value.uconf[activeComm.value] = []
    }
}

// 注册信息
const registerChange = (val: number) => {
    register.value.data = ''
    register.value.prefix = ''
    register.value.postfix = ''
}

// 网络通道启用
const netChange = (val: number) => {
    if(val == 1){
        netValues.value[activeNet.value].type = 'socket'
        netTypeChange(activeNet.value)
    } else {
        netValues.value[activeNet.value].type = ''
        formData.value.conf[activeNet.value] = []
    }
}

// 网络通道类型
const netTypeChange = (ind: number) => {
    if (netValues.value[ind].type == 'http') {
        formData.value.conf[ind] = ["http","1","post","",30,"1","1","","",0,0,0]
    } else if(netValues.value[ind].type == 'mqtt'){
        formData.value.conf[ind] = ["mqtt",300,1800,"",1883,"","",1,"","",0,0,1,"","","tcp","","",""]
    } else if(netValues.value[ind].type == 'onenet'){
        if(netValues.value[ind].onenet.type == 'DTU'){
            formData.value.conf[ind] = ["onenet","DTU","",300,"183.230.40.40","1811","","","",1]
        }else if(netValues.value[ind].onenet.type == 'MQTT'){
            formData.value.conf[ind] = ["onenet","MQTT","300","600","mqtt.heclouds.com","6002","","",1,1,0,0,1]
        }else if(netValues.value[ind].onenet.type == 'MODBUS'){
            formData.value.conf[ind] = ["onenet","MODBUS","120","","",1]
        }
    } else if(netValues.value[ind].type == 'aliyun'){
        if(netValues.value[ind].aliyun.type == 'auto'){
            formData.value.conf[ind] = ["aliyun","auto",300,1800,"cn-shanghai","","","","basic",1,0,1,"",""]
        }else if(netValues.value[ind].aliyun.type == 'otok'){
            formData.value.conf[ind] = ["aliyun","otok",300,1800,"cn-shanghai","","","basic",1,0,1,"","",""]
        }else if(netValues.value[ind].aliyun.type == 'omok'){
            formData.value.conf[ind] = ["aliyun","omok",300,1800,"cn-shanghai","","","","basic",1,0,1,"","",""]
        }
    }
    else
    {
        formData.value.conf[ind] = ["tcp","0x00",300,"",2345,1,"","","",""]
    }
}

// 处理表单数据
const processFormData = () => {
    // 注册信息转换
    if(register.value.type == 0||register.value.type==1||register.value.type==2){
        formData.value.reg = register.value.type
    }else if(register.value.type == 3){
        formData.value.reg = register.value.data
    }else if(register.value.type == 4){
        formData.value.reg =  register.value.prefix + '940802' +register.value.postfix
    }
    // 网络通道转换，socket的heartbeat转换
    formData.value.conf.forEach((item, index) => {
        if (item.length > 0) {
            if(netValues.value[index].type == 'socket'){
                if(netValues.value[index].socket.heartbeat == 0){
                    formData.value.conf[index][1] = netValues.value[index].socket.data
                }else if(netValues.value[index].socket.heartbeat == 1){
                    formData.value.conf[index][1] = netValues.value[index].socket.prefix + '940802' + netValues.value[index].socket.postfix
                }
            } else if(netValues.value[index].type == 'onenet'){
                if(netValues.value[index].onenet.type == 'DTU'){
                    if(netValues.value[index].onenet.heartbeat == 0){
                        formData.value.conf[index][2] = netValues.value[index].onenet.data
                    }else if(netValues.value[index].onenet.heartbeat == 1){
                        formData.value.conf[index][2] = netValues.value[index].onenet.prefix + '940802' + netValues.value[index].onenet.postfix
                    }
                }
            }
        }
    })
    console.log(JSON.stringify(formData.value))
}

// 解析表单数据
const parseFormData = () => {
    // 注册信息转换
    if(formData.value.reg == 0||formData.value.reg==1||formData.value.reg==2){
        register.value.type = formData.value.reg
        register.value.data = ''
        register.value.prefix = ''
        register.value.postfix = ''
    }else if(typeof formData.value.reg == 'string' && formData.value.reg.indexOf('940802') > 0){
        register.value.type = 4
        let str = formData.value.reg || ''
        register.value.prefix = str.substring(0, str.indexOf('940802'))
        register.value.postfix = str.substring(str.indexOf('940802')+6)
        register.value.data = ''
    }else if(typeof formData.value.reg == 'string'){
        register.value.type = 3
        register.value.data = formData.value.reg
        register.value.prefix = ''
        register.value.postfix = ''
    }
    // 串口转换，根据uconf的数组是否为空，初始化commValues
    formData.value.uconf.forEach((item, index) => {
        if (item.length > 0) {
            commValues.value[index] = 1
        }
    })
    // 网络通道转换，根据conf的数组是否为空，初始化netValues
    formData.value.conf.forEach((item, index) => {
        if (item.length > 0) {
            netValues.value[index].enabled = 1
            if(item[0] == 'tcp' || item[0] == 'udp'){
                netValues.value[index].type = 'socket'
                if(item[1].indexOf('940802') > 0){
                    netValues.value[index].socket.heartbeat = 1
                    netValues.value[index].socket.prefix = item[1].substring(0, item[1].indexOf('940802'))
                    netValues.value[index].socket.postfix = item[1].substring(item[1].indexOf('940802')+6)
                }else{
                    netValues.value[index].socket.heartbeat = 0
                    netValues.value[index].socket.data = item[1]
                }
            } else if(item[0] == 'http'){
                netValues.value[index].type = 'http'
            } else if(item[0] == 'mqtt'){
                netValues.value[index].type = 'mqtt'
            } else if(item[0] == 'onenet'){
                netValues.value[index].type = 'onenet'
                if(item[1] == 'DTU'){
                    netValues.value[index].onenet.type = 'DTU'
                    if(item[2].indexOf('940802') > 0){
                        netValues.value[index].onenet.heartbeat = 1
                        netValues.value[index].onenet.prefix = item[2].substring(0, item[2].indexOf('940802'))
                        netValues.value[index].onenet.postfix = item[2].substring(item[2].indexOf('940802')+6)
                    }else{
                        netValues.value[index].onenet.heartbeat = 0
                        netValues.value[index].onenet.data = item[2]
                    }
                }else if(item[1] == 'MQTT'){
                    netValues.value[index].onenet.type = 'MQTT'
                }else if(item[1] == 'MODBUS'){
                    netValues.value[index].onenet.type = 'MODBUS'
                }
            } else if(item[0] == 'aliyun'){
                netValues.value[index].type = 'aliyun'
                netValues.value[index].aliyun.type = item[1]
            }
        }
    })
}

const save = () => {
    processFormData()
    return
    createRef.value.validate((valid) => {
        if (valid) {
            parseData();
            doDtuSave({
                productId: id,
                config: JSON.stringify(formData.value),
            }).then((res) => {
                if (res.code === 0) {
                    modal.msgSuccess('保存成功')
                    init()
                }
            })
        }
    })
}

const enabledChange = (val: boolean) => {
    if (val) {
        modal
            .confirm('启用后产品下所有设备将请求最新版本参数')
            .then(() => {
                return doDtuEnabled({
                    productId: id,
                    enabled: val,
                })
            })
            .then((res) => {
                if (res.code === 0) {
                    tableData.value.enabled = val
                    modal.msgSuccess('操作成功')
                }
            })
            .catch(() => {
                setTimeout(() => {
                    tableData.value.enabled = !val
                }, 500)
            })
    } else {
        doDtuEnabled({
            productId: id,
            enabled: val,
        }).then((res) => {
            if (res.code === 0) {
                tableData.value.enabled = val
                modal.msgSuccess('操作成功')
            }
        })
    }
}

const exportTxt = () => {
    config.value = JSON.parse(tableData.value.config)
    showExport.value = true
}

const init = () => {
    getDtuInfo(id).then((res) => {
        tableData.value = res.data
        return
        if (res.data.config) {
            // JSON.parse(res.data.config) 值赋值给formData.value
            formData.value = JSON.parse(res.data.config) as any

            console.log(formData.value)
            // 根据uconf的数组是否为空，初始化commValues
            formData.value.uconf.forEach((item, index) => {
                if (item.length > 0) {
                    commValues.value[index] = true
                }
            })
        }
    })
}

onMounted(() => {
    init()
})
</script>
<!--定义布局-->
<route lang="yaml">
    meta:
      layout: platform/index
</route>
<style scoped>
.tip {
    color: #999;
    padding-left: 10px;
}
</style>