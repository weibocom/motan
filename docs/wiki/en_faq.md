
If you encounter problems in use, please [submit Issue](https://github.com/weibocom/motan/issues) to communicate with us.

#### Is Motan able to support large-scale cluster?
Yes, as the core component of Weibo, Motan has been supporting a cluster of thousands of machines.

#### Does Motan support asynchronous calls? How to achieve?
Motan requests are asynchronous calls at the transport layer, and no additional configuration is required.

#### The open source version of Motan and Weibo's internal version are the same exactly?
The open source Motan contains most of the features in the internal version, mainly to remove some functions related to internal dependencies.

#### Does Motan support PHP calls?
Motan currently does not support the PHP call, we are working for that, and the PHP client has been tested in a small scope.

#### Motan support module extension?
Yes, you can extend Motan through SPI.