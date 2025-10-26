package ch.so.agi.gretl.copilot.session;

import java.util.Optional;

public class AssistantMessageView {
    private final StringBuilder markdown = new StringBuilder();
    private String codeBlockHtml;
    private String linksHtml;

    public synchronized void appendMarkdown(String text) {
        markdown.append(text);
    }

    public synchronized String getMarkdown() {
        return markdown.toString();
    }

    public synchronized void setCodeBlockHtml(String html) {
        this.codeBlockHtml = html;
    }

    public synchronized Optional<String> getCodeBlockHtml() {
        return Optional.ofNullable(codeBlockHtml);
    }

    public synchronized void setLinksHtml(String html) {
        this.linksHtml = html;
    }

    public synchronized Optional<String> getLinksHtml() {
        return Optional.ofNullable(linksHtml);
    }
}
