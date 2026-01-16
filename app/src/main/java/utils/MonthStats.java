package utils;

public class MonthStats {
    public String month;          // yyyy-MM
    public double savedMoney;
    public long savedTimeMinutes;
    public double maxSpending;
    public double totalSpent;

    public MonthStats(String month,
                      double savedMoney,
                      long savedTimeMinutes,
                      double maxSpending,
                      double totalSpent) {
        this.month = month;
        this.savedMoney = savedMoney;
        this.savedTimeMinutes = savedTimeMinutes;
        this.maxSpending = maxSpending;
        this.totalSpent = totalSpent;
    }
}

