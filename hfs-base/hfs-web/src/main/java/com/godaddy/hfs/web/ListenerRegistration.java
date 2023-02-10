package com.godaddy.hfs.web;

import javax.servlet.ServletContextListener;

public interface ListenerRegistration {

    void addEventListener(ServletContextListener listener);

    void addEventListener(Class<? extends ServletContextListener> listenerClass);

}
