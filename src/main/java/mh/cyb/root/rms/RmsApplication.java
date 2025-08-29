package mh.cyb.root.rms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RmsApplication.class, args);
        System.out.println("\n=== School Exam Result System Started ===");
        System.out.println("Access at: http://localhost:8080");
        System.out.println("Sample students: 101, 102, 103, 201, 202");
        System.out.println("=========================================\n");
    }
}
