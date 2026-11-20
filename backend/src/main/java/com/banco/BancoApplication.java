package com.banco;

import com.banco.modelo.Cuenta;
import com.banco.modelo.CuentaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;

/**
 * Arranque de la aplicación. También concentra CORS y las cuentas demo.
 */
@SpringBootApplication
public class BancoApplication implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String origenes;

    public static void main(String[] args) {
        SpringApplication.run(BancoApplication.class, args);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origenes.split("\\s*,\\s*"))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    /** Crea las cuentas de ejemplo la primera vez que arranca el sistema. */
    @Bean
    CommandLineRunner cuentasDemo(CuentaRepository cuentas) {
        return args -> {
            if (cuentas.count() > 0) {
                return;
            }
            cuentas.save(new Cuenta("001000000001", "ANA TORRES", "A", 150000L, "ACTIVA", LocalDateTime.now()));
            cuentas.save(new Cuenta("001000000002", "BETO RUIZ", "C", 25075L, "ACTIVA", LocalDateTime.now()));
            cuentas.save(new Cuenta("001000000003", "CARLA DIAZ", "A", 0L, "ACTIVA", LocalDateTime.now()));
        };
    }
}
