# Motan Spring Boot Starter

## 如何使用
1. 在 Spring Boot 项目中加入```motan-spring-boot-starter```依赖

    ```Maven```
    ```xml
    <dependency>
       <groupId>com.weibo</groupId>
       <artifactId>motan-spring-boot-starter</artifactId>
       <version>1.1.1-SNAPSHOT</version>
    </dependency>
    ```
    ```Gradle```
    ```xml
    compile 'com.weibo:motan-spring-boot-starter:1.1.1-SNAPSHOT'
    ```
2. 添加配置
    
    ```xml
    spring:
        motan:
            scanPackage: com.weibo.api.motan.demo
        protocol:
            name: motan2
        registry:
            regProtocol: zookeeper
            address: 127.0.0.1:2181
        service:
            group: wsd-java
            module: wsd-java
            application: motan-demo-server
            export: motanProtocolConfig:8003
        referer:
            group: wsd-java
            module: wsd-java
            application: motan-demo-client
            check: false
 
 3. 导出和消费
 
    服务端：参照测试包下面的[SuggestServiceImpl.java](https://github.com/konglz/motan/blob/master/motan-spring-boot-starter/src/test/java/com/weibo/api/motan/demo/server/SuggestServiceImpl.java)
 
    客户端: 参照测试包下面的[SuggestController.java](https://github.com/konglz/motan/blob/master/motan-spring-boot-starter/src/test/java/com/weibo/api/motan/demo/client/SuggestController.java)

## 设计目的

* 减少引入motan时关注的pom依赖

* 将motan的配置文件统一化

* 将motan与spring boot监控体系打通

## 演示
克隆项目，运行```test```包内的```AppLauncher```。

## 参考

[Spring Boot Reference](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
