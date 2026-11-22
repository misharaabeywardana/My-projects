package econest.model;

import java.math.BigDecimal;
import java.time.LocalDate;


public class DailyRevenue {

    private final LocalDate day;
    private final BigDecimal revenue;

    public DailyRevenue(LocalDate day, BigDecimal revenue) {
        this.day = day;
        this.revenue = revenue;
    }

    public LocalDate getDay() {
        return day;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}
