package comp3011.voice_to_text;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.HashMap;

@RestController
public class GlobalStatsController {
	
    private final UsageStatsService usageStatsService;
    // dependency injection
    // spring will find UsageStatsService in applicationcontext
    // and then create globalStatsController with it
    public GlobalStatsController(UsageStatsService usageStatsService) {
        this.usageStatsService = usageStatsService;
    }

    @GetMapping("/api/v1/global/stats")
    public Map<String, Long> getStats() {
        Map<String, Long> result = new HashMap<>();
        result.put("inputTokens", usageStatsService.readInputTokens());
        result.put("outputTokens", usageStatsService.readOutputTokens());
        return result;
    }
}