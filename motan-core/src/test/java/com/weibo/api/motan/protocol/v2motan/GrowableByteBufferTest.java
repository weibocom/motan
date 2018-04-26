package com.weibo.api.motan.protocol.v2motan;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created on 2018/4/10
 *
 * @author: luominggang
 * Description:
 */
public class GrowableByteBufferTest {

    @Test
    public void testZigzag() {
        GrowableByteBuffer buffer = new GrowableByteBuffer(16);
        for (int i = 1; i != 0x80000000; i <<= 1) {
            assertZigzag32(i, buffer);
        }

        for (int i = -1; i != 0; i <<= 1) {
            assertZigzag32(i, buffer);
        }

        assertZigzag32(0xffffffff, buffer);
        assertZigzag32(0, buffer);

        for (long i = 1; i != 0x8000000000000000L; i <<= 1) {
            assertZigzag64(i, buffer);
        }

        for (long i = -1; i != 0; i <<= 1) {
            assertZigzag64(i, buffer);
        }

        assertZigzag64(0xffffffffffffffffL, buffer);
        assertZigzag64(0L, buffer);

        int times = 128;
        int intBase = 1678;
        buffer = new GrowableByteBuffer(times * 8);
        for (int i = 0; i < times; i++) {
            buffer.putZigzag32(intBase * i);
        }
        buffer.flip();
        for (int i = 0; i < times; i++) {
            assertEquals(i * intBase, buffer.getZigZag32());
        }
        assertEquals(0, buffer.remaining());
        buffer.clear();

        long longBase = 7289374928L;
        buffer = new GrowableByteBuffer(times * 8);
        for (int i = 0; i < times; i++) {
            buffer.putZigzag64(longBase * i);
        }
        buffer.flip();
        for (int i = 0; i < times; i++) {
            assertEquals(i * longBase, buffer.getZigZag64());
        }
        assertEquals(0, buffer.remaining());
        buffer.clear();
    }

    @Test
    public void testGetAndPut() {
        GrowableByteBuffer buffer = new GrowableByteBuffer(16);
        buffer.put((byte) 1);
        buffer.flip();
        assertEquals((byte)1, buffer.get());
        assertEquals(0, buffer.remaining());
        assertEquals(1, buffer.position());
        buffer.clear();

        buffer.putShort((short) 1);
        buffer.flip();
        assertEquals((short) 1, buffer.getShort());
        assertEquals(0, buffer.remaining());
        assertEquals(2, buffer.position());
        buffer.clear();

        buffer.putInt(1);
        buffer.flip();
        assertEquals(1, buffer.getInt());
        assertEquals(0, buffer.remaining());
        assertEquals(4, buffer.position());
        buffer.clear();

        buffer.putLong( 1L);
        buffer.flip();
        assertEquals(1L, buffer.getLong());
        assertEquals(0, buffer.remaining());
        assertEquals(8, buffer.position());
        buffer.clear();

        buffer.putFloat(3.1415926f);
        buffer.flip();
        assertEquals(Float.floatToRawIntBits(3.1415926f), Float.floatToRawIntBits(buffer.getFloat()));
        assertEquals(0, buffer.remaining());
        assertEquals(4, buffer.position());
        buffer.clear();

        buffer.putDouble( 3.1415926d);
        buffer.flip();
        assertEquals(Double.doubleToRawLongBits(3.1415926d), Double.doubleToRawLongBits(buffer.getDouble()));
        assertEquals(0, buffer.remaining());
        assertEquals(8, buffer.position());
        buffer.clear();

        byte[] bs = new byte[] {1, 2, 3, 4, 5, 6};
        byte[] dst = new byte[bs.length];
        buffer.put(bs);
        buffer.flip();
        buffer.get(dst);
        assertArrayEquals(bs, dst);
        assertEquals(0, buffer.remaining());
        assertEquals(bs.length, buffer.position());
    }

    @Test
    public void testGetAndPutWithIndex() {
        GrowableByteBuffer buffer = new GrowableByteBuffer(1);
        int index = 0;
        buffer.position(1);
        buffer.put(index, (byte) 1);
        buffer.flip();
        assertEquals((byte)1, buffer.get(index));
        assertEquals(0, buffer.position());
        buffer.clear();

        buffer.position(2);
        buffer.putShort(index, (short) 1);
        buffer.flip();
        assertEquals((short) 1, buffer.getShort(index));
        assertEquals(0, buffer.position());
        buffer.clear();

        buffer.position(4);
        buffer.putInt(index, 1);
        buffer.flip();
        assertEquals(1, buffer.getInt(index));
        assertEquals(0, buffer.position());
        buffer.clear();

        buffer.position(8);
        buffer.putLong( index, 1L);
        buffer.flip();
        assertEquals(1L, buffer.getLong(index));
        assertEquals(0, buffer.position());
        buffer.clear();

        buffer.position(4);
        buffer.putFloat(index, 3.1415926f);
        buffer.flip();
        assertEquals(Float.floatToRawIntBits(3.1415926f), Float.floatToRawIntBits(buffer.getFloat(index)));
        assertEquals(0, buffer.position());
        buffer.clear();

        buffer.position(8);
        buffer.putDouble( index, 3.1415926d);
        buffer.flip();
        assertEquals(Double.doubleToRawLongBits(3.1415926d), Double.doubleToRawLongBits(buffer.getDouble(index)));
        assertEquals(0, buffer.position());
        buffer.clear();
    }

    private void assertZigzag32(int value, GrowableByteBuffer buffer) {
        buffer.clear();
        buffer.putZigzag32(value);
        buffer.flip();
        assertEquals(value, buffer.getZigZag32());
        assertEquals(0, buffer.remaining());
        buffer.clear();
    }

    private void assertZigzag64(long value, GrowableByteBuffer buffer) {
        buffer.clear();
        buffer.putZigzag64(value);
        buffer.flip();
        assertEquals(value, buffer.getZigZag64());
        assertEquals(0, buffer.remaining());
        buffer.clear();
    }
}
