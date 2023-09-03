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

import com.caucho.hessian.io.ClassFactory;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import com.weibo.api.motan.codec.Serialization;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.util.LoggerUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * hessian2 序列化，要求序列化的对象实现 java.io.Serializable 接口
 *
 * @author maijunsheng
 * @version 创建时间：2013-5-30
 */
@SpiMeta(name = "hessian2")
public class Hessian2Serialization implements Serialization {
    private static volatile SerializerFactory serializerFactory; // global serializer factory
    private static final Boolean canSetDeny; // does the current Hessian version support blacklist setting

    static {
        canSetDeny = checkCompatibility();
        serializerFactory = initDefaultSerializerFactory();
    }

    @Override
    public byte[] serialize(Object data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bos);
        out.setSerializerFactory(serializerFactory);
        out.writeObject(data);
        out.flush();
        return bos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(byte[] data, Class<T> clz) throws IOException {
        Hessian2Input input = new Hessian2Input(new ByteArrayInputStream(data));
        input.setSerializerFactory(serializerFactory);
        return (T) input.readObject(clz);
    }

    @Override
    public byte[] serializeMulti(Object[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bos);
        out.setSerializerFactory(serializerFactory);
        for (Object obj : data) {
            out.writeObject(obj);
        }
        out.flush();
        return bos.toByteArray();
    }

    @Override
    public Object[] deserializeMulti(byte[] data, Class<?>[] classes) throws IOException {
        Hessian2Input input = new Hessian2Input(new ByteArrayInputStream(data));
        input.setSerializerFactory(serializerFactory);
        Object[] objects = new Object[classes.length];
        for (int i = 0; i < classes.length; i++) {
            objects[i] = input.readObject(classes[i]);
        }
        return objects;
    }

    @Override
    public int getSerializationNumber() {
        return 0;
    }

    public static boolean deny(String pattern) {
        if (canSetDeny && StringUtils.isNotBlank(pattern)) {
            serializerFactory.getClassFactory().deny(pattern);
            return true;
        }
        return false;
    }

    public static void setSerializerFactory(SerializerFactory serializerFactory) {
        Hessian2Serialization.serializerFactory = serializerFactory;
    }

    private static Boolean checkCompatibility() {
        try {
            SerializerFactory.class.getMethod("getClassFactory"); // hessian version >= 4.0.51
            return true;
        } catch (NoSuchMethodException ignore) {
        }
        return false;
    }

    private static SerializerFactory initDefaultSerializerFactory() {
        SerializerFactory defaultSerializerFactory = new SerializerFactory();
        if (canSetDeny) {
            try {
                ClassFactory classFactory = defaultSerializerFactory.getClassFactory();
                classFactory.setWhitelist(false); // blacklist mode
                classFactory.deny("ch.qos.logback.core.*");
                classFactory.deny("clojure.*");
                classFactory.deny("com.caucho.config.types.*");
                classFactory.deny("com.caucho.hessian.test.*");
                classFactory.deny("com.caucho.naming.*");
                classFactory.deny("com.mchange.v2.c3p0.*");
                classFactory.deny("com.mysql.jdbc.util.*");
                classFactory.deny("com.rometools.rome.feed.*");
                classFactory.deny("com.sun.corba.se.*");
                classFactory.deny("com.sun.jndi.*");
                classFactory.deny("com.sun.org.apache.bcel.*");
                classFactory.deny("com.sun.org.apache.xalan.*");
                classFactory.deny("com.sun.org.apache.xml.*");
                classFactory.deny("com.sun.org.apache.xpath.*");
                classFactory.deny("com.sun.rowset.*");
                classFactory.deny("com.sun.xml.internal.bind.v2.*");
                classFactory.deny("java.awt.*");
                classFactory.deny("java.beans.*");
                classFactory.deny("java.lang.ProcessBuilder");
                classFactory.deny("java.rmi.server.*");
                classFactory.deny("java.security.*");
                classFactory.deny("java.util.ServiceLoader*");
                classFactory.deny("java.util.StringTokenizer");
                classFactory.deny("javassist.*");
                classFactory.deny("javax.imageio.*");
                classFactory.deny("javax.management.*");
                classFactory.deny("javax.media.jai.remote.*");
                classFactory.deny("javax.naming.*");
                classFactory.deny("javax.script.*");
                classFactory.deny("javax.sound.sampled.*");
                classFactory.deny("javax.swing.*");
                classFactory.deny("javax.xml.transform.*");
                classFactory.deny("net.bytebuddy.*");
                classFactory.deny("oracle.jdbc.*");
                classFactory.deny("org.apache.carbondata.core.scan.*");
                classFactory.deny("org.apache.commons.beanutils.*");
                classFactory.deny("org.apache.commons.dbcp.*");
                classFactory.deny("org.apache.ibatis.executor.*");
                classFactory.deny("org.apache.ibatis.javassist.*");
                classFactory.deny("org.apache.tomcat.dbcp.*");
                classFactory.deny("org.apache.wicket.util.*");
                classFactory.deny("org.apache.xalan.*");
                classFactory.deny("org.apache.xpath.*");
                classFactory.deny("org.aspectj.*");
                classFactory.deny("org.codehaus.groovy.runtime.*");
                classFactory.deny("org.eclipse.jetty.util.*");
                classFactory.deny("org.geotools.filter.*");
                classFactory.deny("org.springframework.aop.*");
                classFactory.deny("org.springframework.beans.factory.*");
                classFactory.deny("org.springframework.expression.*");
                classFactory.deny("org.springframework.jndi.*");
                classFactory.deny("org.springframework.orm.jpa.*");
                classFactory.deny("org.springframework.transaction.*");
                classFactory.deny("org.yaml.snakeyaml.tokens.*");
                classFactory.deny("sun.print.*");
                classFactory.deny("sun.rmi.*");
                classFactory.deny("sun.swing.*");
            } catch (Exception e) {
                LoggerUtil.warn("Hessian2Serialization init deny list failed, please upgrade hessian to version 4.0.66 or later", e);
            }
        }
        return defaultSerializerFactory;
    }
}
