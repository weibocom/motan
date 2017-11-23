package com.weibo.api.motan.transport;

/**
 * @author sunnights
 */
public interface SharedObjectFactory<T> {

    T makeObject();

    boolean initObject(T obj) throws Exception;

    boolean rebuildObject(T obj);

}
