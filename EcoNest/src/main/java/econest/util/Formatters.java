package econest.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** One place for how money and dates are shown on screen. */
public final class Formatters {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter FRIENDLY = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy 'at' hh:mm a");

    private Formatters() {
    }

    /** e.g. 1250.5 -> "Rs. 1,250.50" */
    public static String money(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return "Rs. " + df.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    /** e.g. 1250.5 -> "1250.50" (for CSV and edit fields). */
    public static String plainMoney(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    public static String dateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME.format(dateTime);
    }

    public static String friendly(LocalDateTime dateTime) {
        return dateTime == null ? "" : FRIENDLY.format(dateTime);
    }
}
