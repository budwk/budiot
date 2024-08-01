# 打包部署

## 后端打包

### 安装依赖
* `budiot-java-server` 目录下执行 `mvn install`

### 项目打包

* `budiot-java-server/budiot-access/budiot-access-gateway` 

目录下执行 `mvn package nutzboot:shade -Dmaven.test.skip=true`

* `budiot-java-server/budiot-access/budiot-access-processor` 

目录下执行 `mvn package nutzboot:shade -Dmaven.test.skip=true`


* `budiot-java-server/budiot-server` 

目录下执行 `mvn package nutzboot:shade -Dmaven.test.skip=true`

### 项目运行


* 使用默认配置文件 

`nohup java -jar budiot.jar >/dev/null 2>&1 &`

* 指定jar中配置文件 

`nohup java -jar -Dnutz.profiles.active=pro -Xmx450m budiot.jar >/dev/null 2>&1 &`

* 加载文件夹中配置文件 

`nohup java -jar -Dnutz.boot.configure.yaml.dir=/data/blend/ -Xmx450m budiot.jar >/dev/null 2>&1 &`


## 前端打包

`pnpm run build`

## nginx部署

* 详见源码 `init` 目录下的 `demo.budiot.com.conf` 配置文件