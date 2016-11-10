# 如何向Motan贡献代码
## 概述
Motan遵循Apache 2.0开源协议，我们欢迎所有人向Motan项目贡献代码，为开源社区贡献力量。

## 流程

1. （可选）提交Issue，说明你想要增加或修改的功能，我们会与你进行讨论修改的可行性并给出开发的建议。
2. （可选）你也可以认领Issue列表中已有的问题，在Issue中留言告知我们。
3. 开始开发，Motan的开发流程基于Github flow，请参考[Understanding the GitHub Flow](https://guides.github.com/introduction/flow/)
    1. fork Motan仓库(详细步骤请参考[Working with forks](https://help.github.com/articles/working-with-forks/))
    2. 在你的仓库中创建新的分支并提交代码。
    3. 创建pull request
    4. 我们会review你的代码并给出修改的建议。
    5. 代码符合要求后，我们会将代码合并到主分支。
4. 我们会把你的名字更新到Motan的贡献者列表，感谢你对Motan的贡献！

## 要求

1. 所有Motan源文件头需要包含Apache 2.0协议：

    ```
/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
```
2. 代码注释及提交注释使用英文，并使用清晰的描述语句，提交Pull Request前请使用[git rebase](Using Git rebase)功能完善提交记录。
3. 代码格式请参考[Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)或已有的源文件，保持风格一致。
4. 开发过程中如果遇到问题可以随时通过Issue与我们联系。

## 如何更新Motan Wiki
由于github wiki不提供pull request机制，我们在motan项目中放置了[wiki目录](https://github.com/weibocom/motan/tree/master/docs/wiki)，你可以提交pull request来修改这些文件，我们会将更新合并到wiki页面中。

# 扩展机制
## 概述
Motan框架基于扩展机制开发，增加新功能只需按照扩展机制实现对应的接口，甚至可以在不改造Motan代码本身的基础上增加新的功能。

## 扩展点
* Filter 发送/接收请求过程中增加切面逻辑，默认提供日志统计等功能
* HAStrategy 扩展可用性策略，默认提供快速失败等策略
* LoadBalance  扩展负载均衡策略，默认提供轮询等策略
* Serialization 扩展序列化方式，默认使用Hession序列化
* Protocol 扩展通讯协议，默认使用Motan自定义协议
* Registry 扩展服务发现机制，默认支持Zookeeper、Consul等服务发现机制
* Transport 扩展通讯框架，默认使用Netty框架

## 编写一个Motan扩展

1. 实现SPI扩展点接口
2. 实现类增加注解

    ```
@Spi(scope = Scope.SINGLETON)  //扩展加载形式，单例或多例
@SpiMeta(name = "motan")  //name表示扩展点的名称，根据name加载对应扩展
@Activation(sequence = 100) //同类型扩展生效顺序，部分扩展点支持。非必填
```

3. 增加SPI实现声明 
${classpath}/MATA-INF/services/${SPI interface fullname}文件中添加对应SPI接口实现类全名。
可参照motan-core模块/MATA-INF/services/下的配置

## 示例插件
```
//TODO
```