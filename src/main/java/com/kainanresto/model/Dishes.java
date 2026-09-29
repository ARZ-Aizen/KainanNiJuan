package com.kainanresto.model;

import java.math.BigDecimal;

/**
 * UI-facing dish model for the Menu Management page.
 * Populated by the server/database layer. No hard-coded values.
 */
public record Dishes(
        long id,
        String name,
        String category,
        BigDecimal price,
        String imageUrl,
        boolean available
) {}