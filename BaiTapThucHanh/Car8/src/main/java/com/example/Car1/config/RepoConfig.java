package com.example.Car1.config;

import com.example.Car1.repository.CarDao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepoConfig {
    @Bean
    public CarDao carDao(){
        return new CarDao("car.csv");
    }
}
