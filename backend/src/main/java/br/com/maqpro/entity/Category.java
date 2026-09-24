package br.com.maqpro.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "category")
public class Category {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 60)
  private String name;

  protected Category() {}

  public Long getId() { return id; }
  public String getName() { return name; }
}
