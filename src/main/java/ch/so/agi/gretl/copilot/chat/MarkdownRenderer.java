package ch.so.agi.gretl.copilot.chat;

import java.util.List;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MarkdownRenderer {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownRenderer() {
        List<Extension> extensions = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder().extensions(extensions).escapeHtml(true).build();
    }

    public String render(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return "";
        }

        Node document = parser.parse(markdown);
        String html = renderer.render(document);
        return "<div class=\"assistant-markdown\">" + html + "</div>";
    }
}
