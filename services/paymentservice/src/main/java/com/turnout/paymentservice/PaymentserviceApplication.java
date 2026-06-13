package com.turnout.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing  // Required for @CreatedDate / @LastModifiedDate on entities
public class PaymentserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentserviceApplication.class, args);
    }
}
