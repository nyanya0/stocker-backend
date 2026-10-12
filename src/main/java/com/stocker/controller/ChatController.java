package com.stocker.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Simple rule-based "AI advisor" - free, no API key needed.
 * (You can later swap this for a real LLM API call.)
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Pattern GREETING = Pattern.compile("\\b(hello|hi|hey)\\b");

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        String message = body == null ? null : body.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("reply", "Please provide a message."));
        }
        return ResponseEntity.ok(Map.of("reply", reply(message.toLowerCase())));
    }

    private String reply(String m) {
        if (m.contains("market cap") || m.contains("apple")) {
            return "Market Cap (Market Capitalization) is the total value of all a company's shares of stock. "
                    + "It's the share price multiplied by the number of shares outstanding - bigger companies like Apple have huge market caps.";
        }
        if (m.contains("buy") || m.contains("sell") || m.contains("trade")) {
            return "To trade, open the Market page, search for a ticker symbol (like AAPL or TSLA), "
                    + "and use the Buy button. You can sell any holding from your Portfolio page. It's all paper trading with virtual money!";
        }
        if (GREETING.matcher(m).find()) {
            return "Hello there! I am your Stocker advisor. How can I help you navigate the markets today?";
        }
        if (m.contains("watchlist")) {
            return "Tap the star on any stock in the Market page to add it to your watchlist. "
                    + "Your watchlist shows up on your Dashboard so you can keep an eye on it.";
        }
        if (m.contains("profit") || m.contains("loss") || m.contains("p/l")) {
            return "Profit/Loss compares what your holdings are worth now with what you paid for them. "
                    + "If the current value is higher than your total investment, you're in profit.";
        }
        if (m.contains("portfolio") || m.contains("money") || m.contains("balance")) {
            return "Your portfolio tracks all your current stock holdings. You started with $100,000 in simulated cash. "
                    + "Try buying some stocks to see how it changes!";
        }
        if (m.contains("stock") || m.contains("invest") || m.contains("market")) {
            return "Investing in stocks carries risk, but historically the stock market has returned roughly 7-10% per year on average. "
                    + "Diversifying across different companies and sectors is a key way to manage risk.";
        }
        return "I'm a simple demo advisor - I can explain things like market cap, buying and selling, your portfolio and your watchlist. "
                + "Try asking me about one of those!";
    }
}
