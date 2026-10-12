package com.stocker.controller;

import com.stocker.dto.Dtos.CandleDto;
import com.stocker.dto.Dtos.LivePrice;
import com.stocker.dto.Dtos.StockDto;
import com.stocker.service.StockService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stocks;

    public StockController(StockService stocks) {
        this.stocks = stocks;
    }

    @GetMapping
    public List<StockDto> list(@RequestParam(required = false) String keyword) {
        return stocks.search(keyword).stream().map(StockDto::from).toList();
    }

    /** Simulated live prices - the Market page polls this every few seconds. */
    @GetMapping("/live")
    public List<LivePrice> live() {
        return stocks.livePrices();
    }

    @GetMapping("/{id}")
    public StockDto get(@PathVariable Long id) {
        return StockDto.from(stocks.getByIdFresh(id));
    }

    @GetMapping("/{id}/chart")
    public List<CandleDto> chart(@PathVariable Long id) {
        return stocks.history(id);
    }

    /** Admin only (see AuthInterceptor): adds any missing starter stocks. */
    @PostMapping("/seed")
    @ResponseStatus(HttpStatus.CREATED)
    public List<StockDto> seed() {
        return stocks.seedDefaults().stream().map(StockDto::from).toList();
    }
}
