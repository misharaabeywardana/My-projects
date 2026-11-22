package econest.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Reusable input checks used by every service. */
public final class Validator {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    /** 9-15 digits, optional leading +, spaces and dashes allowed. e.g. 0771234567, +94 77 123 4567 */
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9 -]{7,18}[0-9]$");
    private static final Pattern VEHICLE_NO = Pattern.compile("^[A-Za-z0-9 -]{3,20}$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_.]{3,50}$");

    private Validator() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isValidEmail(String value) {
        return value != null && EMAIL.matcher(value.trim()).matches();
    }

    public static boolean isValidPhone(String value) {
        if (value == null || !PHONE.matcher(value.trim()).matches()) {
            return false;
        }
        int digits = value.replaceAll("[^0-9]", "").length();
        return digits >= 9 && digits <= 15;
    }

    public static boolean isValidVehicleNumber(String value) {
        return value != null && VEHICLE_NO.matcher(value.trim()).matches();
    }

    public static boolean isValidUsername(String value) {
        return value != null && USERNAME.matcher(value).matches();
    }

    /** Parses a money amount like "1,250.50". Returns null when the text is not a valid amount. */
    public static BigDecimal parseMoney(String text) {
        if (isBlank(text)) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(text.trim().replace(",", ""));
            if (value.scale() > 2) {
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Parses a whole number. Returns null when the text is not a valid integer. */
    public static Integer parseInt(String text) {
        if (isBlank(text)) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
