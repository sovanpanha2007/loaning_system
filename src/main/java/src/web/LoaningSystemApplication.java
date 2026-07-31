package src.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Separate entry point from src.controller.Main (the CLI). Both share the same LoaningSystem
// business-logic layer and the same SQLite database file; this one just exposes it over HTTP.
@SpringBootApplication
public class LoaningSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoaningSystemApplication.class, args);
    }
}
