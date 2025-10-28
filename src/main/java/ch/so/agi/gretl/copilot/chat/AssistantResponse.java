package ch.so.agi.gretl.copilot.chat;

import gg.jte.Content;

public record AssistantResponse(Content markdownHtml, Content codeBlockHtml, Content linksHtml) {
    public boolean hasMarkdown() {
        return markdownHtml != null && !markdownHtml.isEmptyContent();
    }

    public boolean hasCodeBlock() {
        return codeBlockHtml != null && !codeBlockHtml.isEmptyContent();
    }

    public boolean hasLinks() {
        return linksHtml != null && !linksHtml.isEmptyContent();
    }
}
