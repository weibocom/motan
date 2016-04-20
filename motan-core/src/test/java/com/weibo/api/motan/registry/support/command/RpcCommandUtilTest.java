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

package com.weibo.api.motan.registry.support.command;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.regex.Pattern;

/**
 * @author chengya1
 */
public class RpcCommandUtilTest {

    @Test
    public void testPathMatch() {
        assertFalse(RpcCommandUtil.match("b*  & !bc \n | a* & !ac \t\n | c*", "bc"));
        assertFalse(RpcCommandUtil.match("b*  & !bc* \n | a* & !ac \t\n | c*", "bcd"));
        assertFalse(RpcCommandUtil.match("b*  & !bc \n | a* & !ac \t\n | c*", "ac"));
        assertFalse(RpcCommandUtil.match("b*  & !bc \n | (a* & !ac) \t\n | c*", "bc"));
        assertFalse(RpcCommandUtil.match("b*  & !bc* \n | (a* & !ac) \t\n | c*", "bcd"));
        assertFalse(RpcCommandUtil.match("b*  & !bc \n | a* & !ac \t\n | c*", "ac"));
        assertFalse(RpcCommandUtil.match("b*&!bc&!bd", "bc"));
        assertFalse(RpcCommandUtil.match("b*&!bc&!bd", "bd"));
        assertFalse(RpcCommandUtil.match("((a*&!aa)|b*)", "aa"));

        assertTrue(RpcCommandUtil.match("b*  & !bc \n | a* & !ac \t\n | c*", "ba"));
        assertTrue(RpcCommandUtil.match("b*  & !bc \n | a* & !ac \t\n | c*", "aaa"));
        assertTrue(RpcCommandUtil.match("b*  & !bc \n | a* & !ac \t\n | c*", "acc"));
        assertTrue(RpcCommandUtil.match("b*  & !bc \n | a* & !ac \t\n | c*", "cel"));
        assertTrue(RpcCommandUtil.match("b*  & !bc \n | (a* & !ac) \t\n | c*", "ba"));
        assertTrue(RpcCommandUtil.match("b*  & !bc \n | a* & !ac \t\n | c*", "aaa"));
        assertTrue(RpcCommandUtil.match("b*  & !bc \n | a* & !ac \t\n | c*", "acc"));
        assertTrue(RpcCommandUtil.match("b*  & !bc \n | a* & !ac \t\n | c*", "cel"));
        assertTrue(RpcCommandUtil.match("((b*&!bc) | (a*&!ab))", "ba"));
        assertTrue(RpcCommandUtil.match("a*", "a"));
        assertTrue(RpcCommandUtil.match("b* | (a* & !ab) | (c* & !cc)", "accc"));
    }

    @Test
    public void testRouteRuleMath() {
        Pattern p = Pattern.compile("^!?[0-9.]+\\*?$");

        assertTrue(p.matcher("10.75.0.180").find());
        assertTrue(p.matcher("!10.75.0.180").find());
        assertTrue(p.matcher("10.75.0*").find());
        assertTrue(p.matcher("10.75.0.*").find());
        assertTrue(p.matcher("!10.75.0.*").find());
        assertTrue(p.matcher("!10.75.0*").find());

        assertFalse(p.matcher("!!10.75.0.180").find());
        assertFalse(p.matcher("a").find());
        assertFalse(p.matcher("10.75.**").find());
    }

    @Test
    public void testCodec() {
        String commandString = Constants.commandString1;
        RpcCommand command = RpcCommandUtil.stringToCommand(commandString);
        assertNotNull(command);

        String temp = RpcCommandUtil.commandToString(command);
        assertEquals(commandString, temp);
    }

}


class Constants {
    static String commandString1 =
            "{\"clientCommandList\":[{\"dc\":\"yf\",\"index\":1,\"mergeGroups\":[\"openapi-tc-test-rpc:1\",\"openapi-yf-test-rpc:1\"],\"pattern\":\"com.weibo.Hello\",\"remark\":\"切换50%流量到另外一个机房\",\"routeRules\":[],\"version\":\"1.0\"}]}";

}
