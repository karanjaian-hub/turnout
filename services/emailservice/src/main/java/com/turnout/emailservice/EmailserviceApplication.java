package com.turnout.emailservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.turnout.emailservice.config.BrevoProperties;

@SpringBootApplication
@EnableConfigurationProperties(BrevoProperties.class)
public class EmailserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmailserviceApplication.class, args);
    }
}
