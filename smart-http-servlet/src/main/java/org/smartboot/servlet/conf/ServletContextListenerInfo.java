package org.smartboot.servlet.conf;

import java.util.EventListener;

/**
 * @author 三刀
 * @version V1.0 , 2019/12/13
 */
public class ServletContextListenerInfo {
    private final String listenerClass;
    private EventListener listener;

    public ServletContextListenerInfo(String listenerClass) {
        this.listenerClass = listenerClass;
    }

    public EventListener getListener() {
        return listener;
    }

    public void setListener(EventListener listener) {
        this.listener = listener;
    }

    public String getListenerClass() {
        return listenerClass;
    }
}
