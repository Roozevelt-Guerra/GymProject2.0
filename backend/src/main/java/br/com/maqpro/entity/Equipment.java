package br.com.maqpro.entity;

import java.math.BigDecimal;

/** Immutable catalog item stored in the local JSON file. */
public record Equipment(
    Long id, String name, String description, BigDecimal price, String imageUrl, String category) {}
