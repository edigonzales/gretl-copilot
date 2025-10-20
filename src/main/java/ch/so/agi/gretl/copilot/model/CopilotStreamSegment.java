package ch.so.agi.gretl.copilot.model;

public record CopilotStreamSegment(SegmentType type, String content) {
    public enum SegmentType {
        TEXT,
        CODE_BLOCK,
        LINKS
    }
}
