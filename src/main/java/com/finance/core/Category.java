package com.finance.core;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Category implements Serializable {
  private static final long serialVersionUID = 1L;

  private String name;
  private String description;

  public Category() {
    this("", "");
  }

  @JsonCreator
  public Category(
      @JsonProperty("name") String name, @JsonProperty("description") String description) {
    this.name = name;
    this.description = description;
  }

  public Category(String name) {
    this(name, "");
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  // 🔧 setter для имени
  public void setName(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Имя категории не может быть пустым");
    }
    this.name = name;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return name + (description.isEmpty() ? "" : " (" + description + ")");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Category category = (Category) o;
    return name.equalsIgnoreCase(category.name);
  }

  @Override
  public int hashCode() {
    return name.toLowerCase().hashCode();
  }
}
