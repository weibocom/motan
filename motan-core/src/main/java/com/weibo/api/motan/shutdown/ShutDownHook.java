package com.weibo.api.motan.shutdown;

import com.weibo.api.motan.util.LoggerUtil;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author zhanran
 * add a shutDownHook to close some global connections/services
 */

public class ShutDownHook extends Thread{
    //Smaller the priority is,earlier the resource is to be closed,default Priority is 20
    private static final int defaultPriority = 20;
    //only global resource should be register to ShutDownHook,don't register connections to it.
    private static ShutDownHook instance;
    private ArrayList<closableObject> resourceList = new ArrayList<closableObject>();

    private ShutDownHook(){}


    private static void init(){
        if(instance == null){
            instance = new ShutDownHook();
            Runtime.getRuntime().addShutdownHook(instance);
            LoggerUtil.info("ShutdownHook is initialized");
        }
    }

    @Override
    public synchronized void start(){
        if(instance!=null){
            LoggerUtil.info("start to close global resource due to priority");
            Collections.sort(resourceList);
            for (closableObject resource:resourceList) {
                try{
                    resource.closable.close();
                }catch (Exception e){
                    LoggerUtil.error("Failed to close "+resource.closable.getClass(),e);
                }
                LoggerUtil.info("Success to close "+resource.closable.getClass());
            }
            LoggerUtil.info("Success to close all the resource!");
            resourceList.clear();
        }else{
            System.exit(0);
        }
    }

    public static synchronized void registerShutdownHook(Closable closable){
        registerShutdownHook(closable,defaultPriority);
    }

    public static synchronized void registerShutdownHook(Closable closable,int priority){
        if(instance==null){
            init();
        }
        instance.resourceList.add(new closableObject(closable,priority));
    }

    private static class closableObject implements Comparable<closableObject>{
        Closable closable;
        int priority;

//        public closableObject(Closable closable){
//            this.closable = closable;
//            this.priority = defaultPriority;
//        }

        public closableObject(Closable closable,int priority){
            this.closable = closable;
            this.priority = priority;
        }

        @Override
        public int compareTo(closableObject o) {
            if(this.priority>o.priority) return -1;
            else if(this.priority == o.priority) return 0;
            else return 1;
        }
    }
}
