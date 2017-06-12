集成jboss resteasy支持restful协议

### 功能支持
1. 支持rpc单独进程和部署到servlet容器中
2. 完全支持原有服务治理功能
3. 支持rpc request/response的attachment机制
4. 完全支持rpc filter机制
5. rest服务编程完全按照JAX-RS代码方式编写

### 快速入门

服务接口

```java
@Path("/rest")
public interface HelloResource{
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  List<User> hello(@CookieParam("uid") int uid);

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
```

服务实现

```java
public class RestHelloResource implements HelloResource{

  public List<User> hello(int id){
    return Arrays.asList(new User(id, "de"));
  }

  @Override
  public String testPrimitiveType(){
    return "helloworld";
  }

  @Override
  public Response add(int id, String name){
    return Response.ok().cookie(new NewCookie("ck", String.valueOf(id))).entity(new User(id, name)).build();
  }

  @Override
  public void testException(){
    throw new UnsupportedOperationException("unsupport");
  }

}
```

### 配置restserver

#### 独立rpc进程方式

`<motan:protocol name="restful" endpointFactory="netty" />`

#### 集成到java应用服务器中(如部署到tomcat中)

此时需要注意contextpath问题

`<motan:protocol name="restful" endpointFactory="servlet" />` 服务端还需配置web.xml如下:

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

此时如果使用rpc客户端，则需要注意contextpath配置：`<motan:protocol name="restful" contextpath="/serverContextPath/dispatcherServletUrlPattern" />`,
假如服务端部署的ContextPath为`/myserver`,servlet的url-pattern为`/servlet/*`,则客户端的contextpath则应配置为`/myserver/servlet`