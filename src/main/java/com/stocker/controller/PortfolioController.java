package com.stocker.controller;

import com.stocker.dto.Dtos.PortfolioDto;
import com.stocker.dto.Dtos.PortfolioSummary;
import com.stocker.model.User;
import com.stocker.security.AuthInterceptor;
import com.stocker.service.PortfolioService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolios;

    public PortfolioController(PortfolioService portfolios) {
        this.portfolios = portfolios;
    }

    @GetMapping
    public PortfolioDto get(@RequestAttribute(AuthInterceptor.CURRENT_USER) User current) {
        return portfolios.get(current.getId());
    }

    @GetMapping("/summary")
    public PortfolioSummary summary(@RequestAttribute(AuthInterceptor.CURRENT_USER) User current) {
        return portfolios.summary(current.getId());
    }
}
