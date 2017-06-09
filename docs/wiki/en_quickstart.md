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
        <version>RELEASE</version>
    </dependency>
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-transport-netty</artifactId>
        <version>RELEASE</version>
    </dependency>
    
    <!-- dependencies blow were only needed for spring-based features -->
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-springsupport</artifactId>
        <version>RELEASE</version>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>RELEASE</version>
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
        <version>RELEASE</version>
    </dependency>
    ```

2. Add the definition of consul registry in the configuration of server and client.

    ```xml
    <motan:registry regProtocol="consul" name="my_consul" address="127.0.0.1:8500"/>
    ```   

3. Change the way of service discovery to service discovery through registry in the configuration of server and client.

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
        <version>RELEASE</version>
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

## <a id="other"></a>Other invoke examples

### <a id="motan-yar"></a>Providing YAR protocol service
    
[YAR](https://github.com/laruence/yar) protocol is a rpc extension of php,motan framework can provide yar protocol for RPC services
1、add motan-protocol-yar.jar

   ```xml
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-protocol-yar</artifactId>
        <version>RELEASE</version>
    </dependency>
   ```
    
2、Add annotations @YarConfig to the service interface class to declare the uri of the service

   ```java
    @YarConfig(path = "/openapi/yarserver/test")
    public interface YarService {
        public String hello(String name);
    ｝
   ```
    
3、Configure protocol name = "yar" 

   ```xml
    <motan:protocol id="demoYar" name="yar" .../>
   ```
    
4、Configure the export of service, using yar protocol to provide services
    
   ```xml
    <motan:service interface="com.weibo.motan.demo.service.YarService"
       export="demoYar:8003" .../>
   ```
    
Check motan-demo module to get specific configuration.
YAR protocol use [yar-java](https://github.com/weibocom/yar-java) to parse，and can be use directly when using java as client

### <a id="motan-annotation"></a>Using the annotations to configure the motan
#### server:

1、Declare Annotation to specify the name of the package to be resolved

   ```java
    @Bean
    public AnnotationBean motanAnnotationBean() {
        AnnotationBean motanAnnotationBean = new AnnotationBean();
        motanAnnotationBean.setPackage("com.weibo.motan.demo.server");
        return motanAnnotationBean;
    }
   ```

2、Configure the bean object of ProtocolConfig, RegistryConfig,and BasicServiceConfig, which is consistent with the protocol, registry, and basicService tags in the xml configuration

   ```java
    @Bean(name = "demoMotan")
    public ProtocolConfigBean protocolConfig1() {
        ProtocolConfigBean config = new ProtocolConfigBean();
        config.setDefault(true);
        config.setName("motan");
        config.setMaxContentLength(1048576);
        return config;
    }

    @Bean(name = "registryConfig1")
    public RegistryConfigBean registryConfig() {
        RegistryConfigBean config = new RegistryConfigBean();
        config.setRegProtocol("local");
        return config;
    }

    @Bean
    public BasicServiceConfigBean baseServiceConfig() {
        BasicServiceConfigBean config = new BasicServiceConfigBean();
        config.setExport("demoMotan:8002");
        config.setGroup("testgroup");
        config.setAccessLog(false);
        config.setShareChannel(true);
        config.setModule("motan-demo-rpc");
        config.setApplication("myMotanDemo");
        config.setRegistry("registryConfig1");
        return config;
    }
   ```
    
3、Add the @MotanService annotation to the implementation class of the service. The configuration parameters of the annotation are the same as the service tag of the xml configuration.

   ```java
    @MotanService(export = "demoMotan:8002")
    public class MotanDemoServiceImpl implements MotanDemoService {

        public String hello(String name) {
            System.out.println(name);
            return "Hello " + name + "!";
        }
    }
   ```
    
4、Using [spring-boot](https://github.com/spring-projects/spring-boot) to boot service

   ```java
    @EnableAutoConfiguration
    @SpringBootApplication
    public class SpringBootRpcServerDemo {

        public static void main(String[] args) {
            System.setProperty("server.port", "8081");
            ConfigurableApplicationContext context =  SpringApplication.run(SpringBootRpcServerDemo.class, args);
          
        MotanSwitcherUtil.setSwitcherValue(MotanConstants.REGISTRY_HEARTBEAT_SWITCHER, true);
            System.out.println("server start...");
        }
    }
   ```
    
Check motan-demo module to get specific configuration

#### client:
1、Declare the configuration bean for Annotation, protocolConfig, and RegistryConfig. The server is configured similarly to the server.

2、Configuring basicRefererConfig bean

   ```java
    @Bean(name = "motantestClientBasicConfig")
    public BasicRefererConfigBean baseRefererConfig() {
        BasicRefererConfigBean config = new BasicRefererConfigBean();
        config.setProtocol("demoMotan");
        config.setGroup("motan-demo-rpc");
        config.setModule("motan-demo-rpc");
        config.setApplication("myMotanDemo");
        config.setRegistry("registry");
        config.setCheck(false);
        config.setAccessLog(true);
        config.setRetries(2);
        config.setThrowException(true);
        return config;
    }
   ```
    
3、Add the @MotanReferer annotation to the object that uses the motan service. The registration configuration is consistent with the referer tag in xml mode

   ```java
    @RestController
    public class HelloController {

        @MotanReferer(basicReferer = "motantestClientBasicConfig", group = "testgroup", directUrl = "127.0.0.1:8002")
        MotanDemoService service;

        @RequestMapping("/")
        @ResponseBody
        public String home() {
            String result = service.hello("test");
            return result;
        }
    }
   ```
    
4、Using spring-boot to boot client

   ```java
    @EnableAutoConfiguration
    @SpringBootApplication
    public class SpringBootRpcClientDemo {

        public static void main(String[] args) {
            SpringApplication.run(SpringBootRpcClientDemo.class, args);
        }
    }
   ```
    
Check motan-demo module to get specific configuration

## <a id="opentracing"></a>Using OpenTracing

Motan support [OpenTracing](http://opentracing.io)through the filter's SPI extension mechanism, which can support any trace implementation that implements the OpenTracing standard. The following steps are required to use OpenTracing.

1、add filter-opentracing to pom

   ```xml
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>filter-opentracing</artifactId>
        <version>RELEASE</version>
    </dependency>
   ```

2、If the third-party trace tool declares the io.opentracing.Tracer's SPI extension, you can directly introduce a third-party trace to the jar package. If the third party does not make a statement, turn to the third step.

3、Customize a TracerFactory implementation TracerFactory interface, through getTracer () to get different tracer implementation. Set the tracerFactory of the OpenTracingContext to a custom TracerFactory.

You can refer to the filter-opentracing module src / test / java / com.weibo.api.motan.filter.opentracing.zipkin.demo package under the server and client side to achieve.

