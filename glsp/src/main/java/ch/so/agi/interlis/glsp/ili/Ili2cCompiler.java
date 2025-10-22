package ch.so.agi.interlis.glsp.ili;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.logging.StdListener;
import ch.interlis.ili2c.Ili2cSettings;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.config.FileEntry;
import ch.interlis.ili2c.config.FileEntryKind;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iox_j.logging.FileLogger;

/**
 * Helper that wraps the ili2c compilation and exposes the result for the GLSP
 * diagram builder.
 * <p>
 * The implementation is based on the VS Code language server utility and keeps
 * the locking logic so concurrent compilations are handled gracefully.
 */
@Singleton
public class Ili2cCompiler {

    private static final Logger LOGGER = LogManager.getLogger(Ili2cCompiler.class);
    private static final ReentrantLock COMPILE_LOCK = new ReentrantLock();

    /** Outcome of a compilation run (transfer description plus log output). */
    public record CompilationOutcome(TransferDescription transferDescription, String logText, List<Message> messages) {
    }

    /** Structured message reported by ili2c. */
    public record Message(Severity severity, String fileUriOrPath, int line, int column, String text) {
        public enum Severity {
            ERROR, WARNING, INFO
        }
    }

    /** Optional repositories and compilation flags. */
    public record CompilerSettings(String modelRepositories) {
    }

    /** Compiles the INTERLIS source and returns a {@link CompilationOutcome}. */
    public CompilationOutcome compile(Path source, CompilerSettings settings) {
        settings = Optional.ofNullable(settings).orElse(new CompilerSettings(null));
        COMPILE_LOCK.lock();
        Path logFile = null;
        FileLogger flog = null;
        TransferDescription td = null;

        try {
            logFile = Files.createTempFile("ili2c_", ".log");
            flog = new FileLogger(logFile.toFile(), false);
            StdListener.getInstance().skipInfo(true);
            EhiLogger.getInstance().addListener(flog);
            EhiLogger.getInstance().removeListener(StdListener.getInstance());

            Ili2cSettings iliSettings = new Ili2cSettings();
            ch.interlis.ili2c.Main.setDefaultIli2cPathMap(iliSettings);
            if (settings.modelRepositories() != null && !settings.modelRepositories().isBlank()) {
                iliSettings.setIlidirs(settings.modelRepositories());
            } else {
                iliSettings.setIlidirs(Ili2cSettings.DEFAULT_ILIDIRS);
            }

            Configuration cfg = new Configuration();
            cfg.addFileEntry(new FileEntry(source.toString(), FileEntryKind.ILIMODELFILE));
            cfg.setAutoCompleteModelList(true);
            cfg.setGenerateWarnings(true);

            DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date today = new Date();
            String dateOut = dateFormatter.format(today);

            td = ch.interlis.ili2c.Main.runCompiler(cfg, iliSettings, null);
            if (td == null) {
                EhiLogger.logError("...compiler run failed " + dateOut);
            } else {
                EhiLogger.logState("...compiler run done " + dateOut);
            }
        } catch (Exception ex) {
            LOGGER.warn("ili2c compilation failed: {}", ex.getMessage());
            return new CompilationOutcome(null, ex.getMessage(), List.of());
        } finally {
            try {
                StdListener.getInstance().skipInfo(false);
                EhiLogger.getInstance().addListener(StdListener.getInstance());
                if (flog != null) {
                    try {
                        flog.close();
                    } catch (Exception ignore) {
                        // ignore close error
                    }
                    EhiLogger.getInstance().removeListener(flog);
                }
            } finally {
                COMPILE_LOCK.unlock();
            }
        }

        String logText = readLog(logFile);
        List<Message> messages = Ili2cLogParser.parseErrors(logText);
        return new CompilationOutcome(td, logText, messages);
    }

    private String readLog(Path logFile) {
        if (logFile == null) {
            return "";
        }
        try {
            String text = Files.readString(logFile, StandardCharsets.UTF_8);
            Files.deleteIfExists(logFile);
            return text;
        } catch (IOException ex) {
            LOGGER.debug("Failed to read ili2c log: {}", ex.getMessage());
            return "";
        }
    }

    /** Parses ili2c output and converts it into {@link Message}s. */
    static class Ili2cLogParser {
        private Ili2cLogParser() {
        }

        static List<Message> parseErrors(String logText) {
            if (logText == null || logText.isBlank()) {
                return List.of();
            }
            List<Message> messages = new ArrayList<>();
            for (String line : logText.split("\r?\n")) {
                if (line.startsWith("E:")) {
                    messages.add(new Message(Message.Severity.ERROR, null, -1, -1, line.substring(2).trim()));
                } else if (line.startsWith("W:")) {
                    messages.add(new Message(Message.Severity.WARNING, null, -1, -1, line.substring(2).trim()));
                }
            }
            return messages;
        }
    }

    /** Utility for future integration tests. */
    public List<Message> formatMessages(Map<String, Object> values) {
        List<Message> messages = new ArrayList<>();
        values.forEach((key, value) -> messages.add(new Message(Message.Severity.INFO, key, -1, -1, String.valueOf(value))));
        return messages;
    }
}
