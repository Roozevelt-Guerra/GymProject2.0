package br.com.maqpro.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
public class ConfigController {
  private final String number;

  public ConfigController(@Value("${app.whatsapp}") String number) {
    if (!number.isEmpty() && !number.matches("[1-9][0-9]{9,14}"))
      throw new IllegalArgumentException("WHATSAPP_NUMBER: use país, DDD e número, apenas dígitos");
    this.number = number;
  }

  @GetMapping("/api/config")
  public Map<String, String> config() {
    return Map.of("whatsappNumber", number);
  }
}
