package com.stocker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stocker.model.Holding;
import com.stocker.model.Portfolio;
import com.stocker.model.Stock;
import com.stocker.model.StockTransaction;
import com.stocker.model.User;

import java.time.Instant;
import java.util.List;

/**
 * Request/response shapes. The JSON field names (like "_id", "stockId", "userId")
 * deliberately match what the React frontend already expects.
 */
public final class Dtos {

    private Dtos() {}

    // ---------- auth ----------
    public record RegisterRequest(String name, String email, String password) {}

    public record LoginRequest(String email, String password) {}

    public record UpdateProfileRequest(String name, String email, String password) {}

    public record AuthResponse(@JsonProperty("_id") Long id,
                               String name,
                               String email,
                               String role,
                               double walletBalance,
                               String token) {
        public static AuthResponse of(User u, String token) {
            return new AuthResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getWalletBalance(), token);
        }
    }

    // ---------- stocks ----------
    public record StockDto(@JsonProperty("_id") Long id,
                           String symbol,
                           String companyName,
                           String sector,
                           double currentPrice,
                           Double openPrice,
                           Double closePrice,
                           Long marketCap,
                           Long volume) {
        public static StockDto from(Stock s) {
            return new StockDto(s.getId(), s.getSymbol(), s.getCompanyName(), s.getSector(),
                    s.getCurrentPrice(), s.getOpenPrice(), s.getClosePrice(), s.getMarketCap(), s.getVolume());
        }
    }

    public record LivePrice(@JsonProperty("_id") Long id, String symbol, double currentPrice) {}

    public record CandleDto(Instant date, double open, double high, double low, double close, long volume) {}

    // ---------- trading ----------
    public record TradeRequest(Long stockId, Integer quantity) {}

    public record StockRef(@JsonProperty("_id") Long id, String symbol, String companyName) {}

    public record UserRef(@JsonProperty("_id") Long id, String name, String email) {}

    public record TransactionDto(@JsonProperty("_id") Long id,
                                 String type,
                                 int quantity,
                                 double price,
                                 double totalAmount,
                                 Instant createdAt,
                                 StockRef stockId,
                                 UserRef userId) {
        public static TransactionDto from(StockTransaction t) {
            Stock s = t.getStock();
            User u = t.getUser();
            return new TransactionDto(
                    t.getId(), t.getType(), t.getQuantity(), t.getPrice(), t.getTotalAmount(), t.getCreatedAt(),
                    new StockRef(s.getId(), s.getSymbol(), s.getCompanyName()),
                    u == null ? null : new UserRef(u.getId(), u.getName(), u.getEmail()));
        }
    }

    // ---------- portfolio ----------
    public record HoldingDto(@JsonProperty("_id") Long id,
                             StockDto stockId,
                             int quantity,
                             double averagePrice) {
        public static HoldingDto from(Holding h) {
            return new HoldingDto(h.getId(), StockDto.from(h.getStock()), h.getQuantity(), h.getAveragePrice());
        }
    }

    public record PortfolioDto(@JsonProperty("_id") Long id,
                               List<HoldingDto> holdings,
                               double totalInvestment,
                               double currentValue,
                               double totalProfitLoss) {
        public static PortfolioDto from(Portfolio p) {
            return new PortfolioDto(p.getId(),
                    p.getHoldings().stream().map(HoldingDto::from).toList(),
                    p.getTotalInvestment(), p.getCurrentValue(), p.getTotalProfitLoss());
        }
    }

    public record PortfolioSummary(double walletBalance,
                                   double totalInvestment,
                                   double currentValue,
                                   double totalProfitLoss,
                                   int holdingsCount) {}

    // ---------- admin ----------
    public record AdminUserDto(@JsonProperty("_id") Long id,
                               String name,
                               String email,
                               String role,
                               double walletBalance,
                               Instant createdAt) {
        public static AdminUserDto from(User u) {
            return new AdminUserDto(u.getId(), u.getName(), u.getEmail(), u.getRole(),
                    u.getWalletBalance(), u.getCreatedAt());
        }
    }

    public record AnalyticsDto(long totalUsers, long totalTrades) {}
}
