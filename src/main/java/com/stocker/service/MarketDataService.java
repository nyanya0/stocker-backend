package com.stocker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.stocker.dto.Dtos.CandleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Fetches real prices from Yahoo Finance's public chart endpoint (no API key, free).
 * It's an unofficial endpoint, so every call fails soft: if Yahoo is slow, blocks the
 * server's IP, or the symbol doesn't exist, we just return empty and the app keeps working
 * with the prices already stored in the database.
 */
@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    private static final String CHART_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?range=1mo&interval=1d";

    private static final Pattern SYMBOL = Pattern.compile("^[A-Za-z][A-Za-z0-9.\\-]{0,9}$");

    public record Quote(String symbol, String name, double price, long volume) {}

    private final RestClient client;

    public MarketDataService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(4000);
        factory.setReadTimeout(6000);
        this.client = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; StockerApp/1.0)")
                .build();
    }

    public static boolean looksLikeSymbol(String text) {
        return text != null && SYMBOL.matcher(text.trim()).matches();
    }

    public Optional<Quote> fetchQuote(String symbol) {
        JsonNode result = fetchChartResult(symbol);
        if (result == null) {
            return Optional.empty();
        }
        JsonNode meta = result.path("meta");
        JsonNode price = meta.path("regularMarketPrice");
        if (!price.isNumber()) {
            return Optional.empty();
        }
        String sym = meta.path("symbol").asText(symbol).toUpperCase();
        String name = meta.path("longName").asText(meta.path("shortName").asText(sym));
        long volume = meta.path("regularMarketVolume").asLong(0);
        return Optional.of(new Quote(sym, name, price.asDouble(), volume));
    }

    /** Last ~30 days of daily candles, oldest first. Empty list if unavailable. */
    public List<CandleDto> fetchHistory(String symbol) {
        List<CandleDto> out = new ArrayList<>();
        JsonNode result = fetchChartResult(symbol);
        if (result == null) {
            return out;
        }
        JsonNode timestamps = result.path("timestamp");
        JsonNode quote = result.path("indicators").path("quote").path(0);
        JsonNode closes = quote.path("close");
        JsonNode opens = quote.path("open");
        JsonNode highs = quote.path("high");
        JsonNode lows = quote.path("low");
        JsonNode volumes = quote.path("volume");

        for (int i = 0; i < timestamps.size(); i++) {
            JsonNode close = closes.path(i);
            if (!close.isNumber()) {
                continue; // market holidays come back as null
            }
            double c = close.asDouble();
            double o = opens.path(i).isNumber() ? opens.path(i).asDouble() : c;
            double h = highs.path(i).isNumber() ? highs.path(i).asDouble() : c;
            double l = lows.path(i).isNumber() ? lows.path(i).asDouble() : c;
            long v = volumes.path(i).asLong(0);
            out.add(new CandleDto(Instant.ofEpochSecond(timestamps.path(i).asLong()), o, h, l, c, v));
        }
        return out;
    }

    private JsonNode fetchChartResult(String symbol) {
        if (!looksLikeSymbol(symbol)) {
            return null;
        }
        try {
            JsonNode root = client.get().uri(CHART_URL, symbol.trim()).retrieve().body(JsonNode.class);
            if (root == null) {
                return null;
            }
            JsonNode result = root.path("chart").path("result");
            return (result.isArray() && result.size() > 0) ? result.get(0) : null;
        } catch (Exception e) {
            log.warn("Yahoo Finance lookup failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }
}
