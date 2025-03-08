# Local Development

## Development Environment

* OpenJDK 11
* Redis 6.x or above
* MariaDB 10.x or PostgreSQL 13.x or above
* MongoDB 7.0.x or above
* RocketMQ 5.2.x or above, or RabbitMQ 3.8.x or above
* Node.js v18.x or above

## RocketMQ Startup

* Start the service

`./mqnamesrv`

* Start broker (for cluster deployment, do not set autoCreateTopicEnable=true)

`./mqbroker -n localhost:9876 autoCreateTopicEnable=true`

* Manually create topic (for production environment cluster deployment)

`./mqadmin updatetopic -n localhost:9876 -t DemoTopic -c DefaultCluster -r 10 -w 10`

## MongoDB Startup

* Create data folder

`mkdir -p /Users/wizzer/data/mongo`

* Start MongoDB

`mongod --dbpath /Users/wizzer/data/mongo`

## Backend Startup

~Running in IDEA~

* Right-click on `WebMainLauncher` in the `budiot-java-server/budiot-server` project to run

* Use command line parameter `--nutz.profiles.active=dev` to specify the configuration file to load

* Modify the configuration file, ensuring correct database, Redis, and MongoDB connection information

* Initialize the database using `budiot.sql` in the `init` directory

## Frontend Startup

* Install pnpm: `npm install -g pnpm`

* In the `budiot-vue-admin` directory, execute `pnpm i` and `pnpm run dev`

* Access the system backend at `http://127.0.0.1:1820` in your browser, default account: `superadmin`, default password: `1`

## Device Gateway Service Startup

~Running in IDEA~

* Right-click on `GatewayLauncher` in the `budiot-java-server/budiot-access/budiot-access-gateway` project to run

* Use command line parameter `--nutz.profiles.active=dev` to specify the configuration file to load

* The device protocol identifier in the configuration file (e.g., DEMO) should match the identifier in the backend protocol management. The device gateway starts under the corresponding network protocol and port according to the configuration

```yaml
gateway:
  DEMO: # Match the device protocol identifier
    transport: tcp # Fixed as tcp
    protocolCode: DEMO  # Device protocol identifier
    properties: # Gateway configuration parameters
      port: 9003 # Port number
      host: '0.0.0.0' # Binding host, default is 0.0.0.0
      payloadType: string # Data type, options are hex and string, default is hex
  MODBUSDEMO: # Match the device protocol identifier
    transport: modbusSlave # Fixed as modbusSlave
    protocolCode: MODBUSDEMO  # Device protocol identifier
    properties: # Gateway configuration parameters
      port: 9008 # Port number
      host: '0.0.0.0' # Binding host, default is 0.0.0.0
      payloadType: hex # Data type, options are hex and string, default is hex
```

* By modifying the configuration information, different instances can be run to provide network protocol services on different servers and ports for flexible operation and deployment

## Device Business Service Startup

~Running in IDEA~

* Right-click on `ProcessorLauncher` in the `budiot-java-server/budiot-access/budiot-access-processor` project to run

* Use command line parameter `--nutz.profiles.active=dev` to specify the configuration file to load

* Modify the configuration file, configure `protocol.classes` to load different device protocol parsing classes

```yaml
# Device protocol configuration
protocol:
  # Whether to enable
  enable: true
  # Load parsing packages
  classes:
    - com.budwk.app.access.protocol.demo.DemoProtocol

# Device data processing configuration
processor:
  # Whether to enable
  enable: true
```

* By modifying `protocol.enable` and `processor.enable`, different instances can be run to split device protocol parsing and data processing for separate operation and deployment 