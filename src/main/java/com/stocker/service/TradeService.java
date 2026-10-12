package com.stocker.service;

import com.stocker.dto.Dtos.TradeRequest;
import com.stocker.dto.Dtos.TransactionDto;
import com.stocker.exception.ApiException;
import com.stocker.model.Holding;
import com.stocker.model.Portfolio;
import com.stocker.model.Stock;
import com.stocker.model.StockTransaction;
import com.stocker.model.User;
import com.stocker.repository.PortfolioRepository;
import com.stocker.repository.StockRepository;
import com.stocker.repository.StockTransactionRepository;
import com.stocker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TradeService {

    private final UserRepository users;
    private final StockRepository stocks;
    private final PortfolioRepository portfolios;
    private final StockTransactionRepository transactions;

    public TradeService(UserRepository users, StockRepository stocks,
                        PortfolioRepository portfolios, StockTransactionRepository transactions) {
        this.users = users;
        this.stocks = stocks;
        this.portfolios = portfolios;
        this.transactions = transactions;
    }

    @Transactional
    public TransactionDto buy(Long userId, TradeRequest req) {
        int quantity = validQuantity(req);
        Stock stock = findStock(req);

        // Lock the user row so concurrent trades can't overspend
        User user = users.findByIdForUpdate(userId).orElseThrow(() -> ApiException.notFound("User not found"));

        double price = stock.getCurrentPrice();
        double total = Money.round2(price * quantity);
        if (user.getWalletBalance() < total) {
            throw ApiException.badRequest("Insufficient wallet balance");
        }
        user.setWalletBalance(Money.round2(user.getWalletBalance() - total));

        Portfolio portfolio = portfolioFor(user);
        Optional<Holding> existing = findHolding(portfolio, stock.getId());
        if (existing.isPresent()) {
            Holding h = existing.get();
            int newQty = h.getQuantity() + quantity;
            double newAvg = ((h.getAveragePrice() * h.getQuantity()) + total) / newQty;
            h.setQuantity(newQty);
            h.setAveragePrice(Money.round2(newAvg));
        } else {
            Holding h = new Holding();
            h.setPortfolio(portfolio);
            h.setStock(stock);
            h.setQuantity(quantity);
            h.setAveragePrice(price);
            portfolio.getHoldings().add(h);
        }
        portfolio.setTotalInvestment(Money.round2(portfolio.getTotalInvestment() + total));
        portfolios.save(portfolio);
        users.save(user);

        return TransactionDto.from(record(user, stock, "BUY", quantity, price, total));
    }

    @Transactional
    public TransactionDto sell(Long userId, TradeRequest req) {
        int quantity = validQuantity(req);
        Stock stock = findStock(req);

        User user = users.findByIdForUpdate(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        Portfolio portfolio = portfolioFor(user);

        Holding holding = findHolding(portfolio, stock.getId()).orElse(null);
        if (holding == null || holding.getQuantity() < quantity) {
            throw ApiException.badRequest("Insufficient stock quantity in portfolio");
        }

        double price = stock.getCurrentPrice();
        double total = Money.round2(price * quantity);

        user.setWalletBalance(Money.round2(user.getWalletBalance() + total));

        double costBasisSold = holding.getAveragePrice() * quantity;
        holding.setQuantity(holding.getQuantity() - quantity);
        portfolio.setTotalInvestment(Money.round2(Math.max(0, portfolio.getTotalInvestment() - costBasisSold)));
        if (holding.getQuantity() == 0) {
            portfolio.getHoldings().remove(holding); // orphanRemoval deletes the row
        }

        portfolios.save(portfolio);
        users.save(user);

        return TransactionDto.from(record(user, stock, "SELL", quantity, price, total));
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> history(Long userId) {
        return transactions.findHistoryForUser(userId).stream().map(TransactionDto::from).toList();
    }

    // ---------- helpers ----------

    private StockTransaction record(User user, Stock stock, String type, int qty, double price, double total) {
        StockTransaction t = new StockTransaction();
        t.setUser(user);
        t.setStock(stock);
        t.setType(type);
        t.setQuantity(qty);
        t.setPrice(price);
        t.setTotalAmount(total);
        return transactions.save(t);
    }

    private int validQuantity(TradeRequest req) {
        if (req == null || req.quantity() == null || req.quantity() < 1) {
            throw ApiException.badRequest("Quantity must be at least 1");
        }
        return req.quantity();
    }

    private Stock findStock(TradeRequest req) {
        if (req.stockId() == null) {
            throw ApiException.badRequest("stockId is required");
        }
        return stocks.findById(req.stockId()).orElseThrow(() -> ApiException.notFound("Stock not found"));
    }

    private Portfolio portfolioFor(User user) {
        return portfolios.findByUserId(user.getId()).orElseGet(() -> {
            Portfolio p = new Portfolio();
            p.setUser(user);
            return portfolios.save(p);
        });
    }

    private Optional<Holding> findHolding(Portfolio portfolio, Long stockId) {
        return portfolio.getHoldings().stream()
                .filter(h -> h.getStock().getId().equals(stockId))
                .findFirst();
    }
}
