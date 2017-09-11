- [快速入门](#)
  - [简单调用示例](#简单调用示例)
    - [同步调用](#同步调用)
    - [异步调用](#异步调用)
  - [集群调用示例](#集群调用示例)
    - [使用Consul作为注册中心](#使用consul作为注册中心)
    - [使用ZooKeeper作为注册中心](#使用zookeeper作为注册中心)
  - [其他调用示例](#其他调用示例)
    - [提供YAR协议服务](#提供yar协议服务)
    - [使用注解方式配置motan](#使用注解方式配置motan)
    - [使用Restful协议](#使用restful协议)
  - [使用OpenTracing](#使用opentracing)

快速入门中会给出一些基本使用场景下的配置方式，更详细的使用文档请参考[用户指南](zh_userguide).

> 如果要执行快速入门介绍中的例子，你需要:
>  * JDK 1.7或更高版本。
>  * java依赖管理工具，如[Maven][maven]或[Gradle][gradle]。

## <a id="peer-to-peer"></a>简单调用示例

### <a id="synccall"></a>同步调用

1. 在pom中添加依赖

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
    
    <!-- only needed for spring-based features -->
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
    
### <a id="asynccall"></a>异步调用

异步调用与同步调用基本配置完全一样，只需要在接口类中加上@MotanAsync注解，然后client端稍作修改。server端不需要做任何修改。具体步骤如下：

1. 在接口类上加@MotanAsync注解

    ```java
    package quickstart;
    
    @MotanAsync
    public interface FooService {
        public String hello(String name);
    }
    ```

2. 编译时，Motan自动生成异步service类，生成路径为target/generated-sources/annotations/，生成的类名为service名加上Async，例如service类名为FooService.java,则自动生成的类名为FooServiceAsync.java。
另外，需要将motan自动生产类文件的路径配置为项目source path，可以使用maven plugin或手动配置。pom.xml配置如下：


    ```xml
    <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>build-helper-maven-plugin</artifactId>
        <version>RELEASE</version>
        <executions>
            <execution>
                <phase>generate-sources</phase>
                <goals>
                    <goal>add-source</goal>
                </goals>
                <configuration>
                    <sources>
                        <source>${project.build.directory}/generated-sources/annotations</source>
                    </sources>
                </configuration>
            </execution>
        </executions>
    </plugin>
    ```

3. 在client端配置motan_client.xml时，在同步调用配置的基础上，只需要修改referer的interface为Motan自动生成的接口类即可。

    ```xml
    <motan:referer id="remoteService" interface="quickstart.FooServiceAsync" directUrl="localhost:8002"/>
    ```

4. 异步使用方式如下：

    ```java
    public static void main(String[] args) {
        ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] {"classpath:motan_client.xml"});

        FooServiceAsync service = (FooServiceAsync) ctx.getBean("remoteService");

        // sync call
        System.out.println(service.hello("motan"));

        // async call
        ResponseFuture future = service.helloAsync("motan async ");
        System.out.println(future.getValue());

        // multi call
        ResponseFuture future1 = service.helloAsync("motan async multi-1");
        ResponseFuture future2 = service.helloAsync("motan async multi-2");
        System.out.println(future1.getValue() + ", " + future2.getValue());

        // async with listener
        FutureListener listener = new FutureListener() {
            @Override
            public void operationComplete(Future future) throws Exception {
                System.out.println("async call "
                        + (future.isSuccess() ? "sucess! value:" + future.getValue() : "fail! exception:"
                                + future.getException().getMessage()));
            }
        };
        ResponseFuture future3 = service.helloAsync("motan async multi-1");
        ResponseFuture future4 = service.helloAsync("motan async multi-2");
        future3.addListener(listener);
        future4.addListener(listener);
    }
    ```

具体代码可以参考demo模块

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
        <version>RELEASE</version>
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
        <version>RELEASE</version>
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

## <a id="other"></a>其他调用示例

### <a id="motan-yar"></a>提供YAR协议服务
    
[YAR](https://github.com/laruence/yar)协议是php的一个rpc扩展，motan框架可以提供yar协议的RPC服务
1、引入motan-protocol-yar.jar

   ```xml
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-protocol-yar</artifactId>
        <version>RELEASE</version>
    </dependency>
   ```
    
2、在服务接口类上增加注解@YarConfig,声明服务的uri

   ```java
    @YarConfig(path = "/openapi/yarserver/test")
    public interface YarService {
        public String hello(String name);
    ｝
   ```
    
3、配置protocol的name="yar" 

   ```xml
    <motan:protocol id="demoYar" name="yar" .../>
   ```
    
4、配置service的export，使用yar协议提供服务
    
   ```xml
    <motan:service interface="com.weibo.motan.demo.service.YarService"
       export="demoYar:8003" .../>
   ```
    
具体配置见motan-demo模块
YAR协议使用[yar-java](https://github.com/weibocom/yar-java)进行解析，java作为YAR client时可以直接使用

### <a id="motan-annotation"></a>使用注解方式配置motan
#### server端配置

1、声明Annotation用来指定需要解析的包名

   ```java
    @Bean
    public AnnotationBean motanAnnotationBean() {
        AnnotationBean motanAnnotationBean = new AnnotationBean();
        motanAnnotationBean.setPackage("com.weibo.motan.demo.server");
        return motanAnnotationBean;
    }
   ```

2、配置ProtocolConfig、RegistryConfig、BasicServiceConfig的bean对象，功能与xml配置中的protocol、registry、basicService标签一致。

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
    
3、service的实现类上添加@MotanService注解，注解的配置参数与xml配置方式的service标签一致。

   ```java
    @MotanService(export = "demoMotan:8002")
    public class MotanDemoServiceImpl implements MotanDemoService {

        public String hello(String name) {
            System.out.println(name);
            return "Hello " + name + "!";
        }
    }
   ```
    
4、使用[spring-boot](https://github.com/spring-projects/spring-boot)启动服务

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
    
server端详细配置请参考motan-demo模块

#### client端配置
1、声明Annotation、protocolConfig、RegistryConfig的配置bean。方式与server端配置类似。

2、配置basicRefererConfig bean

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
    
3、在使用motan service 的对象上添加@MotanReferer注解，注册配置与xml方式的referer标签一致

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
    
4、使用spring-boot启动client

   ```java
    @EnableAutoConfiguration
    @SpringBootApplication
    public class SpringBootRpcClientDemo {

        public static void main(String[] args) {
            SpringApplication.run(SpringBootRpcClientDemo.class, args);
        }
    }
   ```
    
client端详细配置请参考motan-demo模块

### <a id="restful-protocol"></a>使用restful协议
#### 功能支持
  1. 支持rpc单独进程和部署到servlet容器中
  2. 完全支持原有服务治理功能
  3. 支持rpc request/response的attachment机制
  4. 完全支持rpc filter机制
  5. rest服务编程完全按照JAX-RS代码方式编写

#### 前置条件
  引入motan-protocol-restful包
   ```xml
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-protocol-restful</artifactId>
        <version>RELEASE</version>
    </dependency>
   ```

#### 接口声明与实现

服务接口

   ```java
    @Path("/rest")
    public interface RestfulService {
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        List<User> getUsers(@QueryParam("uid") int uid);
    
        @GET
        @Path("/primitive")
        @Produces(MediaType.TEXT_PLAIN)
        String testPrimitiveType();
    
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.APPLICATION_JSON)
        Response add(@FormParam("id") int id, @FormParam("name") String name);
    
        @GET
        @Path("/exception")
        @Produces(MediaType.APPLICATION_JSON)
        void testException();
    }
   ```

服务实现

   ```java
    public class RestfulServerDemo implements RestfulService {
           
        @Override
        public List<User> getUsers(@CookieParam("uid") int uid) {
            return Arrays.asList(new User(uid, "name" + uid));
        }
    
        @Override
        public String testPrimitiveType() {
            return "helloworld!";
        }
    
        @Override
        public Response add(@FormParam("id") int id, @FormParam("name") String name) {
            return Response.ok().cookie(new NewCookie("ck", String.valueOf(id))).entity(new User(id, name)).build();
        }
    
        @Override
        public void testException() {
            throw new UnsupportedOperationException("unsupport");
        }
    }
   ```

#### 配置restserver

##### 独立rpc进程方式

server端配置：

   ```xml
        <bean id="motanDemoServiceImpl" class="com.weibo.motan.demo.server.RestfulServerDemo"/>
    
        <motan:registry regProtocol="local" name="registry" />
    
        <motan:protocol id="demoRest" name="restful" endpointFactory="netty"/>
    
        <motan:basicService export="demoRest:8004" group="motan-demo-rpc" module="motan-demo-rpc"
                            application="myMotanDemo" registry="registry" id="serviceBasicConfig"/>
    
        <motan:service interface="com.weibo.motan.demo.service.RestfulService"
                       ref="motanDemoServiceImpl" basicService="serviceBasicConfig"/>
   ```

client端配置：

   ```xml
        <motan:registry regProtocol="direct" name="registry" address="127.0.0.1:8004"/>
    
        <!-- restful 协议 -->
        <motan:protocol id="restfulProtocol" name="restful" endpointFactory="netty"/>
    
        <!-- 通用referer基础配置 -->
        <motan:basicReferer requestTimeout="1000" group="motan-demo-rpc" module="motan-demo-rpc"
                            application="myMotanDemo" protocol="restfulProtocol" registry="registry"
                            id="motantestClientBasicConfig" />
    
        <!-- 使用 restful 协议-->
        <motan:referer id="restfulReferer" interface="com.weibo.motan.demo.service.RestfulService"
                       basicReferer="motantestClientBasicConfig"/>
   ```


##### 集成到java应用服务器中(如部署到tomcat中)

此时需要注意contextpath问题

   `<motan:protocol name="restful" endpointFactory="servlet" />` 

服务端还需配置web.xml如下:

   ```xml

    <!-- 此filter必须在spring ContextLoaderFilter之前 -->
    <listener>
        <listener-class>com.weibo.api.motan.protocol.restful.support.servlet.RestfulServletContainerListener</listener-class>
    </listener>

    <servlet>
        <servlet-name>dispatcher</servlet-name>
        <servlet-class>org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher</servlet-class>
        <load-on-startup>1</load-on-startup>
        <init-param>
            <param-name>resteasy.servlet.mapping.prefix</param-name>
            <param-value>/servlet</param-value>  <!-- 此处实际为servlet-mapping的url-pattern，具体配置见resteasy文档-->
        </init-param>
    </servlet>

    <servlet-mapping>
        <servlet-name>dispatcher</servlet-name>
        <url-pattern>/servlet/*</url-pattern>
    </servlet-mapping>
   ```

集成到java应用服务器的方式（servlet方式）适合不同语言直接http调用，需要注意url中contextpath的问题。推荐使用rpc进程方式

java作为client端调用时，推荐server端同时导出restful和motan两种协议，java使用motan协议进行调用，其他语言使用标准http协议调用。

详细请参考motan-demo模块中的RestfulServerDemo、RestfulClient

## <a id="opentracing"></a>使用OpenTracing

Motan通过filter的SPI扩展机制支持[OpenTracing](http://opentracing.io)，可以支持任何实现了OpenTracing标准的trace实现。使用OpenTracing需要以下步骤。

1、引入filter-opentracing扩展

   ```xml
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>filter-opentracing</artifactId>
        <version>RELEASE</version>
    </dependency>
   ```

2、如果第三方trace工具声明了io.opentracing.Tracer的SPI扩展，直接引入第三方trace的jar包即可。如果第三方没有声明，则转第三步。

3、自定义一个TracerFactory实现TracerFactory接口，通过getTracer()来获取不同tracer实现。设置OpenTracingContext的tracerFactory为自定义的TracerFactory即可。

可参考filter-opentracing模块src/test/java/com.weibo.api.motan.filter.opentracing.zipkin.demo包下的server端和client端实现。
