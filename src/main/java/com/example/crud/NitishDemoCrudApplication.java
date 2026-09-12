package com.example.crud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.crud"})  // ← ADD THIS
public class NitishDemoCrudApplication {

    public static void main(String[] args) {
        SpringApplication.run(NitishDemoCrudApplication.class, args);
        System.out.println("✅ CRUD Application Started Successfully!");
        System.out.println("📝 API Endpoint: http://localhost:8080/api/users/create");
        System.out.println("📊 Database: nitish");
    }
}