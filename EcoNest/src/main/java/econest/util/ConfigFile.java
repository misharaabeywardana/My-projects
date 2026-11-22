package econest.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Reads and writes small settings files in the "config" folder next to the
 * project (for example config/db.properties). Passwords stay out of the
 * Java source code, as the team rules require.
 */
public class ConfigFile {

    /** Folder can be changed with -Deconest.config=/some/folder (used by tests). */
    private static final String CONFIG_DIR = System.getProperty("econest.config", "config");

    private final Path path;
    private final Properties defaults;

    public ConfigFile(String fileName, Properties defaults) {
        this.path = Paths.get(CONFIG_DIR, fileName);
        this.defaults = defaults;
    }

    public Path getPath() {
        return path.toAbsolutePath();
    }

    /** Loads the file; if it does not exist it is created with the defaults. */
    public Properties load() {
        Properties props = new Properties();
        props.putAll(defaults);
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Could not read " + path + ": " + e.getMessage());
            }
        } else {
            try {
                save(props, "Created automatically with default values");
            } catch (IOException e) {
                System.err.println("Could not create " + path + ": " + e.getMessage());
            }
        }
        return props;
    }

    public void save(Properties props, String comment) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (OutputStream out = Files.newOutputStream(path)) {
            props.store(out, comment);
        }
    }
}
