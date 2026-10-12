package com.stocker.service;

import com.stocker.dto.Dtos.StockDto;
import com.stocker.exception.ApiException;
import com.stocker.model.Stock;
import com.stocker.model.User;
import com.stocker.repository.StockRepository;
import com.stocker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class UserService {

    private final UserRepository users;
    private final StockRepository stocks;

    public UserService(UserRepository users, StockRepository stocks) {
        this.users = users;
        this.stocks = stocks;
    }

    @Transactional(readOnly = true)
    public List<StockDto> watchlist(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        return toDtos(user);
    }

    /** Adds the stock to the watchlist, or removes it if it's already there. */
    @Transactional
    public List<StockDto> toggleWatchlist(Long userId, Long stockId) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        Stock stock = stocks.findById(stockId).orElseThrow(() -> ApiException.notFound("Stock not found"));

        boolean removed = user.getWatchlist().removeIf(s -> s.getId().equals(stockId));
        if (!removed) {
            user.getWatchlist().add(stock);
        }
        users.save(user);
        return toDtos(user);
    }

    private List<StockDto> toDtos(User user) {
        return user.getWatchlist().stream()
                .sorted(Comparator.comparing(Stock::getSymbol))
                .map(StockDto::from)
                .toList();
    }
}
