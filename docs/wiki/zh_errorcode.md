
## MotanBizException（业务异常）

业务方抛出的异常，比如用户不存在等，motan直接将原异常抛出，不重试，不catch。

 状态码 | 错误码 | 异常日志 | 处理建议
 --- | --- | --- | ---
503 | 30001 | 业务方抛出的异常信息 | Motan框架不对异常进行catch以及重试，直接向客户端抛出，由客户端自行处理，出现此类异常需要对业务逻辑进行排查

## MotanServiceException（服务异常）

处理请求服务时的异常，如timeout、没有可用service等异常。

 状态码 | 错误码 | 异常日志 | 处理建议
 --- | --- | --- | ---
503 | 10001 | FailoverHaStrategy No referers for request:%s, loadbalance:%s | 不存在可用服务，排查方向：1. 服务提供方服务提供是否正常，可以通过管理后台查看；2. 自定义loadbalance策略是否正确，是否存在漏洞将所有可用服务都过滤掉了
503 | 10001 | Unknow port in service:%s, protocol:%s | 暴露服务中未定义暴露端口，检查serviceconfig的配置
503 | 10001 | export should not empty in service config | service config中未定义暴露的协议与端口，检查serviceconfig的配置
503 | 10001 | Export is malformed | service config中定义export格式不正确，应该为protocol1:port1,protocol2:port2
503 | 10001 | NettyChannel failed to connect to server | 服务使用方初始化netty中，无法连接到服务端，确认服务提供方是否正确提供服务   
503 | 10001 | NettyChannel connect to server timeout | 服务使用方初始化netty中，连接服务端超时，确认服务提供方是否正确提供服务
503 | 10001 | NettyChannel send request to server Error | client向server发起请求出错，未完成请求。 
503 | 10001 | NettyChannel send request to server Timeout | client向server发起请求超时，未完成请求。
503 | 10001 | NettyChannel is not avaliable | 在消费方该nettychannel被标为不可用，检查是否存在连续大于maxClientConnection的次数失败
503 | 10001 | No available referers for call request | 在消费方所有referers被标为不可用，检查是否所有的请求都失败
503 | 10002 | Request(%s) active count exceed the limit (%s), referer:%s | 判断某个接口并发数是否超限，如果超过限制，则上抛异常,同时做简单的统计。
503 | 10002 | process thread pool is full, reject | 服务提供方出现处理线程池满了，检查是否请求量过大
503 | 10002 | NettyClient over of max concurrent request, drop request | 进行最大的请求并发数的控制，如果超过NETTY_CLIENT_MAX_REQUEST的话，那么throw reject exception
503 | 10003 | NettyResponseFuture request timeout | 请求超时，可以调整referer中的requestTimeout
403 | 10101 | service unfound | 找不到服务，查看服务是否正确暴露
403 | 10101 | InjvmReferer call Error: provider not exist | 使用injvm方式未暴露服务
403 | 10101 | ClusterSupport No service urls for the refer:%s, registries:%s | 从服务发现组件中不能获取服务列表，通过管理后台查看是否有正常工作的服务器。
403 | 10101 | Service method not exist: | 服务提供方没有服务消费方调用的方法。

## MotanFrameworkException（框架异常）

框架异常，比如系统启动、关闭、服务暴露、服务注册等非请求时抛的异常，

 状态码 | 错误码 | 异常日志 | 处理建议
 --- | --- | --- | ---
503 | 20001 | RefererConfig is malformed, for protocol not set correctly! | 服务暴露过程中未定义暴露协议，需要增加暴露协议，例如motan
503 | 20001 | %s false to registery/unregistery/subscribe/unsubscribe %s to %s | 服务注册失败/取消失败/订阅/取消订阅，当check为true抛出此异常，当check为false，转入后台线程进行重试，当失败，将会往warn日志中记录False when retry in failback registry。需要检查config server的配置是否正确，config server是否可用。
503 | 20001 | ExtensionLoader loadExtensionClasses error, prefix: | motan通过加载各个定义组件进行提供服务，当加载组件出现错误抛出此异常，需要检查扩展组件是否正确加载。
503 | 20001 | provider alread exist | 暴露的服务已经存在，区分服务的唯一性标志为group/interface/version
503 | 20001 | Class must be interface! | 暴露的服务必须为Interface
503 | 20001 | RequestRouter handler(channel, message) params is null | 服务处理事件缺少channel或者message，检查服务暴露是否正常
503 | 20001 | RequestRouter message type not support: | 服务接收到到非request时抛出的异常
503 | 20001 | XXX call Error: node is not available XXX | 某一个服务节点出现不可达状态。如果节点是可用状态，同时当前连续失败的次数超过限制maxClientConnection次，那么把该节点标示为不可用。此时需要确认是否该服务节点存在问题。
503 | 20001 | HeartbeatFactory not exist: XXX | 定义的心跳监测类不存在，此类异常通常出现在扩展心跳监测类过程中抛出，需要检查是否定义正确
503 | 20001 | NettyDecoder transport header not support, type: XX | 不支持codec的方式，需要确认通信的编码方式是否对应。
503 | 20002 | encode error: isResponse= | 使用编码协议进行encode失败，检查codec方式是否有问题。
503 | 20003 | decode error: format problem | 使用编码协议进行decode失败，检查codec方式是否有问题。
500 | 20004 | Error when append params for config: / class.configDesc should not be null or empty | 配置项中的required值未定义，导致初始化失败，例如ProtocolConfig中的Name属性
500 | 20004 | ServiceConfig must config right export value/has a registryConfig! | 检查ServiceConfig是否定义了export值以及protocol或者registry。
500 | 20004 | RefererConfig must has one protocolConfig/registryConfig! | 检查RefererConfig是否定义了protocol或者registry
500 | 20004 | "The interface XX has more than one method YY , must set argumentTypes attribute." | 暴露的方法定义与真实的暴露方法不一致，找不到定义的方法
500 | 20004 | configService is malformed, for same service (%s) already exists | 暴露的服务已经存在，需要检查暴露的服务是否存在重复定义，服务的唯一标志：group/interface/version
500 | 20004 | Please config local server hostname with intranet IP first! | 暴露服务的机器未配置本地hostname，导致服务暴露过程中无法找到暴露服务的ip
500 | 20004 | Create registry false for url: | 无法创建注册中心，需要查看registry的配置是否正确
500 | 20004 | node init Error:  | 创建服务节点失败，问题可能发生在于无法启动netty server或者client
500 | 20004 | ReferereConfig initRef Error: Class not found | client端无法找到使用的接口，检查是否引入了正确的依赖包
500 | 20004 | Service export Error: share channel but some config param is different, protocol or codec or serialize or maxContentLength or maxServerConnection or maxWorkerThread or heartbeatFactory, source= | 多个服务共享service channel(port) 对外提供服务，需要保持protocol、codec、serialize、maxContentLength、maxServerConnection、maxWorkerThread、heartbeatFactory一致
    
