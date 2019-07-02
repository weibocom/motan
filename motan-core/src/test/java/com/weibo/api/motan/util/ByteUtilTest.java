package com.weibo.api.motan.util;

import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;

import com.diffblue.deeptestutils.mock.DTUMemberMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@RunWith(PowerMockRunner.class)
public class ByteUtilTest {

  /* testedClasses: ByteUtil */
  // Test written by Diffblue Cover.
  @Test
  public void bytes2intInput6ZeroOutputZero() {

    // Arrange
    final byte[] bytes = {(byte)0, (byte)0, (byte)0, (byte)0, (byte)1, (byte)1};
    final int off = 0;

    // Act
    final int actual = ByteUtil.bytes2int(bytes, off);

    // Assert result
    Assert.assertEquals(0, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void bytes2longInput10ZeroOutputZero() {

    // Arrange
    final byte[] bytes = {(byte)0, (byte)0, (byte)0, (byte)0, (byte)0,
                          (byte)0, (byte)0, (byte)0, (byte)1, (byte)1};
    final int off = 0;

    // Act
    final long actual = ByteUtil.bytes2long(bytes, off);

    // Assert result
    Assert.assertEquals(0L, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void bytes2shortInput2ZeroOutputZero() {

    // Arrange
    final byte[] b = {(byte)0, (byte)0};
    final int off = 0;

    // Act
    final short actual = ByteUtil.bytes2short(b, off);

    // Assert result
    Assert.assertEquals((short)0, actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void constructorOutputVoid() {

    // Act, creating object to test constructor
    final ByteUtil objectUnderTest = new ByteUtil();

    // Method returns void, testing that no exception is thrown
  }

  // Test written by Diffblue Cover.
  @PrepareForTest({GZIPOutputStream.class, ByteUtil.class})
  @Test
  public void gzipInput0Output0() throws Exception, IOException {

    // Arrange
    final byte[] data = {};
    final GZIPOutputStream gZIPOutputStream = PowerMockito.mock(GZIPOutputStream.class);
    PowerMockito.whenNew(GZIPOutputStream.class)
        .withParameterTypes(OutputStream.class)
        .withArguments(or(isA(OutputStream.class), isNull(OutputStream.class)))
        .thenReturn(gZIPOutputStream);

    // Act
    final byte[] actual = ByteUtil.gzip(data);

    // Assert result
    Assert.assertArrayEquals(new byte[] {}, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void int2bytesInputZero6ZeroOutputVoid() {

    // Arrange
    final int value = 0;
    final byte[] bytes = {(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0};
    final int off = 0;

    // Act
    ByteUtil.int2bytes(value, bytes, off);

    // Method returns void, testing that no exception is thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void long2bytesInputZero10ZeroOutputVoid() {

    // Arrange
    final long value = 0L;
    final byte[] bytes = {(byte)0, (byte)0, (byte)0, (byte)0, (byte)0,
                          (byte)0, (byte)0, (byte)0, (byte)0, (byte)0};
    final int off = 0;

    // Act
    ByteUtil.long2bytes(value, bytes, off);

    // Method returns void, testing that no exception is thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void short2bytesInputZero2ZeroOutputVoid() {

    // Arrange
    final short value = (short)0;
    final byte[] bytes = {(byte)0, (byte)0};
    final int off = 0;

    // Act
    ByteUtil.short2bytes(value, bytes, off);

    // Method returns void, testing that no exception is thrown
  }

  // Test written by Diffblue Cover.
  @Test
  public void toArrayInput0Output0() {

    // Arrange
    final ArrayList<Byte> list = new ArrayList<Byte>();

    // Act
    final byte[] actual = ByteUtil.toArray(list);

    // Assert result
    Assert.assertArrayEquals(new byte[] {}, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void toArrayInput1Output1() {

    // Arrange
    final ArrayList<Byte> list = new ArrayList<Byte>();
    list.add((byte)0);

    // Act
    final byte[] actual = ByteUtil.toArray(list);

    // Assert result
    Assert.assertArrayEquals(new byte[] {(byte)0}, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void toArrayInputNullOutputNull() {

    // Arrange
    final List<Byte> list = null;

    // Act
    final byte[] actual = ByteUtil.toArray(list);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void toListInput0Output0() {

    // Arrange
    final byte[] array = {};

    // Act
    final List<Byte> actual = ByteUtil.toList(array);

    // Assert result
    final ArrayList<Byte> arrayList = new ArrayList<Byte>();
    Assert.assertEquals(arrayList, actual);
  }

  // Test written by Diffblue Cover.

  @Test
  public void toListInput1Output1() {

    // Arrange
    final byte[] array = {(byte)0};

    // Act
    final List<Byte> actual = ByteUtil.toList(array);

    // Assert result
    final ArrayList<Byte> arrayList = new ArrayList<Byte>();
    arrayList.add((byte)0);
    Assert.assertEquals(arrayList, actual);
  }

  // Test written by Diffblue Cover.
  @Test
  public void toListInputNullOutputNull() {

    // Arrange
    final byte[] array = null;

    // Act
    final List<Byte> actual = ByteUtil.toList(array);

    // Assert result
    Assert.assertNull(actual);
  }

  // Test written by Diffblue Cover.
  @PrepareForTest({GZIPInputStream.class, ByteUtil.class})
  @Test
  public void unGzipInput2Output0() throws Exception, IOException {

    // Arrange
    final byte[] data = {(byte)0, (byte)0};
    final GZIPInputStream gZIPInputStream = PowerMockito.mock(GZIPInputStream.class);
    final Method readMethod =
        DTUMemberMatcher.method(GZIPInputStream.class, "read", byte[].class, int.class, int.class);
    PowerMockito.doReturn(-1)
        .when(gZIPInputStream, readMethod)
        .withArguments(or(isA(byte[].class), isNull(byte[].class)), anyInt(), anyInt());
    PowerMockito.whenNew(GZIPInputStream.class)
        .withParameterTypes(InputStream.class)
        .withArguments(or(isA(InputStream.class), isNull(InputStream.class)))
        .thenReturn(gZIPInputStream);

    // Act
    final byte[] actual = ByteUtil.unGzip(data);

    // Assert result
    Assert.assertArrayEquals(new byte[] {}, actual);
  }
}
