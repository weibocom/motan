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

package com.weibo.service;

import com.alibaba.fastjson.JSON;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.dao.OperationRecordMapper;
import com.weibo.exception.CustomException;
import com.weibo.model.OperationRecord;
import com.weibo.utils.TokenUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Created by Zhang Yu on 2015/12/24 0024 10:43.
 */
@Component
@Aspect
public class LoggingAspect {
    @Autowired(required = false)
    private OperationRecordMapper recordMapper;

    // 任意公共方法的执行
    @Pointcut("execution(public * * (..))")
    private void anyPublicOperation() {
    }

    // CommandService接口定义的任意方法的执行
    @Pointcut("execution(* com.weibo.service.CommandService+.*(..))")
    private void execCommandOperation() {
    }

    @AfterReturning(value = "anyPublicOperation() && execCommandOperation()", returning = "result")
    public void logAfter(JoinPoint joinPoint, boolean result) {
        Object[] args = joinPoint.getArgs();

        OperationRecord record = new OperationRecord();
        record.setOperator(getUsername());
        record.setType(joinPoint.getSignature().getName());
        record.setGroupName(args[0].toString());
        record.setCommand(JSON.toJSONString(args[1]));
        int status = result ? 1 : 0;
        record.setStatus((byte) status);

        if (recordMapper == null) {
            LoggerUtil.accessLog(JSON.toJSONString(record));
        } else {
            recordMapper.insertSelective(record);
        }
    }

    private String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof AnonymousAuthenticationToken) {
            throw new CustomException.UnauthorizedException();
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return TokenUtils.getUserNameFromToken(userDetails.getUsername());
    }
}
