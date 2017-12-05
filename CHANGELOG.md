# Change Log

## [1.1.0](https://github.com/weibocom/motan/tree/1.1.0) (2017-10-31)
[Full Changelog](https://github.com/weibocom/motan/compare/1.0.0...1.1.0)

**Implemented enhancements:**

- add netty4 support [\#595](https://github.com/weibocom/motan/pull/595) ([sunnights](https://github.com/sunnights))

**Fixed bugs:**

- fix MotanAsyncProcessor warning info above jdk7 [\#602](https://github.com/weibocom/motan/pull/602) ([Panying](https://github.com/anylain))
- fix async return null when send exception happened [\#605](https://github.com/weibocom/motan/pull/605) ([Ray](https://github.com/rayzhang0603))
- fix motan2 decode fail when object is null [\#591](https://github.com/weibocom/motan/pull/591) ([Ray](https://github.com/rayzhang0603))

**Merged pull requests:**

- update consul version [\#587](https://github.com/weibocom/motan/pull/587) ([Michael Yang](https://github.com/yangfuhai))



## [1.0.0](https://github.com/weibocom/motan/tree/1.0.0) (2017-10-31)
[Full Changelog](https://github.com/weibocom/motan/compare/0.3.1...1.0.0)

**Implemented enhancements:**

- add motan2 protocol for cross-language transport [\#561](https://github.com/weibocom/motan/pull/561) ([Ray](https://github.com/rayzhang0603))
- add gRPC protocol support [\#561](https://github.com/weibocom/motan/pull/561) ([Ray](https://github.com/rayzhang0603))
- add simple serialization [\#561](https://github.com/weibocom/motan/pull/561) ([Ray](https://github.com/rayzhang0603))
- add RpcContext for pass custom params [\#561](https://github.com/weibocom/motan/pull/561) ([Ray](https://github.com/rayzhang0603))
- add InitializableFilter interface [\#561](https://github.com/weibocom/motan/pull/561) ([Ray](https://github.com/rayzhang0603))
- add transExceptionStack in motan config to avoid transport java exception stack [\#561](https://github.com/weibocom/motan/pull/561) ([Ray](https://github.com/rayzhang0603))

**Fixed bugs:**

**Merged pull requests:**

- refine DefaultProvider log [\#501](https://github.com/weibocom/motan/pull/501) ([yeluoguigen009](https://github.com/yeluoguigen009)) 


## [0.3.1](https://github.com/weibocom/motan/tree/0.3.1) (2017-07-11)
[Full Changelog](https://github.com/weibocom/motan/compare/0.3.0...0.3.1)

**Implemented enhancements:**

- add protobuf serialization [\#425](https://github.com/weibocom/motan/pull/425) ([东方上人](https://github.com/dongfangshangren))
- add restful protocol support [\#458](https://github.com/weibocom/motan/pull/458) ([东方上人](https://github.com/dongfangshangren))

**Fixed bugs:**

- fix basic service not enable [\#423](https://github.com/weibocom/motan/pull/423) ([Voyager3](https://github.com/xxxxzr)) 
- add ShutDownHookListener [\#443](https://github.com/weibocom/motan/pull/443) ([Voyager3](https://github.com/xxxxzr))
- fix zookeeper UT [\#334](https://github.com/weibocom/motan/pull/334) ([sunnights](https://github.com/sunnights))

**Merged pull requests:**

- polish ConsulEcwidClient [\#395](https://github.com/weibocom/motan/pull/395) ([Jin Zhang](https://github.com/lowzj)) 
- reduce duplication of getting referer [\#407](https://github.com/weibocom/motan/pull/407) ([brandy](https://github.com/xiaoqing-yuanfang)) 

## [0.3.0](https://github.com/weibocom/motan/tree/0.3.0) (2017-03-09)
[Full Changelog](https://github.com/weibocom/motan/compare/0.2.3...0.3.0)

**Implemented enhancements:**

- async call [\#372](https://github.com/weibocom/motan/pull/372) ([Ray](https://github.com/rayzhang0603))

**Fixed bugs:**

**Merged pull requests:**


## [0.2.3](https://github.com/weibocom/motan/tree/0.2.3) (2017-02-16)
[Full Changelog](https://github.com/weibocom/motan/compare/0.2.2...0.2.3)

**Implemented enhancements:**

- OpenTracing supported [\#311](https://github.com/weibocom/motan/pull/311) ([Ray](https://github.com/rayzhang0603))
- change xsd type to string  [\#326](https://github.com/weibocom/motan/pull/326) ([Ray](https://github.com/rayzhang0603))

**Fixed bugs:**

- add Ordered interface to AnnotationBean [\#322](https://github.com/weibocom/motan/pull/322) ([feilaoda](https://github.com/feilaoda)) 
- available after register while heartbeat switcher is open  [\#305](https://github.com/weibocom/motan/pull/305) ([Ray](https://github.com/rayzhang0603))

**Merged pull requests:**


## [0.2.2](https://github.com/weibocom/motan/tree/0.2.2) (2016-11-25)
[Full Changelog](https://github.com/weibocom/motan/compare/0.2.1...0.2.2)

**Implemented enhancements:**

- local method do not request server [\#286](https://github.com/weibocom/motan/pull/286) ([Ray](https://github.com/rayzhang0603))
- use ThreadLocalRandom


**Fixed bugs:**

- loadbalance index overflow
- consul registry notify NPE

**Merged pull requests:**


## [0.2.1](https://github.com/weibocom/motan/tree/0.2.1) (2016-08-18)
[Full Changelog](https://github.com/weibocom/motan/compare/0.2.0...0.2.1)

**Implemented enhancements:**

- add Initialization SPI [\#171](https://github.com/weibocom/motan/pull/171) ([Ray](https://github.com/rayzhang0603))
- add abstract mockprotocol [\#171](https://github.com/weibocom/motan/pull/171) ([Ray](https://github.com/rayzhang0603))

**Fixed bugs:**


**Merged pull requests:**

- Added hprose serialization support [\#162](https://github.com/weibocom/motan/pull/162) ([小马哥](https://github.com/andot)) 

## [0.2.0](https://github.com/weibocom/motan/tree/0.2.0) (2016-08-05)
[Full Changelog](https://github.com/weibocom/motan/compare/0.1.2...0.2.0)

**Implemented enhancements:**

- support yar protocol [\#160](https://github.com/weibocom/motan/pull/160) ([Ray](https://github.com/rayzhang0603))

**Fixed bugs:**

- fix bug of LocalFirstLoadBalance referer select [\#155](https://github.com/weibocom/motan/issues/155) 

**Merged pull requests:**

- add annotation for spring [\#101](https://github.com/weibocom/motan/pull/101) ([feilaoda](https://github.com/feilaoda)) 

## [0.1.2](https://github.com/weibocom/motan/tree/0.1.2) (2016-06-27)
[Full Changelog](https://github.com/weibocom/motan/compare/0.1.1...0.1.2)

**Implemented enhancements:**

- support command parse in consul registry [\#96](https://github.com/weibocom/motan/pull/96) ([sunnights](https://github.com/sunnights))
- - support command parse in zk registry [\#49](https://github.com/weibocom/motan/pull/49) ([sunnights](https://github.com/sunnights))
- Support direct registry [\#110](https://github.com/weibocom/motan/pull/110) ([qdaxb](https://github.com/qdaxb))

**Fixed bugs:**

- fix bug of lost server node when zookeeper session change [\#133](https://github.com/weibocom/motan/pull/133) ([Ray](https://github.com/rayzhang0603))
- fix bug of potential overflow of requestId [\#124](https://github.com/weibocom/motan/pull/124) ([Di Tang](https://github.com/tangdi))
- parsing multi directurl [\#78](https://github.com/weibocom/motan/pull/78) ([Ray](https://github.com/rayzhang0603))

**Merged pull requests:**


## [0.1.1](https://github.com/weibocom/motan/tree/0.1.1) (2016-05-14)
[Full Changelog](https://github.com/weibocom/motan/compare/0.1.0...0.1.1)

**Implemented enhancements:**

- manager moudle support consul [\#27](https://github.com/weibocom/motan/issues/27) ([sunnights](https://github.com/sunnights))
- Friendly error message for spi [\#58](https://github.com/weibocom/motan/pull/58) ([qdaxb](https://github.com/qdaxb))

**Fixed bugs:**

- fix bug of zookeeper connect timeout [\#60](https://github.com/weibocom/motan/pull/60) ([qdaxb](https://github.com/qdaxb))
- fix bug of localfirst loadbalance [\#46](https://github.com/weibocom/motan/pull/46) ([qdaxb](https://github.com/qdaxb))
- add loadProperties\(\) [\#30](https://github.com/weibocom/motan/pull/30) ([half-dead](https://github.com/half-dead))

**Merged pull requests:**

- Fixed typos [\#44](https://github.com/weibocom/motan/pull/44) ([radarhere](https://github.com/radarhere))
- Refactor manager registryservice, support query consul service [\#33](https://github.com/weibocom/motan/pull/33) ([sunnights](https://github.com/sunnights))
 

## [0.1.0](https://github.com/weibocom/motan/tree/0.1.0) (2016-04-29)
[Full Changelog](https://github.com/weibocom/motan/compare/0.0.1...0.1.0)

**Implemented enhancements:**

- gracefully shutdown support for zookeeper [\#5](https://github.com/weibocom/motan/issues/5)
- make zookeeper registry configurable [\#23](https://github.com/weibocom/motan/pull/23) ([qdaxb](https://github.com/qdaxb))
- refactor registry for support setting service available [\#11](https://github.com/weibocom/motan/pull/11) ([qdaxb](https://github.com/qdaxb))

**Fixed bugs:**

- replace cache map with thread-safe implemention. Fixes \#6 [\#7](https://github.com/weibocom/motan/pull/7) ([qdaxb](https://github.com/qdaxb))

**Merged pull requests:**

- update pom dependency [\#18](https://github.com/weibocom/motan/pull/18) ([qdaxb](https://github.com/qdaxb))
- Zookeeper graceful shutdown [\#17](https://github.com/weibocom/motan/pull/17) ([sunnights](https://github.com/sunnights))
- Update quickstart [\#12](https://github.com/weibocom/motan/pull/12) ([fingki](https://github.com/fingki))
- Update pom.xml [\#4](https://github.com/weibocom/motan/pull/4) ([sumory](https://github.com/sumory))
- English Quickstart [\#2](https://github.com/weibocom/motan/pull/2) ([wenqisun](https://github.com/wenqisun))



\* *This Change Log was automatically generated by [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator)*
