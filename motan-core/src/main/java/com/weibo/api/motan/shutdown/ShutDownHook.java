package com.weibo.api.motan.shutdown;

import java.util.Stack;

/**
 * Created by voyager on 2017/5/2.
 */
public class ShutDownHook extends Thread{
    //java.util.Stack is synchronized
    private static Stack<Closable> toClose;
    private static boolean isRun = false;
    @Override
    public synchronized void start(){
        if(isRun){
            while(!toClose.isEmpty()){
                toClose.pop().close();
            }
        }
    }

    public static void registerShutdownHook(Closable serviceToClose){
        if(toClose==null){
            toClose = new Stack<Closable>();
            isRun = true;
        }
        toClose.push(serviceToClose);
    }
}
