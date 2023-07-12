package org.smartboot.http.demo.controller;

import org.smartboot.http.restful.annotation.Controller;
import org.smartboot.http.restful.annotation.RequestMapping;

@Controller("controller2")
public class Controller2 {
    @RequestMapping("/helloworld")
    public String helloworld() {
        return "hello " + Controller2.class.getSimpleName();
    }
}
