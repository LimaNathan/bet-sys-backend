package com.coticbet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoticBetApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoticBetApplication.class, args);
    }
}
