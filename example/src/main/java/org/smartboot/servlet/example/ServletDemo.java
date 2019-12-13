package org.smartboot.servlet.example;

import org.smartboot.http.HttpBootstrap;
import org.smartboot.servlet.ServletHttpHandle;

import java.net.MalformedURLException;

/**
 * @author 三刀
 * @version V1.0 , 2019/12/11
 */
public class ServletDemo {
    public static void main(String[] args) throws MalformedURLException {
        System.setProperty("sun.misc.URLClassPath.debug","true");
        HttpBootstrap bootstrap = new HttpBootstrap();
        bootstrap.pipeline().next(new ServletHttpHandle());
        bootstrap.setPort(8080).start();
    }
}