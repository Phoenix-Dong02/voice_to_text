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
stream, reviewing FormData design tradeoffs). All code was written and
debugged independently; Claude was used as a tutor, not as a code
generator. See commit history for incremental, independent progress.

---

## ⚠️ Known Issue: App fails to start via STS / mvnw

**Symptom**: Running via STS Boot Dashboard, right-click Run As → Spring Boot App, `mvnw.cmd spring-boot:run`, or `mvn spring-boot:run` (standalone Maven) all fail with the same error:

NoClassDefFoundError: org/springframework/boot/SpringApplication
Caused by: ClassNotFoundException: org.springframework.boot.SpringApplication


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
- [ ] Frontend: auto-reset UI (recordBtn/status text) so the page is ready for
  the next recording without a manual refresh
- [ ] Backend: endpoint to receive uploaded audio
- [ ] Backend: call OpenAI `/v1/audio/transcriptions` (API key from `OPENAI_API_KEY` env var — must never leak to frontend/logs)
- [ ] Backend: `GET /api/v1/admin/uptime` — return server start time, current time, uptime in seconds
- [ ] Backend: `POST /api/v1/admin/shutdown` — accept shutdown request, return 202, handle 409 if already shutting down
- [ ] Backend: `GET /api/v1/global/stats` — cumulative input/output token counts since server start
- [ ] Concurrency testing: must handle 200+ concurrent requests
- [ ] Package as Fat JAR, test on TITAN
- [ ] Final submission: GitHub link to Gradescope

**Immediate next step**: Write a Spring Boot `@RestController` with a
`@PostMapping("/api/v1/transcribe")` endpoint that accepts a `MultipartFile`
(bound via `@RequestParam("audio")`) — this is currently missing, so the
frontend's fetch call will fail until this exists.

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

*Last updated: 2026-09-04*