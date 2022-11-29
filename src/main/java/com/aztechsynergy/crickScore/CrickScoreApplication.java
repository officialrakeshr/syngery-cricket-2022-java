package com.aztechsynergy.crickScore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CrickScoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrickScoreApplication.class, args);
    }

}
