如果在使用中遇到问题，欢迎[提交Issue](https://github.com/weibocom/motan/issues)与我们交流。

#### Motan是否能够支撑高并发、大规模集群场景？
可以，Motan作为微博的核心基础组件，已经支撑了微博底层平台数千台机器的远程调用需求。性能测试的结果请参考[这里](zh_userguide#性能测试)。

#### Motan支持异步调用吗？如何实现？
Motan的请求在传输层面都是异步调用的，不需要额外配置。

#### 开源版Motan与微博内部的版本功能一样吗？
开源版Motan包含了内部版本中的大部分功能，主要是去除了一些内部的依赖组件相关的功能。

#### Motan支持php调用吗？
目前Motan支持php的YAR协议调用，见motan-extension下的motan-protocol-yar模块。

#### 我在使用Motan的过程中发现日志里有错误，如何解决？
请参考 [错误码及异常日志说明](zh_errorcode)，或者[提交Issue](https://github.com/weibocom/motan/issues)与我们交流。