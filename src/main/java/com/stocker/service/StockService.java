package com.stocker.service;

import com.stocker.dto.Dtos.CandleDto;
import com.stocker.dto.Dtos.LivePrice;
import com.stocker.exception.ApiException;
import com.stocker.model.Stock;
import com.stocker.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final StockRepository stocks;
    private final MarketDataService market;

    public StockService(StockRepository stocks, MarketDataService market) {
        this.stocks = stocks;
        this.market = market;
    }

    /** Search by symbol/company name. Unknown ticker symbols are looked up on Yahoo and saved. */
    public List<Stock> search(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return stocks.findAll(Sort.by("symbol"));
        }

        List<Stock> found = stocks.search(kw);
        if (found.isEmpty() && MarketDataService.looksLikeSymbol(kw)) {
            Optional<Stock> imported = importFromYahoo(kw.toUpperCase());
            if (imported.isPresent()) {
                return List.of(imported.get());
            }
        }
        return found;
    }

    private Optional<Stock> importFromYahoo(String symbol) {
        try {
            Optional<MarketDataService.Quote> quote = market.fetchQuote(symbol);
            if (quote.isEmpty()) {
                return Optional.empty();
            }
            MarketDataService.Quote q = quote.get();
            Optional<Stock> existing = stocks.findBySymbolIgnoreCase(q.symbol());
            if (existing.isPresent()) {
                return existing;
            }
            Stock s = new Stock(q.symbol(), q.name(), "Other", Money.round2(q.price()), 0L, q.volume());
            return Optional.of(stocks.save(s));
        } catch (Exception e) {
            log.warn("Could not import {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    public Stock getById(Long id) {
        return stocks.findById(id).orElseThrow(() -> ApiException.notFound("Stock not found"));
    }

    /** Returns the stock with its price refreshed from Yahoo (when Yahoo is reachable). */
    public Stock getByIdFresh(Long id) {
        Stock stock = getById(id);
        market.fetchQuote(stock.getSymbol()).ifPresent(q -> {
            stock.setCurrentPrice(Money.round2(q.price()));
            if (q.volume() > 0) {
                stock.setVolume(q.volume());
            }
            stocks.save(stock);
        });
        return stock;
    }

    /** 30-day price history. Real data from Yahoo, or a simulated series if Yahoo is unavailable. */
    public List<CandleDto> history(Long id) {
        Stock stock = getById(id);
        List<CandleDto> real = market.fetchHistory(stock.getSymbol());
        if (!real.isEmpty()) {
            return real;
        }
        return simulatedHistory(stock);
    }

    private List<CandleDto> simulatedHistory(Stock stock) {
        Random rnd = new Random(stock.getSymbol().hashCode() * 31L + LocalDate.now(ZoneOffset.UTC).toEpochDay());
        List<CandleDto> out = new ArrayList<>();
        double price = stock.getCurrentPrice();
        for (int i = 0; i < 30; i++) {
            double close = Money.round2(price);
            out.add(new CandleDto(
                    LocalDate.now(ZoneOffset.UTC).minusDays(i).atStartOfDay().toInstant(ZoneOffset.UTC),
                    close, close, close, close, 0));
            // step one day back with a random daily move of up to +/-2%
            price = price / (1 + (rnd.nextDouble() - 0.5) * 0.04);
        }
        Collections.reverse(out);
        return out;
    }

    /** Simulated "live" ticks: stored price +/- 0.1%. */
    public List<LivePrice> livePrices() {
        Random rnd = new Random();
        List<LivePrice> out = new ArrayList<>();
        for (Stock s : stocks.findAll()) {
            double wiggle = 1 + (rnd.nextDouble() * 0.002 - 0.001);
            out.add(new LivePrice(s.getId(), s.getSymbol(), Money.round2(s.getCurrentPrice() * wiggle)));
        }
        return out;
    }

    /** Adds the starter stocks that are missing (never deletes anything). */
    public List<Stock> seedDefaults() {
        List<Stock> defaults = List.of(
                new Stock("AAPL", "Apple Inc.", "Technology", 175.50, 2_800_000_000_000L, 55_000_000L),
                new Stock("GOOGL", "Alphabet Inc.", "Technology", 135.20, 1_700_000_000_000L, 25_000_000L),
                new Stock("MSFT", "Microsoft Corp.", "Technology", 330.10, 2_500_000_000_000L, 22_000_000L),
                new Stock("TSLA", "Tesla Inc.", "Automotive", 245.80, 800_000_000_000L, 110_000_000L),
                new Stock("AMZN", "Amazon.com Inc.", "Retail", 130.00, 1_300_000_000_000L, 45_000_000L),
                new Stock("NVDA", "NVIDIA Corp.", "Technology", 450.00, 1_100_000_000_000L, 40_000_000L),
                new Stock("META", "Meta Platforms Inc.", "Technology", 300.00, 800_000_000_000L, 18_000_000L),
                new Stock("NFLX", "Netflix Inc.", "Entertainment", 420.00, 190_000_000_000L, 4_000_000L));

        for (Stock s : defaults) {
            if (stocks.findBySymbolIgnoreCase(s.getSymbol()).isEmpty()) {
                stocks.save(s);
            }
        }
        return stocks.findAll(Sort.by("symbol"));
    }
}
