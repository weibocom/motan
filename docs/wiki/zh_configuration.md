
## 协议配置列表

\<motan:protocol/>

| Property name       | Type    | Default               | Comment                                                               |
|---------------------|---------|-----------------------|-----------------------------------------------------------------------|
| name                | String  |                       | 服务协议名                                                            |
| serialization       | String  | hessian2              | 序列化方式                                                            |
| payload             | int     |                       | 最大请求数据长度                                                      |
| buffer              | int     |                       | 缓存区大小                                                            |
| heartbeat           | int     |                       | 心跳间隔                                                              |
| transporter         | String  |                       | 网络传输方式                                                          |
| threads             | int     |                       | 线程池大小                                                            |
| iothreads           | int     | availableProcessors+1 | IO线程池大小                                                          |
| requestTimeout      | int     | 200                   | 请求超时                                                              |
| minClientConnection | int     | 2                     | client最小连接数                                                      |
| maxClientConnection | int     | 10                    | client最大连接数                                                      |
| minWorkerThread     | int     | 20                    | 最小工作pool线程数                                                    |
| maxWorkerThread     | int     | 200                   | 最大工作pool线程数                                                    |
| maxContentLength    | int     | 10M                   | 请求响应包的最大长度限制                                              |
| maxServerConnection | int     | 100000                | server支持的最大连接数                                                |
| poolLifo            | boolean | true                  | 连接池管理方式，是否lifo                                              |
| lazyInit            | boolean | false                 | 是否延迟init                                                          |
| endpointFactory     | boolean | motan                 | endpoint factory                                                      |
| cluster             | String  | default               | 采用哪种cluster的实现                                                 |
| loadbalance         | String  | activeWeight          | 负载均衡策略                                                          |
| haStrategy          | String  | failover              | 高可用策略                                                            |
| workerQueueSize     | String  | 0                     | Server工作队列大小                                                    |
| acceptConnections   | int     | 0                     | Server可接受连接数                                                    |
| proxy               | String  | jdk                   | proxy type, like jdk or javassist                                     |
| filter              | String  |                       | filter, 多个filter用","分割，blank String 表示采用默认的filter配置    |
| retries             | int     | 0                     | 调用失败时重试次数                                                    |
| async               | boolean | false                 | if the request is called async, a taskFuture result will be sent back |
| queueSize           | Int     |                       | 线程池队列大小                                                        |
| accepts             | Int     |                       | 最大接收连接数                                                        |
| dispatcher          | String  |                       | 信息线程模型派发方式                                                  |
| server              | String  |                       | 服务器端实现                                                          |
| client              | String  |                       | 客户端端实现                                                          |
| default             | boolean |                       | 是否缺省的配置                                                        |
| switcherService     | String  | localSwitcherService  |                                                                       |
| heartbeatFactory    | String  | motan                 |                                                                       |



## 注册中心配置列表

\<motan:registry/>

| Property name          | Type    | Default | Comment                      |
|------------------------|---------|---------|------------------------------|
| name                   | String  |         | 注册配置名称                 |
| regProtocol            | String  |         | 注册协议                     |
| address                | String  |         | 注册中心地址                 |
| port                   | int     | 0       | 注册中心缺省端口             |
| connectTimeout         | int     | 1000    | 注册中心连接超时时间(毫秒)   |
| requestTimeout         | int     | 200     | 注册中心请求超时时间(毫秒)   |
| registrySessionTimeout | int     | 60s     | 注册中心会话超时时间(毫秒)   |
| registryRetryPeriod    | int     | 30s     | 失败后重试的时间间隔         |
| check                  | boolean | true    | 启动时检查失败后是否仍然启动 |
| register               | boolean | true    | 在该注册中心上服务是否暴露   |
| subscribe              | boolean | true    | 在该注册中心上服务是否引用   |
| default                | boolean |         | 是否缺省的配置               |

## 服务端配置列表
\<motan:service/>

\<motan:basicService/>

> protocol、basic service、extConfig、service中定义相同属性时，优先级为service > extConfig > basic service > protocol

| Property name  | Type    | Default     | Comment                                                                                                      |
|----------------|---------|-------------|--------------------------------------------------------------------------------------------------------------|
| export         | String  |             | 服务暴露的方式，包含协议及端口号，多个协议端口用"," 分隔                                                     |
| basicService   |         |             | 基本service配置                                                                                              |
| interface      | Class   |             | 服务接口名                                                                                                   |
| ref            | String  |             | 接口实现的类                                                                                                 |
| class          | String  |             | 实现service的类名                                                                                            |
| host           | String  |             | 如果有多个ip，但只想暴露指定的某个ip，设置该参数                                                             |
| path           | String  |             | 服务路径                                                                                                     |
| serialization  | String  | hessian2    | 序列化方式                                                                                                   |
| extConfig      | String  |             | 扩展配置                                                                                                     |
| proxy          | String  |             | 代理类型                                                                                                     |
| group          | String  | default_rpc | 服务分组                                                                                                     |
| version        | String  | 1.0         | 版本                                                                                                         |
| throwException | String  | true        | 抛出异常                                                                                                     |
| requestTimeout | String  | 200         | (目前未用)请求超时时间(毫秒)                                                                                 |
| connectTimeout | String  | 1000        | (目前未用)连接超时时间(毫秒)                                                                                 |
| retries        | int     | 0           | (目前未用)重试次数                                                                                           |
| filter         | String  |             | 过滤器配置                                                                                                   |
| listener       | String  |             | 监听器配置                                                                                                   |
| connections    | int     |             | 连接数限制，0表示共享连接，否则为该服务独享连接数；默认共享                                                  |
| application    | String  | motan       | 应用信息                                                                                                     |
| module         | String  | motan       | 模块信息                                                                                                     |
| shareChannel   | boolean | false       | 是否共享channel                                                                                              |
| timeout        | int     |             | 方法调用超时时间                                                                                             |
| actives        | int     | 0           | 最大请求数，0为不做并发限制                                                                                  |
| async          | boolean | false       | 方法是否异步                                                                                                 |
| mock           | String  | false       | 设为true，表示使用缺省Mock类名，即：接口名+Mock 后缀，服务接口调用失败Mock实现类                             |
| check          | boolean | true        | 检查服务提供者是否存在                                                                                       |
| registry       | String  |             | 注册中心的id 列表，多个用“,”分隔，如果为空，则使用所有的配置中心                                             |
| register       | boolean | true        | 在该注册中心上服务是否暴露                                                                                   |
| subscribe      | boolean | true        | 在该注册中心上服务是否引用                                                                                   |
| accessLog      | String  | false       | 设为true，将向logger 中输出访问日志                                                                          |
| usegz          | boolean | false       | 是否开启gzip压缩.只有compressMotan的codec才能支持                                                            |
| mingzSize      | int     | 1000        | 开启gzip压缩的阈值.usegz开关开启，且传输数据大于此阈值时，才会进行gzip压缩。只有compressMotan的codec才能支持 |
| codec          | String  | motan       | 协议编码                                                                                                     |

## client配置列表

\<motan:referer/>

\<motan:basicReferer/>

> protocol、basic referer、extConfig、referer中定义相同属性时，优先级为referer > extConfig > basic referer > protocol

| Property name  | Type    | Default     | Comment                                                                                                      |
|----------------|---------|-------------|--------------------------------------------------------------------------------------------------------------|
| id             | String  |             | 服务引用 BeanId                                                                                              |
| protocol       | String  | motan       | 使用的协议                                                                                                   |
| interface      | Class   |             | 服务接口名                                                                                                   |
| client         | String  |             | 客户端类型                                                                                                   |
| directUrl      | String  |             | 点对点直连服务提供地址                                                                                       |
| basicReferer   | String  |             | 基本 referer 配置                                                                                            |
| extConfig      | String  |             | 扩展配置                                                                                                     |
| proxy          | String  |             | 代理类型                                                                                                     |
| group          | String  | default_rpc | 服务分组                                                                                                     |
| version        | String  | 1.0         | 版本                                                                                                         |
| throwException | String  | true        | 抛出异常                                                                                                     |
| requestTimeout | String  | 200         | 请求超时时间(毫秒)                                                                                           |
| connectTimeout | String  | 1000        | 连接超时时间(毫秒)                                                                                           |
| retries        | int     | 0           | 重试次数                                                                                                     |
| filter         | String  |             | 过滤器配置                                                                                                   |
| listener       | String  |             | 监听器配置                                                                                                   |
| connections    | int     |             | 连接数限制，0表示共享连接，否则为该服务独享连接数；默认共享                                                  |
| application    | String  | motan       | 应用信息                                                                                                     |
| module         | String  | motan       | 模块信息                                                                                                     |
| shareChannel   | boolean | false       | 是否共享channel                                                                                              |
| timeout        | int     |             | (目前未用)方法调用超时时间                                                                                   |
| actives        | int     | 0           | 最大请求数，0为不做并发限制                                                                                  |
| async          | boolean | false       | 方法是否异步                                                                                                 |
| mock           | String  | false       | 设为true，表示使用缺省Mock类名，即：接口名+Mock 后缀，服务接口调用失败Mock实现类                             |
| check          | boolean | true        | 检查服务提供者是否存在                                                                                       |
| registry       | String  |             | 注册中心的id 列表，多个用“,”分隔，如果为空，则使用所有的配置中心                                             |
| register       | boolean | true        | 在该注册中心上服务是否暴露                                                                                   |
| subscribe      | boolean | true        | 在该注册中心上服务是否引用                                                                                   |
| accessLog      | String  | false       | 设为true，将向logger 中输出访问日志                                                                          |
| usegz          | boolean | false       | 是否开启gzip压缩.只有compressMotan的codec才能支持                                                            |
| mingzSize      | int     | 1000        | 开启gzip压缩的阈值.usegz开关开启，且传输数据大于此阈值时，才会进行gzip压缩。只有compressMotan的codec才能支持 |
| codec          | String  | motan       | 协议编码                                                                                                     |


\<motan:method/>  
_需要定义在motan:referer内，用于控制某个函数的行为_

| Property name    | Type      | Default       | Comment                                                                                                        |
| ---------------- | --------- | ------------- | -------------------------------------------------------------------------------------------------------------- |
| name             | String    |               | 函数名                                                                                                         |
| argumentTypes    | String    |               | 参数类型（逗号分隔）, 无参数用void. 如果方法无重载，则可不写                                                   |
| requestTimeout   | int       | 200           | 请求超时时间(毫秒)                                                                                             |
| connectTimeout   | int       | 1000          | 连接超时时间(毫秒)                                                                                             |
