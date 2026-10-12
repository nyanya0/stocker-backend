package com.stocker.controller;

import com.stocker.dto.Dtos.StockDto;
import com.stocker.model.User;
import com.stocker.security.AuthInterceptor;
import com.stocker.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/watchlist")
    public List<StockDto> watchlist(@RequestAttribute(AuthInterceptor.CURRENT_USER) User current) {
        return users.watchlist(current.getId());
    }

    @PostMapping("/watchlist/{stockId}")
    public List<StockDto> toggle(@RequestAttribute(AuthInterceptor.CURRENT_USER) User current,
                                 @PathVariable Long stockId) {
        return users.toggleWatchlist(current.getId(), stockId);
    }
}
