# 本地运行

## 开发环境

* OpenJDK 11
* Redis 6.x 以上
* MariaDB 10.x
* MongoDB 7.0.x 以上
* RocketMQ 5.2.x 以上

* Node.js v18.x 以上

## RocketMQ 启动

* 启动服务

`./mqnamesrv`

* 启动 broker（集群部署不要设置 autoCreateTopicEnable=true）

`./mqbroker -n localhost:9876 autoCreateTopicEnable=true`

* 手动创建 topic（生产环境集群部署）

`./mqadmin updatetopic -n localhost:9876 -t DemoTopic -c DefaultCluster -r 10 -w 10`

## MongoDB 启动

* 创建数据文件夹

`mkdir -p /Users/wizzer/data/mongo`

* 启动 MongoDB

`mongod --dbpath /Users/wizzer/data/mongo`

## 后台运行

~IDEA下运行~

* `budiot-java-server/budiot-server` 项目下找到 `WebMainLauncher` 鼠标右击运行

* `--nutz.profiles.active=dev` 使用命令行参数，可指定加载的配置文件

* 修改配置文件，务必配置正确的数据库、Redis、MongoDB连接信息

* 数据库可以使用 `init` 目录下的sql初始化，也可以使用空数据库，程序会自动生成表结构和数据


## 前台运行

* `npm install -g pnpm` 安装pnpm

* `budiot-vue-admin` 目录下执行 `pnpm i` 和 `pnpm run dev`

* `http://127.0.0.1:1820` 浏览器访问系统后台，默认账号 `superadmin` 默认密码 `1`

## 设备网关服务运行

~IDEA下运行~

* `budiot-java-server/budiot-access/budiot-access-gateway` 项目下找到 `GatewayLauncher`  鼠标右击运行

* `--nutz.profiles.active=dev` 使用命令行参数，可指定加载的配置文件

* 配置文件中的设备协议标识（如 DEMO），与后台协议管理的标识一致，设备网关通过配置在对应的网络协议及端口下启动

```yaml
gateway:
  DEMO: # 和设备协议标识一致
    transport: tcp # 固定 tcp
    protocolCode: DEMO  # 设备协议标识
    properties: # 网关的配置参数
      port: 9003 # 端口号
      host: '0.0.0.0' # 绑定的host，默认 0.0.0.0
      payloadType: string # 数据类型，可选 hex 和 string 默认 hex
  MODBUSDEMO: # 和设备协议标识一致
    transport: modbusSlave # 固定 modbusSlave
    protocolCode: MODBUSDEMO  # 设备协议标识
    properties: # 网关的配置参数
      port: 9008 # 端口号
      host: '0.0.0.0' # 绑定的host，默认 0.0.0.0
      payloadType: hex # 数据类型，可选 hex 和 string 默认 hex
```

* 通过修改配置信息，运行不同的实例，在不同服务器及端口提供网络协议服务，以便灵活运行部署

## 设备业务服务运行

~IDEA下运行~

* `budiot-java-server/budiot-access/budiot-access-processor` 项目下找到 `ProcessorLauncher`  鼠标右击运行

* `--nutz.profiles.active=dev` 使用命令行参数，可指定加载的配置文件

* 修改配置文件，配置 `protocol.classes` 加载不同的设备协议解析类

```yaml
# 设备协议配置
protocol:
  # 是否启用
  enable: true
  # 加载解析包
  classes:
    - com.budwk.app.access.protocol.demo.DemoProtocol

# 设备数据处理配置
processor:
  # 是否启用
  enable: true
```

* 通过修改 `protocol.enable` 和 `processor.enable`，运行不同的实例，可将设备协议解析、数据处理拆分运行部署
