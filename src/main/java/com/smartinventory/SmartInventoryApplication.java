package com.smartinventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Smart Inventory Management System.
 *
 * <p>Entry point of the Java Web application. Spring Boot bootstraps an embedded
 * Tomcat server, the Spring MVC stack, JPA/Hibernate persistence and Spring
 * Security. The application talks to a MySQL database and (optionally) to a
 * Python Flask AI microservice for machine-learning powered insights.</p>
 */
@SpringBootApplication
@EnableScheduling
public class SmartInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartInventoryApplication.class, args);
    }
}
