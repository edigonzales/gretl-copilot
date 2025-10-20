package ch.so.agi.gretl.chat.model;

import java.util.List;

public record RagContext(String intentLabel, List<String> retrievedTaskSummaries) {
}
