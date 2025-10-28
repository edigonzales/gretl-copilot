package ch.so.agi.gretl.copilot.chat;

import gg.jte.Content;
import gg.jte.TemplateOutput;

public class HtmlFragment implements Content {
    private final String html;

    public HtmlFragment(String html) {
        this.html = html;
    }

    @Override
    public void writeTo(TemplateOutput output) {
        if (html != null && !html.isEmpty()) {
            output.writeContent(html);
        }
    }

    @Override
    public boolean isEmptyContent() {
        return html == null || html.isEmpty();
    }
}
