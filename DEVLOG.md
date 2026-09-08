# DEVLOG — voice_to_text (COMP3011 Assignment 1)

Purpose: track key progress, issues encountered, and current status.
---

## Project Info

- **GitHub repo**: `https://github.com/Phoenix-Dong02/voice_to_text.git`
- **Local path**: `D:\devworkspace\voice_to_text` (do **not** use the old copy under the C: drive Chinese-username directory — that one is deprecated)
- **IDE**: Spring Tool Suite (STS), workspace `javaworkspace`
- **JDK path**: `D:\JDK21install\jdk-21.0.12.1+1` (was `D:\JDK` / JDK 17, upgraded for virtual threads — see JDK Upgrade section below)
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
component scanning/bean lifecycle, the mechanics of outbound multipart
requests via RestClient, and the JDK 21 virtual-thread upgrade and
concurrency verification methodology). All code was written and debugged
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

Note: STS's own JRE binding for this project (visible in Package Explorer)
still shows `JavaSE-17` even after the JDK 21 upgrade below — since the
command-line `mvn`/`java -jar` workflow above is already the established
way this project is run and packaged, this has not been fixed yet. Low
priority; only matters if STS's own green "Run" button is used instead.

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

## JDK Upgrade: 17 → 21 (for Virtual Threads)

**Reason**: Java's built-in virtual threads (GA since JDK 21) let Tomcat handle
each blocking HTTP request on a lightweight virtual thread instead of a scarce
platform/OS thread. When the blocking `RestClient` call to OpenAI parks waiting
for a response, the JVM automatically unmounts the virtual thread from its
carrier OS thread, freeing that OS thread to serve other requests — solving
the concurrency requirement without rewriting the Controller into reactive/
WebClient style.

**Pre-check**: confirmed TITAN's runtime is OpenJDK 26 (visible in TITAN's
Job Details console output for a prior submission) — well above 21, so
upgrading locally carries no deployment risk (JVM bytecode compiled at a
lower class-file version always runs on a newer JVM, never the reverse).

**Installation issues encountered** (documented in case they recur):
- GUI `.msi` installer silently failed twice (UAC/permission-related,
  root cause not fully confirmed) — target directory was created but left
  empty, `JAVA_HOME`/`Path` never actually updated despite the installer's
  checkboxes being ticked
- `msiexec` command-line install (`/qb`, various `ADDLOCAL` feature-name
  guesses, then `ADDLOCAL=ALL`) also failed with exit code 1 across multiple
  attempts; MSI verbose log (`/l*v`) confirmed genuine install failure via
  rollback, not a misconfigured parameter
- **Resolution**: abandoned the `.msi` installer entirely. Downloaded the
  Temurin 21 **ZIP** distribution instead, extracted manually to
  `D:\JDK21install\jdk-21.0.12.1+1`, and pointed `JAVA_HOME` at that path by
  hand (same manual method already used for JDK 17) — completely sidesteps
  whatever the installer's permission/rollback issue was
- Old `D:\JDK` (JDK 17) left untouched on disk, just no longer referenced by
  `JAVA_HOME` — kept as a fallback in case anything else on the machine
  still depends on 17

**Updated environment**:
- `JAVA_HOME` → `D:\JDK21install\jdk-21.0.12.1+1`
- `pom.xml`: `<java.version>` changed from `17` to `21`
- Verified via fresh cmd window: `java -version` / `javac -version` both
  report `21.0.12.1`

---

## Concurrency Verification (Virtual Threads + Load Test)

**Enabled** via one line in `application.properties`:
```
spring.threads.virtual.enabled=true
```

**Verified virtual threads were actually active, two independent ways**
(config silently doing nothing was a real risk — Spring Boot does not
error on a misspelled/ineffective property):
1. Temporary debug line in `TranscribeController`:
   `System.out.println("Current thread is virtual: " + Thread.currentThread().isVirtual());`
   → printed `true` for a real request. **Removed before final submission**
   (see Code Quality TODO below).
2. Independent confirmation from a real exception's stack trace during load
   testing, which showed `at java.base/java.lang.VirtualThread.run(...)` at
   the bottom of the call stack — JVM-generated evidence, not something we
   printed ourselves.

**Load test methodology**: wrote a standalone Python script
(`concurrency_test.py`, kept outside this repo — see note below) using
`concurrent.futures.ThreadPoolExecutor` to fire N real concurrent multipart
POST requests at `/api/v1/transcribe`, each carrying a real short `.m4a`
audio file, recording per-request latency and status code.

**Results at N=200**:
- 200/200 requests succeeded (`200 OK`)
- Total wall-clock time: 5.91s; average per-request latency: 3.26s
- 199/200 requests completed within 5s; **one outlier at 5.83s**
- Judged the single outlier as normal latency variance in OpenAI's own API
  response time (outside this application's control), not a sign of a
  concurrency/architecture problem — reasoning: a real thread-starvation or
  resource-exhaustion issue would be expected to affect a *cluster* of
  requests together, not exactly one isolated case, and the outlier request
  still succeeded rather than timing out or erroring
- Distinguished (conceptually, not hit in this run) that a `429` status
  would indicate OpenAI-side rate limiting — a separate concern from this
  application's own concurrency handling — versus `500`, which would point
  to a genuine server-side bug

**Note on `concurrency_test.py`**: this script is a personal testing tool,
not part of the graded deliverable — it hardcodes a local Windows file path
(with a personal WeChat file-transfer folder) and prints status messages in
Chinese. Deliberately **not committed to this repo**; screenshots of the
test run are kept as evidence instead, and this section documents the
methodology in place of the script itself.

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
  - `RestClient` call is **fully blocking** (`.retrieve().body(...)` parks the
    thread until OpenAI responds) — resolved via JDK 21 virtual threads rather
    than rewriting to WebClient (see JDK Upgrade + Concurrency Verification
    sections)
- [x] JDK upgraded 17 → 21, `spring.threads.virtual.enabled=true` set
- [x] Concurrency testing: 200 concurrent requests, 200/200 succeeded, 199/200
  completed within 5s (see Concurrency Verification section for full
  methodology and the one 5.83s outlier)
- [ ] Frontend: auto-reset UI (recordBtn/status text) so the page is ready for
  the next recording without a manual refresh
- [ ] Backend: `GET /api/v1/admin/uptime` — return server start time, current time, uptime in seconds
- [ ] Backend: `POST /api/v1/admin/shutdown` — accept shutdown request, return 202, handle 409 if already shutting down
- [ ] Backend: `GET /api/v1/global/stats` — cumulative input/output token counts since server start
- [ ] Code Quality: remove temporary debug `System.out.println` statements added
  during virtual-thread verification and Multipart debugging (`isVirtual()`,
  `getOriginalFilename()`, `getSize()`) before final submission
- [ ] Code Quality: add proper `try-catch` around the OpenAI call / audio
  processing so `IOException` and OpenAI API errors return a meaningful JSON
  error response instead of a bare 500 with no information
- [ ] Package as Fat JAR, test on TITAN
- [ ] Final submission: GitHub link to Gradescope

**Immediate next step**: Implement the three admin/stats endpoints
(`/api/v1/admin/uptime`, `/api/v1/admin/shutdown`, `/api/v1/global/stats`)
per the YAML spec — concurrency risk is now verified and de-risked, so this
is the next largest unstarted chunk of backend work.

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

*Last updated: 2026-09-08*
```

