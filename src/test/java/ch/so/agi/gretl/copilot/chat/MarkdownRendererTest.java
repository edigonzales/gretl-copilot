package ch.so.agi.gretl.copilot.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarkdownRendererTest {

    @Test
    void rendersHeadingsTablesListsAndInlineCode() {
        MarkdownRenderer renderer = new MarkdownRenderer();
        String markdown = "## Überblick\n\n" +
                "| Property | Typ | Pflicht | Standardwert | Beschreibung |\n" +
                "| --- | --- | --- | --- | --- |\n" +
                "| dataFiles | FileCollection | Nein | – | Zu validierende INTERLIS-Datei(en), z. B. eine XTF-Datei. |\n" +
                "| logFile | File | Nein | – | Datei für das Validator-Protokoll. |\n" +
                "| allObjectsAccessible | Boolean | Nein | – | Option zur Steuerung der Zugänglichkeit aller Objekte während der Prüfung. |\n\n" +
                "* Unterstützt Listenpunkte\n" +
                "* Und weitere Elemente\n\n" +
                "Eingebetteter `Code` innerhalb eines Absatzes.";

        String html = renderer.render(markdown);

        assertThat(html).contains("<h2>Überblick</h2>");
        assertThat(html).contains("<table>");
        assertThat(html).contains("<thead>");
        assertThat(html).contains("<tbody>");
        assertThat(html).contains("<li>Unterstützt Listenpunkte</li>");
        assertThat(html).contains("<code>Code</code>");
    }
}
