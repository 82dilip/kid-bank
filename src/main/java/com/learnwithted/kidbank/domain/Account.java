package com.learnwithted.kidbank.domain;

import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class Account {
  public static final double INTEREST_RATE_PER_MONTH = (0.025 / 12);
  private ImmutableSet<Transaction> transactions;

  private final TransactionRepository transactionRepository;
  private final Clock clock;

  @Autowired
  public Account(TransactionRepository transactionRepository) {
    this.transactionRepository = transactionRepository;
    transactions = ImmutableSet.<Transaction>builder()
                       .addAll(transactionRepository.findAll())
                       .build();
    clock = Clock.systemDefaultZone();
  }

  public Account(TransactionRepository transactionRepository, Clock clock) {
    this.transactionRepository = transactionRepository;
    this.clock = clock;
    this.transactions = ImmutableSet.of();
  }

  public int balance() {
    creditInterestAsNeeded();
    return transactions.stream()
                       .mapToInt(Transaction::signedAmount)
                       .sum();
  }

  public void creditInterestAsNeeded() {
    if (shouldCreditInterest()) {
      int currentBalance = transactions.stream()
                                       .mapToInt(Transaction::signedAmount)
                                       .sum();
      int interestCredit = calculateInterest(currentBalance);
      deposit(LocalDateTime.now(clock), interestCredit, "Interest Credit");
    }
  }

  public boolean shouldCreditInterest() {
    // is it the first of the month && have we NOT credited interest yet?
    return LocalDateTime.now(clock).getDayOfMonth() == 1;
  }

  // Calculates interest credit rounding up
  public int calculateInterest(int currentBalance) {
    return (int) (currentBalance * INTEREST_RATE_PER_MONTH + 0.5);
  }

  public void deposit(LocalDateTime transactionDateTime, int amount, String source) {
    Transaction deposit = Transaction.createDeposit(transactionDateTime, amount, source);
    addNewTransaction(deposit);
  }

  public void spend(LocalDateTime transactionDateTime, int amount, String description) {
    Transaction spend = Transaction.createSpend(transactionDateTime, amount, description);
    addNewTransaction(spend);
  }

  private void addNewTransaction(Transaction transaction) {
    Transaction savedTransaction = transactionRepository.save(transaction);
    transactions = ImmutableSet.<Transaction>builder()
                       .addAll(transactions)
                       .add(savedTransaction)
                       .build();
  }

  public ImmutableSet<Transaction> transactions() {
    return transactions;
  }

  public void load(List<Transaction> transactionsToLoad) {
    transactionRepository.saveAll(transactionsToLoad);
    transactions = ImmutableSet.<Transaction>builder()
                       .addAll(transactions)
                       .addAll(transactionsToLoad)
                       .build();
  }
}
