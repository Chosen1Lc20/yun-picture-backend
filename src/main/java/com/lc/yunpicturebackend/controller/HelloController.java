package com.lc.yunpicturebackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/hello")
public class HelloController {
    @GetMapping("user")
    public String hello() {
        return "hello world";
    }
}
