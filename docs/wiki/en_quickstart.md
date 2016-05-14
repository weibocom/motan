- [Quickstart](#)
  - [Using Motan in single machine environment](#using-motan-in-single-machine-environment)
  - [Using Motan in cluster environment](#using-motan-in-cluster-environment)
    - [Using Consul as the registry](#using-consul-as-registry)
    - [Using ZooKeeper as the registry](#using-zookeeper-as-registry)

The quick start gives very basic example of running server and client on the same machine. For more details about using and developing Motan, please jump to [Documents](en_userguide).

> The minimum requirements to run the quick start are:
>  * JDK 1.7 or above.
>  * A java-based project management software like [Maven][maven] or [Gradle][gradle].

## <a id="peer-to-peer"></a>Using Motan in single machine environment


1. Add dependencies to pom.

   ```xml
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-core</artifactId>
        <version>0.1.1</version>
    </dependency>
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-transport-netty</artifactId>
        <version>0.1.1</version>
    </dependency>
    
    <!-- dependencies blow were only needed for spring-based features -->
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-springsupport</artifactId>
        <version>0.1.1</version>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>4.2.4.RELEASE</version>
    </dependency>
   ```

2. Create an interface for both service provider and consumer.

    `src/main/java/quickstart/FooService.java`  

    ```java
    package quickstart;

    public interface FooService {
        public String hello(String name);
    }
    ```

3. Write an implementation, create and start RPC Server.
    
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
    
    Execute main function in Server will start a Motan server listening on port 8002.

4. Create and start RPC Client.

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
    
    Execute main function in Client will invoke the remote service and print response.
    
## <a id="cluster"></a>Using Motan in cluster environment

In cluster environment, the external service discovery components such as Consul or ZooKeeper is needed to support the use of Motan.


### <a id="consul"></a>Using Consul as registry

#### <a id="consul-start"></a>Install and Start Consul

##### Install（[Official Document](https://www.consul.io/intro/getting-started/install.html)）
    
    # Taking Linux as an example
    wget https://releases.hashicorp.com/consul/0.6.4/consul_0.6.4_linux_amd64.zip
    unzip consul_0.6.4_linux_amd64.zip
    sudo mv consul /bin
    
##### Start（[Official Document](https://www.consul.io/intro/getting-started/agent.html)）

    Starting the test environment：
    consul agent -dev
    
UI backend [http://localhost:8500/ui](http://localhost:8500/ui)

#### <a id="motan-consul"></a>Motan-Consul configuration

1. Add motan-registry-consul in the pom of server and client.

    ```xml
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-registry-consul</artifactId>
        <version>0.1.1</version>
    </dependency>
    ```

2. Add the definition of consul registry in the configuration of server and client.

    ```xml
    <motan:registry regProtocol="consul" name="my_consul" address="127.0.0.1:8500"/>
    ```   

3. Change the way of service discovery to registry in the configuration of server and client.

    server:

    ```xml
    <motan:service interface="quickstart.FooService" ref="serviceImpl" registry="my_consul" export="8002" />
    ```
	
    client:

    ```xml
    <motan:referer id="remoteService" interface="quickstart.FooService" registry="my_consul"/>
    ```

4. After the server starts, you SHOULD call heartbeat switcher explicitly in order to start heartbeat for Consul.

    ```java
    MotanSwitcherUtil.setSwitcherValue(MotanConstants.REGISTRY_HEARTBEAT_SWITCHER, true)
    ```

5. Go to [UI backend](http://localhost:8500/ui). Verify whether the service is normal.

6. Start client, call service.

### <a id="zookeeper"></a>Using ZooKeeper as registry

#### <a id="zookeeper-start"></a>Install and Start ZooKeeper([Official Document](https://zookeeper.apache.org/doc/trunk/zookeeperStarted.html))

Install and start ZooKeeper:

    wget http://mirrors.cnnic.cn/apache/zookeeper/zookeeper-3.4.8/zookeeper-3.4.8.tar.gz
    tar zxvf zookeeper-3.4.8.tar.gz
    
    cd zookeeper-3.4.8/conf/
    cp zoo_sample.cfg zoo.cfg
    
    cd ../
    sh bin/zkServer.sh start

#### <a id="motan-zookeeper"></a>Motan-ZooKeeper configuration

1. Add motan-registry-zookeeper in the pom of server and client.

    ```xml
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-registry-zookeeper</artifactId>
        <version>0.1.1</version>
    </dependency>
    ```

2. Add the definition of ZooKeeper registry in the configuration of server and client.

    single node ZooKeeper:  
    
    ```xml
    <motan:registry regProtocol="zookeeper" name="my_zookeeper" address="127.0.0.1:2181"/>
    ```
    
    multi-nodes ZooKeeper:

    ```xml
    <motan:registry regProtocol="zookeeper" name="my_zookeeper" address="127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183"/>
    ```
    
3. Change the way of service discovery to registry in the configuration of server and client.

    server:

    ```xml
    <motan:service interface="quickstart.FooService" ref="serviceImpl" registry="my_zookeeper" export="8002" />
    ```
	
    client:

    ```xml
    <motan:referer id="remoteService" interface="quickstart.FooService" registry="my_zookeeper"/>
    ```

4. After the server starts, you SHOULD call heartbeat switcher explicitly in order to start heartbeat for Zookeeper.

    ```java
    MotanSwitcherUtil.setSwitcherValue(MotanConstants.REGISTRY_HEARTBEAT_SWITCHER, true)
    ```

5. Start client, call service.


[maven]:https://maven.apache.org
[gradle]:http://gradle.org

