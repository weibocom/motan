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

package com.weibo.dao;

import com.weibo.model.OperationRecord;

import java.util.List;

public interface OperationRecordMapper {
    OperationRecord selectByPrimaryKey(Integer id);

    List<OperationRecord> selectAll();

    int deleteByPrimaryKey(Integer id);

    int insert(OperationRecord record);

    int insertSelective(OperationRecord record);

    int updateByPrimaryKeySelective(OperationRecord record);

    int updateByPrimaryKey(OperationRecord record);
}