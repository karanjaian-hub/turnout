package com.turnout.aiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.turnout.aiservice", "com.turnout.common"})
public class AiserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiserviceApplication.class, args);
    }
}
