package comp3011.voice_to_text;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

// let Spring instantiate this as a singleton so both
// TranscribeController and GlobalStatsController share the same counters
@Component
public class UsageStatsService {

    private final AtomicLong inputTokenCounter = new AtomicLong(0);
    private final AtomicLong outputTokenCounter = new AtomicLong(0);

    public void addTokens(long inputTokens, long outputTokens) {
        inputTokenCounter.addAndGet(inputTokens);
        outputTokenCounter.addAndGet(outputTokens);
    }

    public long readInputTokens() {
        return inputTokenCounter.get();
    }

    public long readOutputTokens() {
        return outputTokenCounter.get();
    }
}