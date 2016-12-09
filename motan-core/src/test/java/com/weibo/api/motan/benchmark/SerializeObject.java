package com.weibo.api.motan.benchmark;

import java.io.Serializable;
import java.util.Map;

public class SerializeObject implements Serializable {
    private static final long serialVersionUID = 2366873906296131107L;
    private long age;
    private String hello;
    private int[] arr;
    private int[][] arr2;
    private Map<String, Object> obj;

    public SerializeObject() {}

    public void setAge(long age) {
        this.age = age;
    }

    public void setHello(String hello) {
        this.hello = hello;
    }

    public void setArr(int[] arr) {
        this.arr = arr;
    }

    public void setArr2(int[][] arr2) {
        this.arr2 = arr2;
    }

    public long getAge() {
        return age;
    }

    public String getHello() {
        return hello;
    }

    public int[] getArr() {
        return arr;
    }

    public int[][] getArr2() {
        return arr2;
    }

    public Map<String, Object> getObj() {
        return obj;
    }

    public void setObj(Map<String, Object> obj) {
        this.obj = obj;
    }
}