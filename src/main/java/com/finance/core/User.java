package com.finance.core;

import java.io.Serializable;
import java.util.Objects;

public class User implements Serializable {
  private static final long serialVersionUID = 1L;

  private String login;
  private String password;
  private Wallet wallet;

  public User() {
    this.wallet = new Wallet();
  }

  public User(String login, String password, Wallet wallet) {
    this.login = login;
    this.password = password;
    this.wallet = wallet != null ? wallet : new Wallet();
  }

  public User(String login, String password) {
    this.login = login;
    this.password = password;
    this.wallet = new Wallet();
  }

  public String getLogin() {
    return login;
  }

  public String getPassword() {
    return password;
  }

  public Wallet getWallet() {
    return wallet;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setWallet(Wallet wallet) {
    this.wallet = wallet;
  }

  public boolean authenticate(String password) {
    return this.password.equals(password);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return login.equals(user.login);
  }

  @Override
  public int hashCode() {
    return Objects.hash(login);
  }

  @Override
  public String toString() {
    return "User{" + "login='" + login + '\'' + ", wallet=" + wallet + '}';
  }
}
