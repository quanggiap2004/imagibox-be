package com.imagibox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ImagiBoxApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImagiBoxApplication.class, args);
    }
}
