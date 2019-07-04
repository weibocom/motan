package com.weibo.api.motan.rpc;

import java.util.concurrent.Executor;

/**
 * @author sunnights
 */
public interface Callbackable {

    void addFinishCallback(Runnable runnable, Executor executor);

    void onFinish();
}
