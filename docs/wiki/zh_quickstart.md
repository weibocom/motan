- [快速入门](#)
  - [简单调用示例](#简单调用示例)
  - [集群调用示例](#集群调用示例)
    - [使用Consul作为注册中心](#使用Consul作为注册中心)
    - [使用ZooKeeper作为注册中心](#使用Zookeeper作为注册中心)

快速入门中会给出一些基本使用场景下的配置方式，更详细的使用文档请参考[用户指南](zh_userguide).

> 如果要执行快速入门介绍中的例子，你需要:
>  * JDK 1.7或更高版本。
>  * java依赖管理工具，如[Maven][maven]或[Gradle][gradle]。

## <a id="peer-to-peer"></a>简单调用示例

1. 在pom中添加依赖

   ```xml
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-core</artifactId>
        <version>0.1.0</version>
    </dependency>
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-transport-netty</artifactId>
        <version>0.1.0</version>
    </dependency>
    
    <!-- only needed for spring-based features -->
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-springsupport</artifactId>
        <version>0.1.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>4.2.4.RELEASE</version>
    </dependency>
   ```

2. 为调用方和服务方创建公共接口。

    `src/main/java/quickstart/FooService.java`  

    ```java
    package quickstart;

    public interface FooService {
        public String hello(String name);
    }
    ```

3. 编写业务接口逻辑、创建并启动RPC Server。
    
    `src/main/java/quickstart/FooServiceImpl.java`  

    ```java
    package quickstart;

    public class FooServiceImpl implements FooService {
    
        public String hello(String name) {
            System.out.println(name + " invoked rpc service");
            return "hello " + name;
        }
    }
    ```

    `src/main/resources/motan_server.xml`
    
    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
    	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    	xmlns:motan="http://api.weibo.com/schema/motan"
    	xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.5.xsd
       http://api.weibo.com/schema/motan http://api.weibo.com/schema/motan.xsd">

        <!-- service implemention bean -->
        <bean id="serviceImpl" class="quickstart.FooServiceImpl" />
        <!-- exporting service by Motan -->
        <motan:service interface="quickstart.FooService" ref="serviceImpl" export="8002" />
    </beans>
    ```

       `src/main/java/quickstart/Server.java`
    
    ```java
    package quickstart;
    
    import org.springframework.context.ApplicationContext;
    import org.springframework.context.support.ClassPathXmlApplicationContext;
    
    public class Server {
    
        public static void main(String[] args) throws InterruptedException {
            ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:motan_server.xml");
            System.out.println("server start...");
        }
    }
    ```
  
    执行Server类中的main函数将会启动Motan服务，并监听8002端口.

4. 创建并执行RPC Client。

    `src/main/resources/motan_client.xml`

    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:motan="http://api.weibo.com/schema/motan"
    xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.5.xsd
       http://api.weibo.com/schema/motan http://api.weibo.com/schema/motan.xsd">

        <!-- reference to the remote service -->
        <motan:referer id="remoteService" interface="quickstart.FooService" directUrl="localhost:8002"/>
    </beans>
    ```

    `src/main/java/quickstart/Client.java`

    ```java
    package quickstart;

    import org.springframework.context.ApplicationContext;
    import org.springframework.context.support.ClassPathXmlApplicationContext;


    public class Client {
    
        public static void main(String[] args) throws InterruptedException {
            ApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:motan_client.xml");
            FooService service = (FooService) ctx.getBean("remoteService");
            System.out.println(service.hello("motan"));
        }
    }
    ```
    
    执行Client类中的main函数将执行一次远程调用，并输出结果。
    
    
## <a id="cluster"></a>集群调用示例

在集群环境下使用Motan需要依赖外部服务发现组件，目前支持consul或zookeeper。

### <a id="consul"></a>使用Consul作为注册中心

#### <a id="consul-start"></a>Consul安装与启动

##### 安装（[官方文档](https://www.consul.io/intro/getting-started/install.html)）
    
    # 这里以linux为例
    wget https://releases.hashicorp.com/consul/0.6.4/consul_0.6.4_linux_amd64.zip
    unzip consul_0.6.4_linux_amd64.zip
    sudo mv consul /bin
    
##### 启动（[官方文档](https://www.consul.io/intro/getting-started/agent.html)）

    测试环境启动：
    consul agent -dev
    
ui后台 [http://localhost:8500/ui](http://localhost:8500/ui)

#### <a id="motan-consul"></a>Motan-Consul配置

1. 在server和client中添加motan-registry-consul依赖

    ```xml
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-registry-consul</artifactId>
        <version>0.1.0</version>
    </dependency>
    ```

2. 在server和client的配置文件中分别增加consul registry定义。

    ```xml
    <motan:registry regProtocol="consul" name="my_consul" address="127.0.0.1:8500"/>
    ```   

3. 在Motan client及server配置改为通过registry服务发现。

    client 

    ```xml
        <motan:referer id="remoteService" interface="quickstart.FooService" registry="my_consul"/>
    ```

    server

    ```xml
        <motan:service interface="quickstart.FooService" ref="serviceImpl" registry="my_consul" export="8002" />
    ```

4. server程序启动后，需要显式调用心跳开关，注册到consul。

    ```java
    MotanSwitcherUtil.setSwitcherValue(MotanConstants.REGISTRY_HEARTBEAT_SWITCHER, true)
    ```

5. 进入[ui后台](http://localhost:8500/ui)查看服务是否正常提供调用

6. 启动client，调用服务

### <a id="zookeeper"></a>使用ZooKeeper作为注册中心

#### <a id="zookeeper-start"></a>ZooKeeper安装与启动([官方文档](https://zookeeper.apache.org/doc/trunk/zookeeperStarted.html))

单机版安装与启动

    wget http://mirrors.cnnic.cn/apache/zookeeper/zookeeper-3.4.8/zookeeper-3.4.8.tar.gz
    tar zxvf zookeeper-3.4.8.tar.gz
    
    cd zookeeper-3.4.8/conf/
    cp zoo_sample.cfg zoo.cfg
    
    cd ../
    sh bin/zkServer.sh start

#### <a id="motan-zookeeper"></a>Motan-ZooKeeper配置

1. 在server和client中添加motan-registry-zookeeper依赖

    ```xml
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-registry-zookeeper</artifactId>
        <version>0.1.0</version>
    </dependency>
    ```

2. 在server和client的配置文件中分别增加zookeeper registry定义。 

    zookeeper为单节点  
    
    ```xml
    <motan:registry regProtocol="zookeeper" name="my_zookeeper" address="127.0.0.1:2181"/>
    ```
    
    zookeeper多节点集群  

    ```xml
    <motan:registry regProtocol="zookeeper" name="my_zookeeper" address="127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183"/>
    ```
    
3. 在Motan client及server配置改为通过registry服务发现。

    client

    ```xml
    <motan:referer id="remoteService" interface="quickstart.FooService" registry="my_zookeeper"/>
    ```

    server

    ```xml
    <motan:service interface="quickstart.FooService" ref="serviceImpl" registry="my_zookeeper" export="8002" />
    ```

4. server程序启动后，需要显式调用心跳开关，注册到zookeeper。

    ```java
    MotanSwitcherUtil.setSwitcherValue(MotanConstants.REGISTRY_HEARTBEAT_SWITCHER, true)
    ```

5. 启动client，调用服务


[maven]:https://maven.apache.org
[gradle]:http://gradle.org

