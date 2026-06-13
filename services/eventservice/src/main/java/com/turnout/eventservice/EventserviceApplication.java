package com.turnout.eventservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EventserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventserviceApplication.class, args);
    }
}
