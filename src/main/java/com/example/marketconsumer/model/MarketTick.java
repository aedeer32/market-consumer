package com.example.marketconsumer.model;

import java.time.Instant;

public record MarketTick(Instant ts, String symbol, double price) {}
