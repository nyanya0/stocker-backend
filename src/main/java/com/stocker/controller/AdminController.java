package com.stocker.controller;

import com.stocker.dto.Dtos.AdminUserDto;
import com.stocker.dto.Dtos.AnalyticsDto;
import com.stocker.dto.Dtos.TransactionDto;
import com.stocker.model.User;
import com.stocker.security.AuthInterceptor;
import com.stocker.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Every route under /api/admin is admin-only (enforced in AuthInterceptor). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/users")
    public List<AdminUserDto> users() {
        return admin.allUsers();
    }

    @DeleteMapping("/users/{id}")
    public Map<String, String> deleteUser(@RequestAttribute(AuthInterceptor.CURRENT_USER) User current,
                                          @PathVariable Long id) {
        admin.deleteUser(id, current.getId());
        return Map.of("message", "User removed");
    }

    @GetMapping("/transactions")
    public List<TransactionDto> transactions() {
        return admin.allTransactions();
    }

    @GetMapping("/analytics")
    public AnalyticsDto analytics() {
        return admin.analytics();
    }
}
