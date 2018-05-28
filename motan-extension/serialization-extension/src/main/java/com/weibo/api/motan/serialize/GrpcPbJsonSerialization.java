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

package com.weibo.api.motan.serialize;

import com.weibo.api.motan.core.extension.SpiMeta;

/**
 * Created by zhanglei28 on 2017/5/15.
 * grpc pb json 仅作为不同序列化标识，序列化处理逻辑与grpc pb一致
 */
@SpiMeta(name = "grpc-pb-json")
public class GrpcPbJsonSerialization extends GrpcPbSerialization {

    @Override
    public int getSerializationNumber() {
        return 7;
    }
}
