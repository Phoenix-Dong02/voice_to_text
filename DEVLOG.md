```markdown
# DEVLOG — voice_to_text (COMP3011 Assignment 1)

Purpose: track key progress, issues encountered, and current status.
---

## Project Info

- **GitHub repo**: `https://github.com/Phoenix-Dong02/voice_to_text.git`
- **Local path**: `D:\devworkspace\voice_to_text` (do **not** use the old copy under the C: drive Chinese-username directory — that one is deprecated)
- **IDE**: Spring Tool Suite (STS), workspace `javaworkspace`
- **JDK path**: `D:\JDK`
- **Standalone Maven**: installed at `D:\maven\apache-maven-3.9.16`; `MAVEN_HOME` and `Path` environment variables configured
- **Package structure**: `comp3011.voice_to_text`
- **Main class**: `comp3011.voice_to_text.VoiceToTextApplication`
- **Maven coordinates**: groupId=`comp3011`, artifactId=`voice_to_text`
- **Dependencies**: Spring Web, HTTP Client (Spring Boot 4.1.0)

---

## AI Assistance Disclosure

Development was supported by conversations with Claude (Anthropic) for
concept explanation, code review, and Socratic-style debugging guidance
(e.g. clarifying why MediaRecorder.stop() doesn't stop the underlying
stream, reviewing FormData design tradeoffs, understanding Spring's
component scanning/bean lifecycle, and the mechanics of outbound
multipart requests via RestClient). All code was written and debugged
independently; Claude was used as a tutor, not as a code generator.
See commit history for incremental, independent progress.

---

## ⚠️ Known Issue: App fails to start via STS / mvnw

**Symptom**: Running via STS Boot Dashboard, right-click Run As → Spring Boot App, `mvnw.cmd spring-boot:run`, or `mvn spring-boot:run` (standalone Maven) all fail with the same error:

```
NoClassDefFoundError: org/springframework/boot/SpringApplication
Caused by: ClassNotFoundException: org.springframework.boot.SpringApplication
```

**Causes investigated and ruled out**:
- ❌ Chinese username in the file path (moved to a plain-ASCII D: drive path — issue persisted)
- ❌ JAVA_HOME misconfigured (confirmed correct, `D:\JDK`)
- ❌ Dependencies not downloaded (`mvn dependency:tree` confirms `spring-boot` core jar present with `compile` scope)
- ❌ Corrupted local jar (file size normal, 1.39MB)
- ❌ Stale Run Configuration / Maven cache (cleared and retried, no effect)

**Root cause not fully identified**, but a reliable workaround was found:

```bash
# Always use this two-step process to run the app — avoid spring-boot:run and mvnw
mvn clean package -DskipTests
java -jar target\voice_to_text-0.0.1-SNAPSHOT.jar
```

This starts successfully with Tomcat listening on port 8080. Conveniently, **this is also exactly the format TITAN expects** (a Fat/Uber JAR), so this workflow will be used for the rest of development rather than chasing the STS/mvnw bug further.

---

## Local environment variable setup (OPENAI_API_KEY)

Created a personal OpenAI API key at platform.openai.com (billing configured,
$5 pay-as-you-go with auto-reload) for **local testing only**. This is
separate from the key TITAN already provides via its own OS environment —
this local key exists purely so `/api/v1/transcribe` can be tested against
the real OpenAI API before deployment.

`set OPENAI_API_KEY=...` in cmd is **not persistent** — it only applies to
the current cmd session. Must be re-run every time a new terminal window is
opened before `java -jar ...`. Worth revisiting later whether STS Run
Configuration environment variables offer a more permanent local setup.

---

## Current Progress

- [x] GitHub repo created, push/pull working
- [x] Spring Boot project skeleton generated (Maven, Spring Web + HTTP Client)
- [x] App starts successfully (via package + java -jar); `localhost:8080` shows Whitelabel Error Page (expected — no Controller written yet)
- [x] Write first test `@RestController` (`HelloController`, GET `/api/v1/hello`)
- [x] Frontend: record button + MediaRecorder + stop button
  - [x] `index.html` created under `static/`, with recordBtn / status / result elements
  - [x] `getUserMedia()` wired up: clicking start requests mic permission, status updates to "recording", confirmed working in browser (mic icon shows in address bar, MediaStream logged to console)
  - [x] Create `MediaRecorder` from the stream to actually capture audio
  - [x] Add stop button + stop logic (stop recording, release mic, package audio data)
    - Used `mediaRecorder.start(1000)` (with timeslice) rather than `start()` with no
      argument, so `dataavailable` fires periodically and `audioChunks` genuinely
      accumulates multiple Blob fragments before being merged — not just a single
      fragment at stop time
    - Bug encountered + fixed: wrote `stream.getTracks.forEach(...)` (missing `()`
      after `getTracks`), which threw `TypeError: ... forEach is not a function`
      because `getTracks` without invocation returns the method itself, not the
      array. Diagnosed using `debugger;` statement to pause execution and inspect
      `stream` at the exact failure point, rather than guessing from stale console output
  - [x] Package `finalRecording` Blob into `FormData`, upload via `fetch()` to
    `/api/v1/transcribe`, parse JSON response and display `text` field in `result` div
    - Field name `"audio"` chosen for the FormData key; backend Controller must
      use matching `@RequestParam("audio")` to bind correctly
    - `finalizeRecording` marked `async` so it can `await uploadRecording(...)`,
      ensuring mic release happens before upload starts, and the function doesn't
      return until upload + transcription display fully completes
- [x] Frontend: display transcription result in `result` div
- [x] Located assignment YAML spec + official grading rubric (course site, 2026-09-04)
- [x] Backend: `TranscribeController` with `@PostMapping("/api/v1/transcribe")`,
  accepting `@RequestParam("audio") MultipartFile audio` — confirmed end-to-end
  with frontend, logs original filename + size to console to verify receipt
- [x] Backend: call OpenAI `/v1/audio/transcriptions` (model `gpt-4o-mini-transcribe`) —
  **tested working end-to-end with real API, returns actual transcribed speech**
  - `OPENAI_API_KEY` read via `System.getenv(...)`; only presence (non-null) is
    logged to console, never the key value itself (hard requirement per rubric —
    logging/printing the key caps the whole category at Fail regardless of
    everything else working)
  - Incoming `MultipartFile` cannot be re-sent as-is (it's a receive-only Spring
    wrapper); rewrapped its bytes in an anonymous `ByteArrayResource` subclass
    overriding `getFilename()`, since OpenAI's multipart parser also requires a
    filename on the file part
  - Request body built as `MultiValueMap<String, Object>` (not `Map`) because
    `RestClient` only knows how to serialize multipart bodies from that type —
    not because this endpoint currently needs multiple values per key
  - `RestClient` call is currently **fully blocking** (`.retrieve().body(...)`
    parks the thread until OpenAI responds) — this is expected and fine for a
    single request, but is the direct reason the concurrency requirement (200+
    simultaneous blocking requests) needs deliberate handling next, not an
    afterthought
- [ ] Frontend: auto-reset UI (recordBtn/status text) so the page is ready for
  the next recording without a manual refresh
- [ ] Backend: `GET /api/v1/admin/uptime` — return server start time, current time, uptime in seconds
- [ ] Backend: `POST /api/v1/admin/shutdown` — accept shutdown request, return 202, handle 409 if already shutting down
- [ ] Backend: `GET /api/v1/global/stats` — cumulative input/output token counts since server start
- [ ] Concurrency testing: must handle 200+ concurrent requests without significant delay or crashing
- [ ] Package as Fat JAR, test on TITAN
- [ ] Final submission: GitHub link to Gradescope

**Immediate next step**: Verify concurrency behaviour of the blocking
`RestClient` call under load (e.g. with `ab` or `wrk` at 200+ simultaneous
requests) before building out the remaining admin/stats endpoints — this is
the single biggest unverified risk area and worth 30/100 points on its own.

---

## Core Assignment Requirements (recap)

- Web page served at `http://localhost:8080/`, with a clear recording-in-progress indicator and a stop control
- Transcription displayed within 5 seconds of stopping; page auto-resets for the next recording
- API key must only be read from an environment variable on the backend — never logged, printed, or exposed to the frontend
- Must handle 200+ concurrent blocking HTTP requests within a single Java process without blocking
- Use Spring profiles/environment variables to switch between local and TITAN configs — not commented-out code
- Due Sunday, September 13; worth 20% of final grade; requires at least one TITAN hand-in plus a final GitHub link submission via Gradescope
- Viva demonstration required — must be able to explain and defend every line of code written
- Transcription model must be `gpt-4o-mini-transcribe` (not just "any" OpenAI STT model)
- For recordings under 1 minute, transcription result must display within 5 seconds of stopping
- **Concurrency is graded as its own 30/100-point rubric category (equal weight to backend correctness)**:
  must handle 200+ concurrent blocking HTTP requests within a single Java process without
  significant delay or crashing. This is not a stretch goal — it must be planned for before
  the transcribe endpoint is finalized, not tested as an afterthought.
- Grading rubric breakdown (for reference): REST API/Backend/Cloud STT 30pts,
  Concurrency 30pts, Frontend 20pts, Code Quality/Comments/Tests 20pts
- API key handling is a hard fail condition if violated: rubric explicitly caps score at
  Fail tier if the OPENAI_API_KEY is ever logged, printed, persisted, or leaked to the client

---

*Last updated: 2026-09-07*
```