package com.weibo.api.motan.transport;

/**
 * @author sunnights
 */
public interface SharedObjectFactory<T> {

    T makeObject() throws Exception;

    boolean rebuildObject(T obj) throws Exception;

}
