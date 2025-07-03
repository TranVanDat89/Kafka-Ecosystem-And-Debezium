package com.dattran.kafka_etl.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UploadFileController {
    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/upload-test")
    public String uploadTest() {
        return "index";
    }
}
