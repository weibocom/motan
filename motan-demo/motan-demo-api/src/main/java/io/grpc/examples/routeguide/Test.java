package io.grpc.examples.routeguide;

import com.weibo.api.motan.serialize.ProtobufSerialization;

/**
 * Created by zhanglei28 on 2017/4/27.
 */
public class Test {
    public static void main(String[] args) throws Exception {
        ProtobufSerialization serialization = new ProtobufSerialization();

        Point point1 = Point.newBuilder().setLatitude(123).setLongitude(4567).build();
        Point point2 = Point.newBuilder().setLatitude(333).setLongitude(555).build();
        byte[] bytes = serialization.serialize(point2);
        Point p = serialization.deserialize(bytes, Point.class);

        bytes = serialization.serializeMulti(new Object[]{point1, point2});

        Object[] objects = serialization.deserializeMulti(bytes, new Class[]{Point.class, Point.class});
        System.out.println(objects.length);
    }
}
