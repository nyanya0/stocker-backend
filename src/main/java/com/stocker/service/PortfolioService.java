package com.stocker.service;

import com.stocker.dto.Dtos.PortfolioDto;
import com.stocker.dto.Dtos.PortfolioSummary;
import com.stocker.exception.ApiException;
import com.stocker.model.Holding;
import com.stocker.model.Portfolio;
import com.stocker.model.User;
import com.stocker.repository.PortfolioRepository;
import com.stocker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolios;
    private final UserRepository users;

    public PortfolioService(PortfolioRepository portfolios, UserRepository users) {
        this.portfolios = portfolios;
        this.users = users;
    }

    @Transactional
    public PortfolioDto get(Long userId) {
        Portfolio portfolio = loadOrCreate(userId);
        recalculate(portfolio);
        portfolios.save(portfolio);
        return PortfolioDto.from(portfolio);
    }

    @Transactional
    public PortfolioSummary summary(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        Portfolio portfolio = loadOrCreate(userId);
        recalculate(portfolio);
        portfolios.save(portfolio);
        return new PortfolioSummary(
                user.getWalletBalance(),
                portfolio.getTotalInvestment(),
                portfolio.getCurrentValue(),
                portfolio.getTotalProfitLoss(),
                portfolio.getHoldings().size());
    }

    private Portfolio loadOrCreate(Long userId) {
        return portfolios.findByUserId(userId).orElseGet(() -> {
            User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
            Portfolio p = new Portfolio();
            p.setUser(user);
            return portfolios.save(p);
        });
    }

    private void recalculate(Portfolio portfolio) {
        double value = 0;
        for (Holding h : portfolio.getHoldings()) {
            value += h.getQuantity() * h.getStock().getCurrentPrice();
        }
        portfolio.setCurrentValue(Money.round2(value));
        portfolio.setTotalProfitLoss(Money.round2(value - portfolio.getTotalInvestment()));
    }
}
