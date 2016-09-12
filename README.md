# Motan
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/weibocom/motan/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.weibo/motan.svg?label=Maven Central)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.weibo%22%20AND%20motan)
[![Build Status](https://img.shields.io/travis/weibocom/motan/master.svg?label=Build)](https://travis-ci.org/weibocom/motan)

# Overview
Motan is a remote procedure call(RPC) framework for rapid development of high performance distributed services.

# Features
- Create distributed services without writing extra code.
- Provides cluster support and integrate with popular service discovery services like [Consul][consul] or [Zookeeper][zookeeper]. 
- Supports advanced scheduling features like weighted load-balance, scheduling cross IDCs, etc.
- Optimization for high load scenarios, provides high availability in production environment.

# Quick Start

The quick start gives very basic example of running client and server on the same machine. For the detailed information about using and developing Motan, please jump to [Documents](#documents).

> The minimum requirements to run the quick start are: 
>  * JDK 1.7 or above
>  * A java-based project management software like [Maven][maven] or [Gradle][gradle]

1. Add dependencies to pom.

   ```xml
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-core</artifactId>
        <version>0.2.1</version>
    </dependency>
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-transport-netty</artifactId>
        <version>0.2.1</version>
    </dependency>
    
    <!-- dependencies blow were only needed for spring-based features -->
    <dependency>
        <groupId>com.weibo</groupId>
        <artifactId>motan-springsupport</artifactId>
        <version>0.2.1</version>
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
        <!-- exporting service by motan -->
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
    
    Execute main function in Server will start a motan server listening on port 8002.

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
    

# Documents

* [Wiki](https://github.com/weibocom/motan/wiki)
* [Wiki(中文)](https://github.com/weibocom/motan/wiki/zh_overview)

# Contributors

* maijunsheng([@maijunsheng](https://github.com/maijunsheng))
* fishermen([@hustfisher](https://github.com/hustfisher))
* TangFulin([@tangfl](https://github.com/tangfl))
* bodlyzheng([@bodlyzheng](https://github.com/bodlyzheng))
* jacawang([@jacawang](https://github.com/jacawang))
* zenglingshu([@zenglingshu](https://github.com/zenglingshu))
* Sugar Zouliu([@lamusicoscos](https://github.com/lamusicoscos))
* tangyang([@tangyang](https://github.com/tangyang))
* olivererwang([@olivererwang](https://github.com/olivererwang))
* jackael([@jackael9856](https://github.com/jackael9856))
* Ray([@rayzhang0603](https://github.com/rayzhang0603))
* r2dx([@half-dead](https://github.com/half-dead))
* Jake Zhang([sunnights](https://github.com/sunnights))
* axb([@qdaxb](https://github.com/qdaxb))
* wenqisun([@wenqisun](https://github.com/wenqisun))
* fingki([@fingki](https://github.com/fingki))
* 午夜([@sumory](https://github.com/sumory))
* guanly([@guanly](https://github.com/guanly))
* Di Tang([@tangdi](https://github.com/tangdi))
* 肥佬大([@feilaoda](https://github.com/feilaoda))
* 小马哥([@andot](https://github.com/andot))

# License

Motan is released under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

[maven]:https://maven.apache.org
[gradle]:http://gradle.org
[consul]:http://www.consul.io
[zookeeper]:http://zookeeper.apache.org


