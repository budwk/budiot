# Packaging and Deployment

## Backend Packaging

### Install Dependencies
* Execute `mvn install` in the `budiot-java-server` directory

### Project Packaging

* `budiot-java-server/budiot-access/budiot-access-gateway` 

Execute `mvn package nutzboot:shade -Dmaven.test.skip=true` in this directory

* `budiot-java-server/budiot-access/budiot-access-processor` 

Execute `mvn package nutzboot:shade -Dmaven.test.skip=true` in this directory

* `budiot-java-server/budiot-server` 

Execute `mvn package nutzboot:shade -Dmaven.test.skip=true` in this directory

### Project Execution

* Using default configuration file 

`nohup java -jar budiot.jar >/dev/null 2>&1 &`

* Specifying configuration file in jar 

`nohup java -jar -Dnutz.profiles.active=pro -Xmx450m budiot.jar >/dev/null 2>&1 &`

* Loading configuration file from folder 

`nohup java -jar -Dnutz.boot.configure.yaml.dir=/data/blend/ -Xmx450m budiot.jar >/dev/null 2>&1 &`

## Frontend Packaging

`pnpm run build`

## Nginx Deployment

* See the `demo.budiot.com.conf` configuration file in the `init` directory of the source code for details 