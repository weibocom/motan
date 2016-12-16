/*
 * Copyright 2009-2016 Weibo, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.api.motan.serialize;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class BaseModel implements Serializable {
    public static BaseModel getRandom(){
        Random random = new Random();
        
        BaseModel model = new BaseModel();
        model.i = random.nextInt();
        model.l = random.nextLong();
        model.bool = random.nextBoolean();
        model.f = random.nextFloat();
        model.d = random.nextDouble();
        model.integer = random.nextInt();
        model.array = new byte[40];
        random.nextBytes(model.array);
        model.str = String.valueOf(model.i);
        
        SubModel<String> sub = new SubModel<String>();
        for(int i =0; i< 5; i++){
            int temp = random.nextInt();
            sub.putMap("key" + i, "v" + temp);
            sub.addList(String.valueOf(temp));
        }
        model.subModel = sub;
        return model;
    }
    
    private static final long serialVersionUID = -6654784635984161860L;
    private int i;
    private Long l;
    private float f;
    private Double d;
    private Integer integer;
    private boolean bool;
    private String str;
    private byte[] array;
    private SubModel subModel;

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public Integer getInteger() {
        return integer;
    }

    public void setInteger(Integer integer) {
        this.integer = integer;
    }

    public Long getL() {
        return l;
    }

    public void setL(Long l) {
        this.l = l;
    }

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public byte[] getArray() {
        return array;
    }

    public void setArray(byte[] array) {
        this.array = array;
    }

    public float getF() {
        return f;
    }

    public void setF(float f) {
        this.f = f;
    }

    public Double getD() {
        return d;
    }

    public void setD(Double d) {
        this.d = d;
    }

    public boolean isBool() {
        return bool;
    }

    public void setBool(boolean bool) {
        this.bool = bool;
    }

    public SubModel getSubModel() {
        return subModel;
    }

    public void setSubModel(SubModel subModel) {
        this.subModel = subModel;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(array);
        result = prime * result + (bool ? 1231 : 1237);
        result = prime * result + ((d == null) ? 0 : d.hashCode());
        result = prime * result + Float.floatToIntBits(f);
        result = prime * result + i;
        result = prime * result + ((integer == null) ? 0 : integer.hashCode());
        result = prime * result + ((l == null) ? 0 : l.hashCode());
        result = prime * result + ((str == null) ? 0 : str.hashCode());
        result = prime * result + ((subModel == null) ? 0 : subModel.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        BaseModel other = (BaseModel) obj;
        if (!Arrays.equals(array, other.array)) return false;
        if (bool != other.bool) return false;
        if (d == null) {
            if (other.d != null) return false;
        } else if (!d.equals(other.d)) return false;
        if (Float.floatToIntBits(f) != Float.floatToIntBits(other.f)) return false;
        if (i != other.i) return false;
        if (integer == null) {
            if (other.integer != null) return false;
        } else if (!integer.equals(other.integer)) return false;
        if (l == null) {
            if (other.l != null) return false;
        } else if (!l.equals(other.l)) return false;
        if (str == null) {
            if (other.str != null) return false;
        } else if (!str.equals(other.str)) return false;
        if (subModel == null) {
            if (other.subModel != null) return false;
        } else if (!subModel.equals(other.subModel)) return false;
        return true;
    }


    static class SubModel<T> implements Serializable {
        private static final long serialVersionUID = 8308370279201177508L;
        private List<T> list = new ArrayList<T>();
        private Map<String, T> map = new HashMap<String, T>();

        public List<T> getList() {
            return list;
        }

        public void setList(List<T> list) {
            this.list = list;
        }

        public Map<String, T> getMap() {
            return map;
        }

        public void setMap(Map<String, T> map) {
            this.map = map;
        }

        public void addList(T object) {
            list.add(object);
        }

        public void putMap(String key, T value) {
            map.put(key, value);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((list == null) ? 0 : list.hashCode());
            result = prime * result + ((map == null) ? 0 : map.hashCode());
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            SubModel<T> other = (SubModel<T>) obj;
            if (list == null) {
                if (other.list != null) return false;
            } else if (!list.equals(other.list)) return false;
            if (map == null) {
                if (other.map != null) return false;
            } else if (!map.equals(other.map)) return false;
            return true;
        }

    }
}
