package com.example.loginsystem;

// =============================================================================
// LoginSystemApplication.java — The Application Entry Point
// =============================================================================
//
// WHAT IS THIS?
// ─────────────
// Every Spring Boot application has exactly one main class.
// It contains the standard Java main() method — the start of execution.
//
// @SpringBootApplication is actually THREE annotations combined:
//
//   @SpringBootConfiguration
//     → Marks this as a configuration class (can define @Bean methods)
//
//   @EnableAutoConfiguration
//     → THE MAGIC OF SPRING BOOT!
//     → Spring Boot scans your classpath and automatically configures things.
//     → Has H2 on classpath? → Configures DataSource automatically.
//     → Has spring-security? → Enables security automatically.
//     → Has spring-web? → Sets up DispatcherServlet automatically.
//     → You would normally need 100s of lines of XML for all this setup.
//
//   @ComponentScan
//     → Scans the current package (and sub-packages) for Spring components.
//     → Finds all @Service, @Repository, @Controller, @Component classes.
//     → Registers them as beans in the Spring application context.
//
// SpringApplication.run():
//   1. Creates the Spring Application Context (IoC container)
//   2. Starts the embedded Tomcat server
//   3. Registers all beans (via component scanning)
//   4. Runs auto-configuration
//   5. App is ready to accept requests!
//
// IoC (Inversion of Control):
//   Instead of YOU creating objects (new UserService()), Spring creates
//   and manages them. You just say "I need a UserService" (@Autowired or
//   constructor injection) and Spring provides it. This is "Hollywood
//   Principle": "Don't call us, we'll call you."
//
// =============================================================================

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LoginSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoginSystemApplication.class, args);
        // That's it! Spring Boot handles the rest.
        // The server starts and listens on http://localhost:8080
    }
}
