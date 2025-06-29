# Change Log

## [1.2.5](https://github.com/weibocom/motan/tree/1.2.5) (2025-06-11)

[Full Changelog](https://github.com/weibocom/motan/compare/1.2.4...1.2.5)

**Implemented enhancements:**

- Supports force specifying motan2 codec (no longer compatible with motan1
  protocol) [\#1074](https://github.com/weibocom/motan/pull/1074) ([Ray](https://github.com/rayzhang0603))
- Implement sandbox and backup group
  capabilities [\#1077](https://github.com/weibocom/motan/pull/1077) ([Ray](https://github.com/rayzhang0603))

**Merged pull requests:**

**Fixed bugs:**

## [1.2.4](https://github.com/weibocom/motan/tree/1.2.4) (2024-11-08)

[Full Changelog](https://github.com/weibocom/motan/compare/1.2.3...1.2.4)

**Implemented enhancements:**

- implement dynamic
  filter [\#1066](https://github.com/weibocom/motan/pull/1066) ([Ray](https://github.com/rayzhang0603))
- optimize switch usage [\#1070](https://github.com/weibocom/motan/pull/1070) ([Ray](https://github.com/rayzhang0603))
- optimize management
  port [\#1071](https://github.com/weibocom/motan/pull/1071) ([Ray](https://github.com/rayzhang0603))
- optimize obtaining local
  IP [\#1072](https://github.com/weibocom/motan/pull/1072) ([Ray](https://github.com/rayzhang0603))

**Merged pull requests:**

**Fixed bugs:**

## [1.2.3](https://github.com/weibocom/motan/tree/1.2.3) (2024-05-30)

[Full Changelog](https://github.com/weibocom/motan/compare/1.2.2...1.2.3)

**Implemented enhancements:**

- accelerate netty client fusing when getting channel
  fails [\#1047](https://github.com/weibocom/motan/pull/1047) ([Ray](https://github.com/rayzhang0603))
- support server-side asynchronous
  implementation [\#1052](https://github.com/weibocom/motan/pull/1052) ([Ray](https://github.com/rayzhang0603))
- supports dynamic management capabilities through the admin
  port [\#1054](https://github.com/weibocom/motan/pull/1054) ([Ray](https://github.com/rayzhang0603))
- support fault injection [\#1055](https://github.com/weibocom/motan/pull/1055) ([Ray](https://github.com/rayzhang0603))
- force close endpoint when provider not
  exist [\#1058](https://github.com/weibocom/motan/pull/1058) ([Ray](https://github.com/rayzhang0603))
- support querying runtime
  info [\#1059](https://github.com/weibocom/motan/pull/1059) ([Ray](https://github.com/rayzhang0603))
- supports appending group suffix through env when
  registering [\#1060](https://github.com/weibocom/motan/pull/1060) ([Ray](https://github.com/rayzhang0603))
- meta information transfer mechanism; implement dynamic weight roundrobin load
  balancing [\#1061](https://github.com/weibocom/motan/pull/1061) ([Ray](https://github.com/rayzhang0603))

**Merged pull requests:**

**Fixed bugs:**

- fix synce server response attachment set
  failed [\#1056](https://github.com/weibocom/motan/pull/1056) ([Ray](https://github.com/rayzhang0603))

## [1.2.2](https://github.com/weibocom/motan/tree/1.2.2) (2023-08-21)

[Full Changelog](https://github.com/weibocom/motan/compare/1.2.1...1.2.2)

**Implemented enhancements:**

- execute the onfinish method when sending response
  fails [\#1028](https://github.com/weibocom/motan/pull/1028) ([Ray](https://github.com/rayzhang0603))
- access log add proxy ip [\#1029](https://github.com/weibocom/motan/pull/1029) ([Ray](https://github.com/rayzhang0603))

**Merged pull requests:**

**Fixed bugs:**

- fix RCE of
  Hessian2Serialization [\#1038](https://github.com/weibocom/motan/pull/1038) ([Ray](https://github.com/rayzhang0603))
- fix RCE of
  ProtobufSerialization [\#1040](https://github.com/weibocom/motan/pull/1040) ([Ray](https://github.com/rayzhang0603))

## [1.2.1](https://github.com/weibocom/motan/tree/1.2.1) (2023-03-07)

[Full Changelog](https://github.com/weibocom/motan/compare/1.2.0...1.2.1)

**Implemented enhancements:**

- add global configs load from
  properties [\#1017](https://github.com/weibocom/motan/pull/1017) ([Ray](https://github.com/rayzhang0603))
- mesh proxy support motan1
  protocol [\#1018](https://github.com/weibocom/motan/pull/1018) ([Ray](https://github.com/rayzhang0603))
- support lazy init in
  motan-transport-netty4 [\#1022](https://github.com/weibocom/motan/pull/1022) ([Ray](https://github.com/rayzhang0603))
- support calling with
  MeshClient [\#1024](https://github.com/weibocom/motan/pull/1024) ([Ray](https://github.com/rayzhang0603))

**Merged pull requests:**

**Fixed bugs:**

## [1.2.0](https://github.com/weibocom/motan/tree/1.2.0) (2022-10-19)

[Full Changelog](https://github.com/weibocom/motan/compare/1.1.12...1.2.0)

**Implemented enhancements:**

- update dependencies to fix security vulnerabilities. such as spring, netty, log4j
- RefererConfigBean getObjectType return default class when interface class is not set

**Merged pull requests:**

- support extended
  annotation [\#993](https://github.com/weibocom/motan/pull/993) ([dragon-zhang](https://github.com/dragon-zhang))

**Fixed bugs:**

## [1.1.12](https://github.com/weibocom/motan/tree/1.1.12) (2022-08-11)

[Full Changelog](https://github.com/weibocom/motan/compare/1.1.11...1.1.12)

**Implemented enhancements:**

**Merged pull requests:**

**Fixed bugs:**

- fix access log NPE when client request
  timeout [\#991](https://github.com/weibocom/motan/pull/991) ([Ray](https://github.com/rayzhang0603))

## [1.1.11](https://github.com/weibocom/motan/tree/1.1.11) (2022-08-03)

[Full Changelog](https://github.com/weibocom/motan/compare/1.1.10...1.1.11)

**Implemented enhancements:**

- support additional group from env; add configuration items(fusingThreshold and
  connectTimeout)  [\#990](https://github.com/weibocom/motan/pull/990) ([Ray](https://github.com/rayzhang0603))
- print access log according to the attachment settings;add message length, segment time and whole time in access
  log [\#988](https://github.com/weibocom/motan/pull/988) ([Ray](https://github.com/rayzhang0603))
- support mesh proxy by env
  setting [\#987](https://github.com/weibocom/motan/pull/987) ([Ray](https://github.com/rayzhang0603))
- support regist multi group in server
  end [\#985](https://github.com/weibocom/motan/pull/985) ([Ray](https://github.com/rayzhang0603))
- support disable default filter by '-'
  prefix [\#984](https://github.com/weibocom/motan/pull/984) ([Ray](https://github.com/rayzhang0603))
- try find provider by path if servicekey (group+path+version) not
  found  [\#984](https://github.com/weibocom/motan/pull/984) ([Ray](https://github.com/rayzhang0603))
- change asyncInitConnection default value from false to
  true [\#978](https://github.com/weibocom/motan/pull/978) ([Ray](https://github.com/rayzhang0603))

**Merged pull requests:**

**Fixed bugs:**

- export multi service with random
  port [\#985](https://github.com/weibocom/motan/pull/985) ([Ray](https://github.com/rayzhang0603))

## [1.1.10](https://github.com/weibocom/motan/tree/1.1.10) (2021-12-22)

[Full Changelog](https://github.com/weibocom/motan/compare/1.1.9...1.1.10)

**Implemented enhancements:**

- add cluster stat; throw exception as default when not found
  extension [\#957](https://github.com/weibocom/motan/pull/957) ([Ray](https://github.com/rayzhang0603))
- support mix group in config and
  command [\#953](https://github.com/weibocom/motan/pull/953) ([Ray](https://github.com/rayzhang0603))
- add dynamic switcher for
  accessLogFilter [\#949](https://github.com/weibocom/motan/pull/949) ([Ray](https://github.com/rayzhang0603))
- support weibo mesh as
  registry [\#947](https://github.com/weibocom/motan/pull/947) ([Ray](https://github.com/rayzhang0603))

**Merged pull requests:**

- update update java
  doc [\#954](https://github.com/weibocom/motan/pull/954) ([Forest](https://github.com/forestyoung23))
- fix typo [\#950](https://github.com/weibocom/motan/pull/950) ([Forest](https://github.com/forestyoung23))
- update dependency version to fix security vulnerability

**Fixed bugs:**

- change dir of generate-code to
  StandardLocation.SOURCE_OUTPUT [\#959](https://github.com/weibocom/motan/pull/959) ([Ray](https://github.com/rayzhang0603))
- fix netty channel
  deadlock [\#941](https://github.com/weibocom/motan/pull/941) ([Ray](https://github.com/rayzhang0603))

## [1.1.9](https://github.com/weibocom/motan/tree/1.1.9) (2021-02-02)

[Full Changelog](https://github.com/weibocom/motan/compare/1.1.8...1.1.9)

**Merged pull requests:**

- shuffle when loadbalance
  onrefresh [\#933](https://github.com/weibocom/motan/pull/933) ([Ray](https://github.com/rayzhang0603))
- update breeze version to
  0.1.4 [\#927](https://github.com/weibocom/motan/pull/927) ([Ray](https://github.com/rayzhang0603))
- support setting export ip from
  env [\#914](https://github.com/weibocom/motan/pull/914) ([Ray](https://github.com/rayzhang0603))
- support export at random
  port [\#912](https://github.com/weibocom/motan/pull/912) ([Ray](https://github.com/rayzhang0603))
- provider protected strategy support
  expansion [\#905](https://github.com/weibocom/motan/pull/905) ([X-L-Chen](https://github.com/X-L-Chen))
- add serverIp in exception information when decode
  error [\#904](https://github.com/weibocom/motan/pull/904) ([X-L-Chen](https://github.com/X-L-Chen))
- process in local when method is
  hashCode [\#903](https://github.com/weibocom/motan/pull/903) ([X-L-Chen](https://github.com/X-L-Chen))
- change explicit exception such as timeout exception to
  stackless [\#900](https://github.com/weibocom/motan/pull/900) ([X-L-Chen](https://github.com/X-L-Chen))

**Fixed bugs:**

- fix StandardThreadExecutor reject
  processing [\#934](https://github.com/weibocom/motan/pull/934) ([Ray](https://github.com/rayzhang0603))
- fix nettymessage cast [\#918](https://github.com/weibocom/motan/pull/918) ([Ray](https://github.com/rayzhang0603))

## [1.1.8](https://github.com/weibocom/motan/tree/1.1.8) (2020-03-26)

[Full Changelog](https://github.com/weibocom/motan/compare/1.1.7...1.1.8)

**Merged pull requests:**

- Bump tomcat-embed-core from 7.0.91 to
  7.0.99 [\#881](https://github.com/weibocom/motan/pull/881) ([dependabot](https://github.com/dependabot))
- add reject profile
  detail [\#883](https://github.com/weibocom/motan/pull/883) ([sunnights](https://github.com/sunnights))
- limit client total
  connections [\#885](https://github.com/weibocom/motan/pull/885) ([Wshoway](https://github.com/Wshoway))
- improve README:fix typo and improve
  format [\#890](https://github.com/weibocom/motan/pull/890) ([oldratlee](https://github.com/oldratlee))

**Fixed bugs:**

- fix multi consul registry [\#885](https://github.com/weibocom/motan/pull/885) ([Ray](https://github.com/rayzhang0603))

## [1.1.7](https://github.com/weibocom/motan/tree/1.1.7) (2019-12-05)

[Full Changelog](https://github.com/weibocom/motan/compare/1.1.6...1.1.7)

**Implemented enhancements:**

- support breeze
  serialization [\#822](https://github.com/weibocom/motan/pull/822) ([Ray](https://github.com/rayzhang0603))
- update yar-java [\#872](https://github.com/weibocom/motan/pull/872) ([Ray](https://github.com/rayzhang0603))
- remove BeanPostProcessor in
  ServiceConfigBean [\#875](https://github.com/weibocom/motan/pull/875) ([Wshoway](https://github.com/Wshoway))

**Fixed bugs:**

- remove request exception stacktrace in
  transport-netty4 [\#836](https://github.com/weibocom/motan/pull/836) ([sunnights](https://github.com/sunnights))

## [1.1.6](https://github.com/weibocom/motan/tree/1.1.6) (2019-07-04)

[Full Changelog](https://github.com/weibocom/motan/compare/1.1.5...1.1.6)

**Implemented enhancements:**

- add trace tag in
  request&response [\#836](https://github.com/weibocom/motan/pull/836) ([sunnights](https://github.com/sunnights))
- support custom slow
  threshold [\#836](https://github.com/weibocom/motan/pull/836) ([sunnights](https://github.com/sunnights))

**Fixed bugs:**

- remove request exception stacktrace in
  transport-netty4 [\#836](https://github.com/weibocom/motan/pull/836) ([sunnights](https://github.com/sunnights))

## [1.1.5](https://github.com/weibocom/motan/tree/1.1.5) (2019-05-16)

[Full Changelog](https://github.com/weibocom/motan/compare/1.1.4...1.1.5)

**Implemented enhancements:**

- decouple attachments for request &
  response [\#827](https://github.com/weibocom/motan/pull/827) ([sunnights](https://github.com/sunnights))
- optimize getChannel in
  SharedPoolClient [\#827](https://github.com/weibocom/motan/pull/827) ([sunnights](https://github.com/sunnights))

**Fixed bugs:**

- fix #829, available check in
  transport-netty [\#827](https://github.com/weibocom/motan/pull/827) ([sunnights](https://github.com/sunnights))

## [1.1.4](https://github.com/weibocom/motan/tree/1.1.4) (2018-12-18)

[Full Changelog](https://github.com/weibocom/motan/compare/1.1.3...1.1.4)

**Implemented enhancements:**

- add Content-Length header & fix
  #812 [\#816](https://github.com/weibocom/motan/pull/816) ([sunnights](https://github.com/sunnights))

**Fixed bugs:**

- fix dependency conflict
  issue [\#802](https://github.com/weibocom/motan/pull/802) ([HelloCoCooo](https://github.com/HelloCoCooo))
- fix dependency conflict
  issue [\#815](https://github.com/weibocom/motan/pull/815) ([HelloCoCooo](https://github.com/HelloCoCooo))

## [1.1.3](https://github.com/weibocom/motan/tree/1.1.3) (2018-12-24)

[Full Changelog](https://github.com/weibocom/motan/compare/1.1.2...1.1.3)

**Fixed bugs:**

- fix: compatible with zookeeper string
  serializer [\#707](https://github.com/weibocom/motan/pull/781) ([sunnights](https://github.com/sunnights))

## [1.1.2](https://github.com/weibocom/motan/tree/1.1.2) (2018-12-18)

[Full Changelog](https://github.com/weibocom/motan/compare/1.1.1...1.1.2)

**Implemented enhancements:**

- improvement: nettyclient scheduledExecutor
  optimization [\#709](https://github.com/weibocom/motan/pull/709) ([sunnights](https://github.com/sunnights))
- improvement: getChannelKey when
  necessary [\#711](https://github.com/weibocom/motan/pull/711) ([pifuant](https://github.com/pifuant))
- improvement: LocalSwitcherService registerListener and unRegisterListener
  optimization [\#713](https://github.com/weibocom/motan/pull/713) ([pifuant](https://github.com/pifuant))
- optimize exception stack and log
  level [\#730](https://github.com/weibocom/motan/pull/730) ([sunnights](https://github.com/sunnights))
- optimize zookeeper serialization
  method [\#732](https://github.com/weibocom/motan/pull/732) ([Zha-Zha](https://github.com/Zha-Zha))
- update dependency for potential security
  vulnerability [\#762](https://github.com/weibocom/motan/pull/762) ([rayzhang0603](https://github.com/rayzhang0603))
- add traceable request and statistic
  message [\#740](https://github.com/weibocom/motan/pull/740) ([sunnights](https://github.com/sunnights))

**Fixed bugs:**

- netty4 add channel
  manage [\#707](https://github.com/weibocom/motan/pull/707) ([sunnights](https://github.com/sunnights))
- fix consul read
  timeout [\#746](https://github.com/weibocom/motan/pull/746) ([sunnights](https://github.com/sunnights))
- fix get channel error [\#776](https://github.com/weibocom/motan/pull/776) ([caorong](https://github.com/caorong))

## [1.1.1](https://github.com/weibocom/motan/tree/1.1.1) (2018-05-17)

[Full Changelog](https://github.com/weibocom/motan/compare/1.1.0...1.1.1)

**Implemented enhancements:**

- support multi serialize in
  simpleSerialization [\#635](https://github.com/weibocom/motan/pull/635) ([Ray](https://github.com/rayzhang0603))
- support more data type in
  simpleSerialization [\#683](https://github.com/weibocom/motan/pull/683) ([lion2luo](https://github.com/lion2luo))
- add rpc common
  client [\#682](https://github.com/weibocom/motan/pull/682) [\#702](https://github.com/weibocom/motan/pull/702) ([sunnights](https://github.com/sunnights))
- enable channel manage for netty4
  server [\#707](https://github.com/weibocom/motan/pull/707) ([sunnights](https://github.com/sunnights))

**Fixed bugs:**

- optimize netty4 server when reject
  request [\#613](https://github.com/weibocom/motan/pull/613) ([sunnights](https://github.com/sunnights))
- fix NPE when discover service
  failed [\#637](https://github.com/weibocom/motan/pull/637) ([sunnights](https://github.com/sunnights))
- fix netty channel close issue in
  netty4 [\#693](https://github.com/weibocom/motan/pull/693) ([sunnights](https://github.com/sunnights))
- fix connection leak when netty client create connection with
  exception [\#670](https://github.com/weibocom/motan/pull/670) ([lion2luo](https://github.com/lion2luo))

## [1.1.0](https://github.com/weibocom/motan/tree/1.1.0) (2017-10-31)

[Full Changelog](https://github.com/weibocom/motan/compare/1.0.0...1.1.0)

**Implemented enhancements:**

- add netty4 support [\#595](https://github.com/weibocom/motan/pull/595) ([sunnights](https://github.com/sunnights))

**Fixed bugs:**

- fix MotanAsyncProcessor warning info above
  jdk7 [\#602](https://github.com/weibocom/motan/pull/602) ([Panying](https://github.com/anylain))
- fix async return null when send exception
  happened [\#605](https://github.com/weibocom/motan/pull/605) ([Ray](https://github.com/rayzhang0603))
- fix motan2 decode fail when object is
  null [\#591](https://github.com/weibocom/motan/pull/591) ([Ray](https://github.com/rayzhang0603))

**Merged pull requests:**

- update consul
  version [\#587](https://github.com/weibocom/motan/pull/587) ([Michael Yang](https://github.com/yangfuhai))

## [1.0.0](https://github.com/weibocom/motan/tree/1.0.0) (2017-10-31)

[Full Changelog](https://github.com/weibocom/motan/compare/0.3.1...1.0.0)

**Implemented enhancements:**

- add motan2 protocol for cross-language
  transport [\#561](https://github.com/weibocom/motan/pull/561) ([Ray](https://github.com/rayzhang0603))
- add gRPC protocol support [\#561](https://github.com/weibocom/motan/pull/561) ([Ray](https://github.com/rayzhang0603))
- add simple serialization [\#561](https://github.com/weibocom/motan/pull/561) ([Ray](https://github.com/rayzhang0603))
- add RpcContext for pass custom
  params [\#561](https://github.com/weibocom/motan/pull/561) ([Ray](https://github.com/rayzhang0603))
- add InitializableFilter
  interface [\#561](https://github.com/weibocom/motan/pull/561) ([Ray](https://github.com/rayzhang0603))
- add transExceptionStack in motan config to avoid transport java exception
  stack [\#561](https://github.com/weibocom/motan/pull/561) ([Ray](https://github.com/rayzhang0603))

**Fixed bugs:**

**Merged pull requests:**

- refine DefaultProvider
  log [\#501](https://github.com/weibocom/motan/pull/501) ([yeluoguigen009](https://github.com/yeluoguigen009))

## [0.3.1](https://github.com/weibocom/motan/tree/0.3.1) (2017-07-11)

[Full Changelog](https://github.com/weibocom/motan/compare/0.3.0...0.3.1)

**Implemented enhancements:**

- add protobuf
  serialization [\#425](https://github.com/weibocom/motan/pull/425) ([东方上人](https://github.com/dongfangshangren))
- add restful protocol
  support [\#458](https://github.com/weibocom/motan/pull/458) ([东方上人](https://github.com/dongfangshangren))

**Fixed bugs:**

- fix basic service not
  enable [\#423](https://github.com/weibocom/motan/pull/423) ([Voyager3](https://github.com/xxxxzr))
- add ShutDownHookListener [\#443](https://github.com/weibocom/motan/pull/443) ([Voyager3](https://github.com/xxxxzr))
- fix zookeeper UT [\#334](https://github.com/weibocom/motan/pull/334) ([sunnights](https://github.com/sunnights))

**Merged pull requests:**

- polish ConsulEcwidClient [\#395](https://github.com/weibocom/motan/pull/395) ([Jin Zhang](https://github.com/lowzj))
- reduce duplication of getting
  referer [\#407](https://github.com/weibocom/motan/pull/407) ([brandy](https://github.com/xiaoqing-yuanfang))

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
- change xsd type to
  string  [\#326](https://github.com/weibocom/motan/pull/326) ([Ray](https://github.com/rayzhang0603))

**Fixed bugs:**

- add Ordered interface to
  AnnotationBean [\#322](https://github.com/weibocom/motan/pull/322) ([feilaoda](https://github.com/feilaoda))
- available after register while heartbeat switcher is
  open  [\#305](https://github.com/weibocom/motan/pull/305) ([Ray](https://github.com/rayzhang0603))

**Merged pull requests:**

## [0.2.2](https://github.com/weibocom/motan/tree/0.2.2) (2016-11-25)

[Full Changelog](https://github.com/weibocom/motan/compare/0.2.1...0.2.2)

**Implemented enhancements:**

- local method do not request
  server [\#286](https://github.com/weibocom/motan/pull/286) ([Ray](https://github.com/rayzhang0603))
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

- Added hprose serialization
  support [\#162](https://github.com/weibocom/motan/pull/162) ([小马哥](https://github.com/andot))

## [0.2.0](https://github.com/weibocom/motan/tree/0.2.0) (2016-08-05)

[Full Changelog](https://github.com/weibocom/motan/compare/0.1.2...0.2.0)

**Implemented enhancements:**

- support yar protocol [\#160](https://github.com/weibocom/motan/pull/160) ([Ray](https://github.com/rayzhang0603))

**Fixed bugs:**

- fix bug of LocalFirstLoadBalance referer select [\#155](https://github.com/weibocom/motan/issues/155)

**Merged pull requests:**

- add annotation for
  spring [\#101](https://github.com/weibocom/motan/pull/101) ([feilaoda](https://github.com/feilaoda))

## [0.1.2](https://github.com/weibocom/motan/tree/0.1.2) (2016-06-27)

[Full Changelog](https://github.com/weibocom/motan/compare/0.1.1...0.1.2)

**Implemented enhancements:**

- support command parse in consul
  registry [\#96](https://github.com/weibocom/motan/pull/96) ([sunnights](https://github.com/sunnights))
-
    - support command parse in zk
      registry [\#49](https://github.com/weibocom/motan/pull/49) ([sunnights](https://github.com/sunnights))
- Support direct registry [\#110](https://github.com/weibocom/motan/pull/110) ([qdaxb](https://github.com/qdaxb))

**Fixed bugs:**

- fix bug of lost server node when zookeeper session
  change [\#133](https://github.com/weibocom/motan/pull/133) ([Ray](https://github.com/rayzhang0603))
- fix bug of potential overflow of
  requestId [\#124](https://github.com/weibocom/motan/pull/124) ([Di Tang](https://github.com/tangdi))
- parsing multi directurl [\#78](https://github.com/weibocom/motan/pull/78) ([Ray](https://github.com/rayzhang0603))

**Merged pull requests:**

## [0.1.1](https://github.com/weibocom/motan/tree/0.1.1) (2016-05-14)

[Full Changelog](https://github.com/weibocom/motan/compare/0.1.0...0.1.1)

**Implemented enhancements:**

- manager moudle support
  consul [\#27](https://github.com/weibocom/motan/issues/27) ([sunnights](https://github.com/sunnights))
- Friendly error message for spi [\#58](https://github.com/weibocom/motan/pull/58) ([qdaxb](https://github.com/qdaxb))

**Fixed bugs:**

- fix bug of zookeeper connect
  timeout [\#60](https://github.com/weibocom/motan/pull/60) ([qdaxb](https://github.com/qdaxb))
- fix bug of localfirst
  loadbalance [\#46](https://github.com/weibocom/motan/pull/46) ([qdaxb](https://github.com/qdaxb))
- add loadProperties\(\) [\#30](https://github.com/weibocom/motan/pull/30) ([half-dead](https://github.com/half-dead))

**Merged pull requests:**

- Fixed typos [\#44](https://github.com/weibocom/motan/pull/44) ([radarhere](https://github.com/radarhere))
- Refactor manager registryservice, support query consul
  service [\#33](https://github.com/weibocom/motan/pull/33) ([sunnights](https://github.com/sunnights))

## [0.1.0](https://github.com/weibocom/motan/tree/0.1.0) (2016-04-29)

[Full Changelog](https://github.com/weibocom/motan/compare/0.0.1...0.1.0)

**Implemented enhancements:**

- gracefully shutdown support for zookeeper [\#5](https://github.com/weibocom/motan/issues/5)
- make zookeeper registry
  configurable [\#23](https://github.com/weibocom/motan/pull/23) ([qdaxb](https://github.com/qdaxb))
- refactor registry for support setting service
  available [\#11](https://github.com/weibocom/motan/pull/11) ([qdaxb](https://github.com/qdaxb))

**Fixed bugs:**

- replace cache map with thread-safe implemention. Fixes
  \#6 [\#7](https://github.com/weibocom/motan/pull/7) ([qdaxb](https://github.com/qdaxb))

**Merged pull requests:**

- update pom dependency [\#18](https://github.com/weibocom/motan/pull/18) ([qdaxb](https://github.com/qdaxb))
- Zookeeper graceful
  shutdown [\#17](https://github.com/weibocom/motan/pull/17) ([sunnights](https://github.com/sunnights))
- Update quickstart [\#12](https://github.com/weibocom/motan/pull/12) ([fingki](https://github.com/fingki))
- Update pom.xml [\#4](https://github.com/weibocom/motan/pull/4) ([sumory](https://github.com/sumory))
- English Quickstart [\#2](https://github.com/weibocom/motan/pull/2) ([wenqisun](https://github.com/wenqisun))

\* *This Change Log was automatically generated
by [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator)*
