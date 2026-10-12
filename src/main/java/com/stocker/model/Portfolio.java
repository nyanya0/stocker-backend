package com.stocker.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "portfolios")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Holding> holdings = new ArrayList<>();

    private double totalInvestment = 0;
    private double currentValue = 0;
    private double totalProfitLoss = 0;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public List<Holding> getHoldings() { return holdings; }
    public double getTotalInvestment() { return totalInvestment; }
    public void setTotalInvestment(double totalInvestment) { this.totalInvestment = totalInvestment; }
    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }
    public double getTotalProfitLoss() { return totalProfitLoss; }
    public void setTotalProfitLoss(double totalProfitLoss) { this.totalProfitLoss = totalProfitLoss; }
}
