package comp3011.voice_to_text;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.HashMap;

@RestController
public class TranscribeController {

    @PostMapping("/api/v1/transcribe")
    public Map<String, String> transcribe(@RequestParam("audio") MultipartFile audio) {


    	System.out.println(audio.getOriginalFilename());
    	System.out.println(audio.getSize());

    	Map<String, String> result = new HashMap<>();
    	result.put("text", "hello from backend");
    	return result;

    }
}