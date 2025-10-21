package ch.so.agi.gretl.copilot.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "ch.so.agi.gretl.copilot")
public class GretlCopilotApplication {
    public static void main(String[] args) {
        SpringApplication.run(GretlCopilotApplication.class, args);
    }
}
