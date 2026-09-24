package br.com.maqpro.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "equipment")
public class Equipment {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false, length = 120)
  private String name;
  @Column(nullable = false, length = 4000)
  private String description;
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal price;
  @Column(name = "image_url", nullable = false, length = 2048)
  private String imageUrl;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;
  @Column(nullable = false)
  private boolean active = true;

  public Equipment() {}

  public void update(String name, String description, BigDecimal price,
      String imageUrl, Category category, Boolean active) {
    this.name = name;
    this.description = description;
    this.price = price;
    this.imageUrl = imageUrl;
    this.category = category;
    if (active != null) this.active = active;
  }

  public Long getId() { return id; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public BigDecimal getPrice() { return price; }
  public String getImageUrl() { return imageUrl; }
  public Category getCategory() { return category; }
  public boolean isActive() { return active; }
}
