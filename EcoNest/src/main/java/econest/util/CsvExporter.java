package econest.util;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.table.TableModel;

/** Writes table data to a CSV file that opens in Excel. */
public final class CsvExporter {

    private CsvExporter() {
    }

    public static void export(TableModel model, Path file) throws IOException {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write('﻿'); // lets Excel detect UTF-8
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (c > 0) {
                    w.write(',');
                }
                w.write(escape(model.getColumnName(c)));
            }
            w.write("\r\n");
            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (c > 0) {
                        w.write(',');
                    }
                    Object value = model.getValueAt(r, c);
                    w.write(escape(value == null ? "" : value.toString()));
                }
                w.write("\r\n");
            }
        }
    }

    public static void exportLines(List<String[]> rows, Path file) throws IOException {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write('﻿');
            for (String[] row : rows) {
                for (int c = 0; c < row.length; c++) {
                    if (c > 0) {
                        w.write(',');
                    }
                    w.write(escape(row[c] == null ? "" : row[c]));
                }
                w.write("\r\n");
            }
        }
    }

    static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
