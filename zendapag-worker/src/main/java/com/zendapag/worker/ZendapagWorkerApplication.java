package com.zendapag.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.zendapag"})
@EntityScan(basePackages = {"com.zendapag.core.entity"})
@EnableJpaRepositories(basePackages = {"com.zendapag.core.repository"})
@EnableKafka
@EnableScheduling
public class ZendapagWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZendapagWorkerApplication.class, args);
    }
}