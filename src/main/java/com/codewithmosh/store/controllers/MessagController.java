package com.codewithmosh.store.controllers;

import com.codewithmosh.store.entities.Message;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController //返回json数据
public class MessagController {
    @RequestMapping("/Hello")
    public Message hello() {
        return new Message("Hello World");
    }

}
