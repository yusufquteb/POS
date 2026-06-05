package com.pos.system.domain.repository;

import com.pos.system.domain.model.Expense;
import java.util.List;

public interface ExpenseRepository {
    List<Expense> getAllExpenses();
    List<Expense> getExpensesByDateRange(String fromDate, String toDate);
    List<Expense> getExpensesByCategory(String category);
    Expense getExpenseById(long id);
    long insertExpense(Expense expense);
    int updateExpense(Expense expense);
    int deleteExpense(long id);
    double getTotalForPeriod(String fromDate, String toDate);
}
