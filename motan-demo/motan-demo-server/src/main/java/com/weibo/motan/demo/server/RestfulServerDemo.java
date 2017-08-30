package com.weibo.motan.demo.server;

import com.weibo.motan.demo.service.RestfulService;
import com.weibo.motan.demo.service.model.User;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

/**
 * Created by zhanglei28 on 2017/8/30.
 */
public class RestfulServerDemo implements RestfulService {
    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:motan_demo_server_restful.xml");
        System.out.println("restful server start...");
        Thread.sleep(Long.MAX_VALUE);
    }

    @Override
    public List<User> hello(@CookieParam("uid") int uid) {
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
