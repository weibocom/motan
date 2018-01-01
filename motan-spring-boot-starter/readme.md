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

    服务端配置
    
    ```xml
    spring.motan.scan.package=${你的包名，包括consumer和provider}
    spring.motan.protocol.name=motan2
    spring.motan.registry.regProtocol=zookeeper
    spring.motan.registry.address=127.0.0.1:2181
    spring.motan.service.export=8002
    spring.motan.service.group=wsd-java
    spring.motan.service.application=motan-demo-server

    # ...其他配置（可选，不是必须的）
    spring.motan.protocol.haStrategy=failover
    spring.motan.protocol.loadbalance=roundrobin
    spring.motan.service.check=true
    ```
    
    客户端端配置
    ```xml
    spring.motan.scan.package=${你的包名，包括consumer和provider}
    spring.motan.protocol.name=motan2
    spring.motan.registry.regProtocol=zookeeper
    spring.motan.registry.address=127.0.0.1:2181
    spring.motan.referer.group=wsd-java
    spring.motan.referer.application=motan-demo-client

    # ...其他配置（可选，不是必须的）
    spring.motan.referer.retries=3
    spring.motan.referer.throwException=true
 
 3. 导出和消费
 
    服务端
    ```java
    package com.douyu.motan.demo.rpc;

    import com.douyu.motan.demo.api.std.StdResponse;
    import com.douyu.motan.demo.api.suggest.ContentWrapper;
    import com.douyu.motan.demo.api.suggest.SuggestService;
    import com.douyu.motan.demo.utils.JsonUtils;

    import java.net.URLEncoder;
    import javax.annotation.Resource;

    import com.weibo.api.motan.config.springsupport.annotation.MotanService;
    import lombok.Setter;
    import lombok.extern.slf4j.Slf4j;
    import org.jsoup.Jsoup;
    import org.springframework.web.client.RestTemplate;

    @MotanService
    @Setter
    @Slf4j
    public class SuggestServiceImpl implements SuggestService {

        @Resource
        private RestTemplate restTemplate;

        @Override
        public StdResponse<ContentWrapper> query(String keyword) {
            if (keyword == null) {
                return StdResponse.asError(StdResponse.CODE_BAD_REQUEST, "参数不合法", "参数不合法: " + keyword);
            }

            log.info("收到rpc请求: keyword={}", keyword);

            try {
                String url = "http://www.ximalaya.com/search/suggest?scope=all&kw=" + URLEncoder.encode(keyword, "UTF-8");

                String json = Jsoup.connect(url).ignoreContentType(true).execute().body();
                ContentWrapper res = JsonUtils.toObject(json, ContentWrapper.class);

                return StdResponse.asSuccess(res);
            } catch (Exception e) {
                log.error("服务器内部错误: keyword={}", keyword, e);
                return StdResponse.asError(StdResponse.CODE_INTERNAL_SERVER_ERROR, "服务器内部错误", "服务器内部错误: " + e.getMessage());
            }
        }
    }
    ```
 
    客户端
    ```java
    package com.douyu.motan.demo.controller;

    import com.douyu.motan.demo.api.std.StdRequest;
    import com.douyu.motan.demo.api.std.StdResponse;
    import com.douyu.motan.demo.api.suggest.Condition;
    import com.douyu.motan.demo.api.suggest.ContentWrapper;
    import com.douyu.motan.demo.api.suggest.SuggestService;

    import com.weibo.api.motan.config.springsupport.annotation.MotanReferer;
    import org.springframework.beans.BeanUtils;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RestController;

    @RestController
    public class SuggestController {

        @MotanReferer(basicReferer = "motanBasicReferer")
        private SuggestService suggestService;

        @GetMapping(value = "/search")
        public SearchRes search(String kw) {
            long timestamp = System.currentTimeMillis();
            Condition cond = new Condition();
            cond.setKeyword(kw);
            cond.setScope("all");
            StdRequest<Condition> req = new StdRequest<Condition>(timestamp, "", cond);
            StdResponse<ContentWrapper> response = suggestService.query(kw);
            ContentWrapper wrapper = response.getData();
            SearchRes searchRes = new SearchRes();
            if (wrapper != null) {
                BeanUtils.copyProperties(wrapper, searchRes);
            }
            return searchRes;
        }
    }
    ```

## 设计目的

* 减少引入motan时关注的pom依赖

* 将motan的配置文件统一化

* 将motan与spring boot监控体系打通

## 演示
克隆项目，运行```test```包内的```DemoApplication```。

## 参考

[Spring Boot Reference](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
