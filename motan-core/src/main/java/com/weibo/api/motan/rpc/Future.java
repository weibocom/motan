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

package com.weibo.api.motan.rpc;

/**
 * 
 * @author maijunsheng
 * @version 创建时间：2013-6-14
 * 
 */
public interface Future {
    /**
     * cancle the task
     * 
     * @return
     */
    boolean cancel();

    /**
     * task cancelled
     * 
     * @return
     */
    boolean isCancelled();

    /**
     * task is complete : normal or exception
     * 
     * @return
     */
    boolean isDone();

    /**
     * isDone() & normal
     * 
     * @return
     */
    boolean isSuccess();

    /**
     * if task is success, return the result.
     * 
     * @throws Exception when timeout, cancel, onFailure
     * @return
     */
    Object getValue();

    /**
     * if task is done or cancle, return the exception
     * 
     * @return
     */
    Exception getException();

    /**
     * add future listener , when task is success，failure, timeout, cancel, it will be called
     * 
     * @param listener
     */
    void addListener(FutureListener listener);

}
