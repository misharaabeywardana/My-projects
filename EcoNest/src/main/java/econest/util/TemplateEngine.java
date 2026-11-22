package econest.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Replaces {placeholders} in e-mail templates with real values. */
public final class TemplateEngine {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z]+)\\}");

    private TemplateEngine() {
    }

    /** Unknown placeholders are left unchanged so mistakes are easy to spot. */
    public static String render(String template, Map<String, String> values) {
        if (template == null) {
            return "";
        }
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String replacement = values.containsKey(key) ? values.get(key) : m.group(0);
            m.appendReplacement(out, Matcher.quoteReplacement(replacement == null ? "" : replacement));
        }
        m.appendTail(out);
        return out.toString();
    }
}
