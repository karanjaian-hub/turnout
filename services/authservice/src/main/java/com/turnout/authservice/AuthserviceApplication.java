package com.turnout.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ComponentScan(basePackages = {
        "com.turnout.authservice",
        "com.turnout.common"
})
public class AuthserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthserviceApplication.class, args);
    }
}
