#!/bin/bash
DIR=`pwd`
cd `dirname $0`
cd ../lib
LIB_DIR=`pwd`

SERVER_NAME='com.weibo.motan.benchmark.MotanBenchmarkClient'

PIDS=`ps -ef | grep java | grep "$LIB_DIR" | grep ${SERVER_NAME} | awk '{print $2}'`
if [ -n "$PIDS" ]; then
    echo "start failed, the $SERVER_NAME already started!"
    exit 1
fi

LIB_JARS=`ls ${LIB_DIR} | grep .jar | awk '{print "'${LIB_DIR}'/"$0}' | tr "\n" ":"`
cd ..
nohup java -classpath ${LIB_JARS} ${SERVER_NAME} nohup.client.out 2>&1 &

echo "start "${SERVER_NAME}" success!"
cd ${DIR}