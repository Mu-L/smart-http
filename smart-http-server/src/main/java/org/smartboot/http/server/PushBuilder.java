package org.smartboot.http.server;

import java.util.Set;

public interface PushBuilder {

    PushBuilder method(String method);

    PushBuilder queryString(String queryString);

    PushBuilder setHeader(String name, String value);


    PushBuilder addHeader(String name, String value);


    PushBuilder removeHeader(String name);


    PushBuilder path(String path);


    void push();


    String getMethod();


    String getQueryString();


    String getSessionId();


    Set<String> getHeaderNames();


    String getHeader(String name);

    String getPath();
}
