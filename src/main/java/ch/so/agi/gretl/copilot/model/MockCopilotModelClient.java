package ch.so.agi.gretl.copilot.model;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

@Component
public class MockCopilotModelClient implements CopilotModelClient {

    @Override
    public Flux<CopilotStreamSegment> streamResponse(CopilotPrompt prompt) {
        String explanation = "Here is the GRETL task you can use to import an INTERLIS transfer "
                + "file into PostGIS. I also included the Gradle snippet so you can paste it into "
                + "your pipeline.";

        String buildGradle = "task importInterlis(type: ch.so.agi.gretl.tasks.InterlisImport) {\n"
                + "    dataset = 'https://example.com/fubar.xtf'\n"
                + "    targetSchema = 'interlis_import'\n"
                + "    modeldir = 'https://models.interlis.ch/'\n"
                + "    defaultSrsCode = '2056'\n"
                + "    createSqlLog = true\n"
                + "}";

        String links = "Top hits: tasks/interlis/importPostgis, examples/interlis/postgis-import, task properties.";

        Flux<CopilotStreamSegment> textStream = Flux.fromIterable(tokenize(explanation))
                .delayElements(Duration.ofMillis(120))
                .map(token -> new CopilotStreamSegment(CopilotStreamSegment.SegmentType.TEXT, token));

        CopilotStreamSegment codeSegment = new CopilotStreamSegment(CopilotStreamSegment.SegmentType.CODE_BLOCK,
                buildGradle);

        CopilotStreamSegment linksSegment = new CopilotStreamSegment(CopilotStreamSegment.SegmentType.LINKS, links);

        return Flux.concat(textStream, Flux.just(codeSegment), Flux.just(linksSegment));
    }

    private List<String> tokenize(String explanation) {
        return List.of(explanation.split(" "));
    }
}
