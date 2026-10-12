package com.stocker.config;

import com.stocker.model.User;
import com.stocker.repository.StockRepository;
import com.stocker.repository.UserRepository;
import com.stocker.service.AuthService;
import com.stocker.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Runs once on startup:
 *  1. adds starter stocks if the stocks table is empty
 *  2. creates an admin account if ADMIN_EMAIL + ADMIN_PASSWORD are set
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final StockRepository stocks;
    private final StockService stockService;
    private final UserRepository users;
    private final AuthService authService;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(StockRepository stocks, StockService stockService, UserRepository users,
                      AuthService authService,
                      @Value("${app.admin.email:}") String adminEmail,
                      @Value("${app.admin.password:}") String adminPassword) {
        this.stocks = stocks;
        this.stockService = stockService;
        this.users = users;
        this.authService = authService;
        this.adminEmail = adminEmail == null ? "" : adminEmail.trim().toLowerCase();
        this.adminPassword = adminPassword == null ? "" : adminPassword;
    }

    @Override
    public void run(String... args) {
        if (stocks.count() == 0) {
            stockService.seedDefaults();
            log.info("Seeded starter stocks");
        }

        if (!adminEmail.isEmpty() && adminPassword.length() >= 6) {
            Optional<User> existing = users.findByEmail(adminEmail);
            if (existing.isEmpty()) {
                authService.createUser("Admin", adminEmail, adminPassword, "ADMIN");
                log.info("Created admin account {}", adminEmail);
            } else if (!"ADMIN".equals(existing.get().getRole())) {
                User u = existing.get();
                u.setRole("ADMIN");
                users.save(u);
                log.info("Promoted {} to ADMIN", adminEmail);
            }
        }
    }
}
