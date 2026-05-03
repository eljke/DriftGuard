package ru.eljke.driftguard.demo;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "DriftGuard Demo API", version = "1.0.0"))
public class DriftGuardDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DriftGuardDemoApplication.class, args);
    }
}
