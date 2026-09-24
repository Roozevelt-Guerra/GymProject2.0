package br.com.maqpro.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record EquipmentDto(
    Long id,
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 4000) String description,
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
    @NotBlank
        @Size(max = 2048)
        @Pattern(regexp = "https://[^\\s]+", message = "Informe uma URL HTTPS válida")
        String imageUrl,
    @NotBlank @Size(max = 60) String category,
    Boolean active) {}
