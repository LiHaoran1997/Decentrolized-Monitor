# 去中心化服务监控

## 系统设计架构

我们结合了区块链技术，用区块链的共识机制代替了服务注册节点的分布式一致性协议，同时也改变了中心化的服务监控思路，提出了面向跨信任域场景的服务质量监控框架。最后我们在开源组件Spring Cloud的基础上，结合联盟链Hyperledger Fabric，开发了一整套去中心化环境下的、面向跨信任域的服务质量监控框架。

此项目为架构中的服务监控模块

![nacos-1](https://github.com/modriclee/Decentrolized-ServiceRegistry/blob/master/nacos-架构图.jpg?raw=true)

![nacos-2](https://github.com/modriclee/Decentrolized-ServiceRegistry/blob/master/nacos-架构图2.jpg?raw=true)

服务注册见Decentrolized-monitor仓库

## 运行方法

### 方法一：编译安装

#### 1.安装Hyperledger fabric 1.1版本

#### 2.启动区块链

```
cd fabric-nodejs
./runApp.sh   #启动配置好的fabric集群，kafka/zookeeper/CA/order/peer
cd new_scripts
./channel.sh  #创建通道并加入通道
./install_monitor.sh #安装并实例化链码
```

#### 3.运行

```
#修改application.properties 配置ip地址以及服务注册地址
vi monitor/src/main/resources/application.properties

#编译项目
mvn clean 
mvn install

```

### 方法二：从镜像安装（推荐）

docker-compose文件：

```
version: '2'
services:
  monitor1:
    image: registry.cn-hangzhou.aliyuncs.com/nacos-fabric/monitor:test-2.0.1
    environment:
      SERVER_PORT: '5001'  #端口
    network_mode: host
    extra_hosts:  #所有监控集群地址
    - puwei-1.novalocal:10.77.70.173
    - puwei-2.novalocal:10.77.70.174
    - puwei-3.novalocal:10.77.70.175
    - puwei-4.novalocal:10.77.70.176
    - puwei-5.novalocal:10.77.70.177
    - puwei-6.novalocal:10.77.70.178
    - puwei-7.novalocal:10.77.70.179
    - puwei-8.novalocal:10.77.70.180
    - puwei-9.novalocal:10.77.70.181
    - puwei-10.novalocal:10.77.70.182
    - puwei-11.novalocal:10.77.70.183
    - puwei-12.novalocal:10.77.70.184
    - puwei-13.novalocal:10.77.70.185
    - puwei-14.novalocal:10.77.70.186
    - puwei-15.novalocal:10.77.70.187
    ports:
    - 5001:5001/tcp
```

```
docker-compose -f  xxxxxxx.yml up  #启动镜像 
docker-compose -f  xxxxxxx.yml down  #关闭镜像 
```

