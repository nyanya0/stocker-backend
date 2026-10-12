package com.stocker.service;

import com.stocker.dto.Dtos.AdminUserDto;
import com.stocker.dto.Dtos.AnalyticsDto;
import com.stocker.dto.Dtos.TransactionDto;
import com.stocker.exception.ApiException;
import com.stocker.model.User;
import com.stocker.repository.PortfolioRepository;
import com.stocker.repository.StockTransactionRepository;
import com.stocker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository users;
    private final PortfolioRepository portfolios;
    private final StockTransactionRepository transactions;

    public AdminService(UserRepository users, PortfolioRepository portfolios,
                        StockTransactionRepository transactions) {
        this.users = users;
        this.portfolios = portfolios;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> allUsers() {
        return users.findAll().stream().map(AdminUserDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> allTransactions() {
        return transactions.findAllWithDetails().stream().map(TransactionDto::from).toList();
    }

    @Transactional(readOnly = true)
    public AnalyticsDto analytics() {
        return new AnalyticsDto(users.count(), transactions.count());
    }

    @Transactional
    public void deleteUser(Long id, Long requestingAdminId) {
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        if ("ADMIN".equals(user.getRole()) || user.getId().equals(requestingAdminId)) {
            throw ApiException.badRequest("Admin accounts can't be deleted");
        }
        // keep the trade history, but unlink it from the deleted user
        transactions.detachUser(id);
        portfolios.findByUserId(id).ifPresent(portfolios::delete);
        users.delete(user); // also clears the user's watchlist rows
    }
}
