/*
 * Copyright 2009-2016 Weibo, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.api.motan.protocol.grpc;

import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultResponseFuture;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;
import io.grpc.ClientCall;
import io.grpc.stub.StreamObserver;

/**
 * 
 * @Description GrpcResponseFuture
 * @author zhanglei
 * @date Oct 13, 2016
 *
 * @param <RespT>
 */
public class GrpcResponseFuture<RespT> extends DefaultResponseFuture implements StreamObserver<RespT> {
    private final ClientCall<?, RespT> call;

    public GrpcResponseFuture(Request requestObj, int timeout, URL serverUrl, ClientCall<?, RespT> call) {
        super(requestObj, timeout, serverUrl);
        this.call = call;
    }

    public boolean cancel() {
        call.cancel("GrpcResponseFuture was cancelled", null);
        return super.cancel();
    }

    private long calculateProcessTime() {
        return System.currentTimeMillis() - createTime;
    }

    @Override
    public void onNext(Object value) {
        this.result = value;
    }

    @Override
    public void onError(Throwable t) {
        if (t instanceof Exception) {
            this.exception = (Exception) t;
        } else {
            this.exception = new MotanServiceException("grpc response future has fatal error.", t);
        }
        done();
    }

    @Override
    public void onCompleted() {
        this.processTime = calculateProcessTime();
        done();
    }


}
