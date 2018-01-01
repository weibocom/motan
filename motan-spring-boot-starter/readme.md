# Motan Spring Boot Starter

## 如何使用
1. 在 Spring Boot 项目中加入```motan-spring-boot-starter```依赖

    ```Maven```
    ```xml
    <dependency>
       <groupId>com.weibo</groupId>
       <artifactId>motan-spring-boot-starter</artifactId>
       <version>1.1.0</version>
    </dependency>
    ```
    ```Gradle```
    ```xml
    compile 'com.weibo:motan-spring-boot-starter:1.1.0'
    ```
2. 添加配置
    ```xml
    motan.xx.url= 
    motan.xx.url= 
    motan.xx.url= 
    # ...其他配置（可选，不是必须的，使用内嵌数据库的话上述三项也可省略不填）
    ```

## 配置属性
Motan Spring Boot Starter 配置属性的名称完全遵照 Motan，你可以通过 Spring Boot 配置文件来配置Motan RPC，如果没有配置则使用默认值。

- 注册中心 配置
```xml
spring.datasource.druid.url= # 或spring.datasource.url= 
spring.datasource.druid.username= # 或spring.datasource.username=
spring.datasource.druid.password= # 或spring.datasource.password=
spring.datasource.druid.driver-class-name= #或 spring.datasource.driver-class-name=
```
- 服务端 配置
```
spring.datasource.druid.initial-size=
spring.datasource.druid.max-active=
spring.datasource.druid.min-idle=
spring.datasource.druid.max-wait=
spring.datasource.druid.pool-prepared-statements=
spring.datasource.druid.max-pool-prepared-statement-per-connection-size= 
spring.datasource.druid.max-open-prepared-statements= #和上面的等价
spring.datasource.druid.validation-query=
spring.datasource.druid.validation-query-timeout=
spring.datasource.druid.test-on-borrow=
spring.datasource.druid.test-on-return=
spring.datasource.druid.test-while-idle=
spring.datasource.druid.time-between-eviction-runs-millis=
spring.datasource.druid.min-evictable-idle-time-millis=
spring.datasource.druid.max-evictable-idle-time-millis=
spring.datasource.druid.filters= #配置多个英文逗号分隔
....//more
```
- 客户端 配置
```
spring.datasource.druid.initial-size=
spring.datasource.druid.max-active=
spring.datasource.druid.min-idle=
spring.datasource.druid.max-wait=
spring.datasource.druid.pool-prepared-statements=
spring.datasource.druid.max-pool-prepared-statement-per-connection-size= 
spring.datasource.druid.max-open-prepared-statements= #和上面的等价
spring.datasource.druid.validation-query=
spring.datasource.druid.validation-query-timeout=
spring.datasource.druid.test-on-borrow=
spring.datasource.druid.test-on-return=
spring.datasource.druid.test-while-idle=
spring.datasource.druid.time-between-eviction-runs-millis=
spring.datasource.druid.min-evictable-idle-time-millis=
spring.datasource.druid.max-evictable-idle-time-millis=
spring.datasource.druid.filters= #配置多个英文逗号分隔
....//more
```
- 监控配置
```
# WebStatFilter配置，说明请参考Druid Wiki，配置_配置WebStatFilter
spring.datasource.druid.web-stat-filter.enabled= #是否启用StatFilter默认值true
spring.datasource.druid.web-stat-filter.url-pattern=
spring.datasource.druid.web-stat-filter.exclusions=
spring.datasource.druid.web-stat-filter.session-stat-enable=
spring.datasource.druid.web-stat-filter.session-stat-max-count=
spring.datasource.druid.web-stat-filter.principal-session-name=
spring.datasource.druid.web-stat-filter.principal-cookie-name=
spring.datasource.druid.web-stat-filter.profile-enable=

# StatViewServlet配置，说明请参考Druid Wiki，配置_StatViewServlet配置
spring.datasource.druid.stat-view-servlet.enabled= #是否启用StatViewServlet默认值true
spring.datasource.druid.stat-view-servlet.url-pattern=
spring.datasource.druid.stat-view-servlet.reset-enable=
spring.datasource.druid.stat-view-servlet.login-username=
spring.datasource.druid.stat-view-servlet.login-password=
spring.datasource.druid.stat-view-servlet.allow=
spring.datasource.druid.stat-view-servlet.deny=

# Spring监控配置，说明请参考Druid Github Wiki，配置_Druid和Spring关联监控配置
spring.datasource.druid.aop-patterns= # Spring监控AOP切入点，如x.y.z.service.*,配置多个英文逗号分隔
# 如果spring.datasource.druid.aop-patterns要代理的类没有定义interface请设置spring.aop.proxy-target-class=true
```
Druid Spring Boot Starter 不仅限于对以上配置属性提供支持，[```DruidDataSource```](https://github.com/alibaba/druid/blob/master/src/main/java/com/alibaba/druid/pool/DruidDataSource.java) 内提供```setter```方法的可配置属性都将被支持。你可以参考WIKI文档或通过IDE输入提示来进行配置。配置文件的格式你可以选择```.properties```或```.yml```，效果是一样的，在配置较多的情况下推荐使用```.yml```。


## 设计目的

* 减少引入motan时关注的pom依赖

* 将motan的配置文件统一化

* 将motan与spring boot监控体系打通

## IDE 提示支持
![](https://raw.githubusercontent.com/lihengming/java-codes/master/shared-resources/github-images/druid-spring-boot-starter-ide-hint.jpg)

## 演示
克隆项目，运行```test```包内的```DemoApplication```。

## 参考
[Druid Wiki](https://github.com/alibaba/druid/wiki/%E9%A6%96%E9%A1%B5)

[Spring Boot Reference](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
