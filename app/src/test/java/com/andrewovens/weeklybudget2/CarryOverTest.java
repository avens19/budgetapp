package com.andrewovens.weeklybudget2;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CarryOverTest {

    private static List<Expense> spent(double... amounts) {
        List<Expense> week = new ArrayList<>();
        for (double amount : amounts) {
            Expense e = new Expense();
            e.Amount = amount;
            week.add(e);
        }
        return week;
    }

    @Test
    public void aSurplusCarriesAsANegativeExpense() {
        assertEquals(-124.5, CarryOver.amountToCarry(200, spent(50, 25.5)), 0);
    }

    /** The request that started this: a deficit has to follow the reader too. */
    @Test
    public void aDeficitCarriesAsAPositiveExpense() {
        assertEquals(25.25, CarryOver.amountToCarry(100, spent(80, 45.25)), 0);
    }

    @Test
    public void aCarryAlreadyInTheWeekCountsAsSpending() {
        // Opened with 30 carried in (-30); 150 spent leaves 80 of 200.
        assertEquals(-80, CarryOver.amountToCarry(200, spent(-30, 150)), 0);
    }

    @Test
    public void anEvenWeekCarriesNothing() {
        assertEquals(0, CarryOver.amountToCarry(100, spent(100)), 0);
        assertEquals(0, CarryOver.amountToCarry(100, spent(33.33, 66.67)), 0);
    }

    /** 0.1 + 0.2 is 0.30000000000000004, and a $0.00 carry is clutter. */
    @Test
    public void ignoresRoundingNoise() {
        assertEquals(0, CarryOver.amountToCarry(0.3, spent(0.1, 0.2)), 0);
        assertEquals(0, CarryOver.amountToCarry(100, spent(99.996)), 0);
    }

    @Test
    public void roundsToTheCent() {
        assertEquals(-6.67, CarryOver.amountToCarry(10, spent(3.333)), 0);
    }

    @Test
    public void anEmptyWeekCarriesTheWholeAllowance() {
        assertEquals(-120, CarryOver.amountToCarry(120, Arrays.<Expense>asList()), 0);
    }
}
