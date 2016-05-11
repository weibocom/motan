### demo 运行说明(Deprecated, 请在IDE中调试, 参加issue #41 #45)
###1、在motan-demo项目下执行 
	mvn clean package

###2、运行rpc server。

	cd motan-demo-server/target
	
	tar -zxvf motan-demo-server-1.1.48-SNAPSHOT-assembly.tar.gz

	cd motan-demo-server-1.1.48-SNAPSHOT

	bash bin/start.sh

###3、运行rpc client

	cd motan-demo-server/target

	tar -zxvf motan-demo-client-1.1.48-SNAPSHOT-assembly.tar.gz

	cd motan-demo-client-1.1.48-SNAPSHOT

	bash bin/start.sh

###4、停止rpc

分别执行client、server的bin/stop.sh命令即可。