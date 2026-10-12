package com.stocker.controller;

import com.stocker.dto.Dtos.TradeRequest;
import com.stocker.dto.Dtos.TransactionDto;
import com.stocker.model.User;
import com.stocker.security.AuthInterceptor;
import com.stocker.service.TradeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trade")
public class TradeController {

    private final TradeService trades;

    public TradeController(TradeService trades) {
        this.trades = trades;
    }

    @PostMapping("/buy")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionDto buy(@RequestAttribute(AuthInterceptor.CURRENT_USER) User current,
                              @RequestBody TradeRequest request) {
        return trades.buy(current.getId(), request);
    }

    @PostMapping("/sell")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionDto sell(@RequestAttribute(AuthInterceptor.CURRENT_USER) User current,
                               @RequestBody TradeRequest request) {
        return trades.sell(current.getId(), request);
    }

    @GetMapping("/history")
    public List<TransactionDto> history(@RequestAttribute(AuthInterceptor.CURRENT_USER) User current) {
        return trades.history(current.getId());
    }
}
