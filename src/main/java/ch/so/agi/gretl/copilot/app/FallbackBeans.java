package ch.so.agi.gretl.copilot.app;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.so.agi.gretl.copilot.intent.IntentClassifier;
import ch.so.agi.gretl.copilot.intent.MockIntentClassifier;

@Configuration
public class FallbackBeans {

    @Bean
    @ConditionalOnMissingBean(IntentClassifier.class)
    public IntentClassifier mockIntentClassifier() {
        
        System.out.println("***** xxxccx");
        return new MockIntentClassifier();
    }
}
