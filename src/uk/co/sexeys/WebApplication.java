package uk.co.sexeys;

import java.io.FileNotFoundException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point for the web-based JIM interface.
 * Run this class to start the web server.
 *
 * Usage:
 *   java -cp jim.jar uk.co.sexeys.WebApplication
 *
 * Or run directly from IDE.
 *
 * The web interface will be available at http://localhost:8080
 */
@SpringBootApplication
public class WebApplication {

    public static void main(String[] args) {
        // Initialize fixed spares pool
        Fix.InitSpares();

        // Start Spring Boot
        SpringApplication.run(WebApplication.class, args);
     }
}
