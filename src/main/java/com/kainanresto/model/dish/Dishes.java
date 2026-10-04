package com.kainanresto.model.dish;

import java.math.BigDecimal;

public record Dishes(
        long id,
        String name,
        String category,
        BigDecimal price,
        String imageUrl,
        boolean available,
        String description,
        int quantity
) {}