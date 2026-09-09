package comp3011.voice_to_text;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final ConfigurableApplicationContext context;
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    public AdminController(ConfigurableApplicationContext context) {
        this.context = context;
    }

    // i don't need to create a variable to record time
    // because JVM is a process that will record time since birth
    @GetMapping("/uptime")
    public Map<String, Object> uptime() {

        long time = ManagementFactory.getRuntimeMXBean().getStartTime();
        Instant startTime = Instant.ofEpochMilli(time);

        Map<String, Object> response = new HashMap<>();
        response.put("startTime", startTime);
        Instant now = Instant.now();
        response.put("currentTime", now);
        // duration accepts instant type only
        response.put("uptimeSeconds", Duration.between(startTime, now).getSeconds());

        return response;
    }

    @PostMapping("/shutdown")
    public ResponseEntity<Map<String, Object>> shutdown() {

    	boolean shutDown = isShuttingDown.compareAndSet(false, true);

    	if(shutDown) {
    		// spawn a new thread to close the context
    		// because once this method returns, no code after the return statement can execute
    		// if context.close() ran before the return, the response might neverbe sent
    		
    		new Thread(()->{
    			try{
    				Thread.sleep(500);
    			} catch(InterruptedException e) {
    				// mark the thread interrupted mannually
    				Thread.currentThread().interrupt();
    				
    			}
    			context.close();
    		}).start();
    		
    		Map<String, Object> response = new HashMap<>();
    		response.put("message", "this application is shutting down now!");
    		return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    	}else {
    		Map<String, Object> response = new HashMap<>();
    		response.put("message", "this application is already in shutdown process!");
    		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    	}

    }
 }