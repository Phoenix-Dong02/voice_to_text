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
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@RestController
public class TranscribeController {

    private static final String OPENAI_URL = "https://api.openai.com/v1/audio/transcriptions";
    private static final String MODEL = "gpt-4o-mini-transcribe";
    private final UsageStatsService usageStatsService;

    public TranscribeController(UsageStatsService usageStatsService) {
        this.usageStatsService = usageStatsService;
    }

        @PostMapping("/api/v1/transcribe")
        public ResponseEntity<Map<String, String>> transcribe(@RequestParam("audio") MultipartFile audio) {


            // uploading byte stream
            byte[] audioBytes;
            try {
                audioBytes = audio.getBytes();
                // the error is specific and only about byte uploading failure
                // IOException can cover the error
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(errorBody("Could not read uploaded audio file."));
            }

            String apiKey = System.getenv("OPENAI_API_KEY");

            // because multipartfile can not be sent
            // so i need to change it to the file to be sent
            // why use ByteArrayResource not byte[]
            // because openai requires filename and byte[] only stores bytes
            ByteArrayResource fileResource = new ByteArrayResource(audioBytes) {
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

            // call Open AI
            Map<String, Object> response;
            try {
                // pack it up and send it to openai
                response = RestClient.create()
                        .post()
                        .uri(OPENAI_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .body(body)
                        .retrieve()
                        .body(Map.class);
             // the error varies and i am not sure which error
             // use exception to deal with errors generally
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(errorBody("Transcription service is unavailable, please try again."));
            }

            // parse the response and get the content
            try {
                Map<String, Object> usage = (Map<String, Object>) response.get("usage");
             // not sure whether the values are double or integer
             // so i change them to number first and then long type
                long inputTokens = ((Number) usage.get("input_tokens")).longValue();
                long outputTokens = ((Number) usage.get("output_tokens")).longValue();
                usageStatsService.addTokens(inputTokens, outputTokens);
             // return to frontend
                Map<String, String> result = new HashMap<>();
                result.put("text", (String) response.get("text"));
                return ResponseEntity.ok(result);
                // parsing the response can fail in multiple ways (missing keys, wrong types)
                // use exception to deal with errors generally
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorBody("Unexpected response format from transcription service."));
            }
            
        }

        private Map<String, String> errorBody(String message) {
            Map<String, String> body = new HashMap<>();
            body.put("error", message);
            return body;
        }
}
