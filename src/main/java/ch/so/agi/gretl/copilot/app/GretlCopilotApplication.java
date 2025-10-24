package ch.so.agi.gretl.copilot.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import ch.so.agi.gretl.copilot.intent.IntentClassifierProperties;
import ch.so.agi.gretl.copilot.retrieval.RetrievalProperties;

@SpringBootApplication(scanBasePackages = "ch.so.agi.gretl.copilot")
@EnableConfigurationProperties({ RetrievalProperties.class, IntentClassifierProperties.class })
public class GretlCopilotApplication {
    public static void main(String[] args) {
        SpringApplication.run(GretlCopilotApplication.class, args);
    }
}
