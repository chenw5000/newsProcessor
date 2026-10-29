package com.newsprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NewsProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewsProcessorApplication.class, args);
    }
}
