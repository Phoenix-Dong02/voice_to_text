package comp3011.voice_to_text;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@RestController
public class TranscribeController {

    private static final String OPENAI_URL = "https://api.openai.com/v1/audio/transcriptions";
    private static final String MODEL = "gpt-4o-mini-transcribe";

    @PostMapping("/api/v1/transcribe")
    public Map<String, String> transcribe(@RequestParam("audio") MultipartFile audio) throws IOException {

        System.out.println(audio.getOriginalFilename());
        System.out.println(audio.getSize());

        String apiKey = System.getenv("OPENAI_API_KEY");
        // just check if I get  the key immediately
        // in case that key is empty
        if (apiKey != null) {
        	System.out.println("got key");
        } else {
        	System.out.println("can't access key");
        	
        }
        // because multipartfile can not be sent
        // so i need to changge it to the file to be sent
        // why use ByteArrayResource not byte[]
        // because openai requires filename and byte[] only stores bytes
        ByteArrayResource fileResource = new ByteArrayResource(audio.getBytes()) {
            @Override
            public String getFilename() {
                return audio.getOriginalFilename();
            }
        };
        
        // why use multivaluemap not map
        // main reason is that restclient accepts multivaluemap not map
        // partial reason is that i can send multiaudio to openai at once for future development
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("model", MODEL);
        

        // pack it up and send it to openai
        Map<String, Object> response = RestClient.create()
        		.post()
        		.uri(OPENAI_URL)
        		.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
        		.body(body)
        		.retrieve()
        		.body(Map.class);

        // return to frontend
        Map<String, String> result = new HashMap<>();
        result.put("text", (String)response.get("text"));
        return result;
        
    }
}