# BudIot 单应用版本

## 编译打包

* `mvn package nutzboot:shade`

## 部署运行

* 默认配置 

`nohup java -jar budiot.jar >/dev/null 2>&1 &`

* 指定jar中配置 

`nohup java -jar -Dnutz.profiles.active=pro -Xmx450m budiot.jar >/dev/null 2>&1 &`

* 加载文件夹中配置 

`nohup java -jar -Dnutz.boot.configure.yaml.dir=/data/blend/ -Xmx450m budiot.jar >/dev/null 2>&1 &`

## RocketMQ 启动

* 启动服务
`./mqnamesrv`

* 启动 broker（集群部署不要设置 autoCreateTopicEnable=true）
`./mqbroker -n localhost:9876 autoCreateTopicEnable=true`

* 生产环境（集群部署）创建 topic
`./mqadmin updatetopic -n localhost:9876 -t DemoTopic -c DefaultCluster -r 10 -w 10`

## MongoDB 启动

* 创建好数据文件夹
`mkdir -p /Users/wizzer/data/mongo`

* 启动 MongoDB
`mongod --dbpath /Users/wizzer/data/mongo`