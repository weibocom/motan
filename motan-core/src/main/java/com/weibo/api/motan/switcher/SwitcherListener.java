package com.weibo.api.motan.switcher;

/**
 * Created by axb on 16/4/25.
 */
public interface SwitcherListener {

    void onValueChanged(String key,Boolean value);
}
