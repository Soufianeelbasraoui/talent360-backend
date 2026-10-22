package com.talent360;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class Talent360Application {

    public static void main(String[] args) {
        SpringApplication.run(Talent360Application.class, args);
    }
}
