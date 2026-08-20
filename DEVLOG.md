# DEVLOG — voice_to_text (COMP3011 Assignment 1)

Purpose: track key progress, issues encountered, and current status. Paste this file into a new chat with Claude to quickly resume context.

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

## Current Progress

- [x] GitHub repo created, push/pull working
- [x] Spring Boot project skeleton generated (Maven, Spring Web + HTTP Client)
- [x] App starts successfully (via package + java -jar); `localhost:8080` shows Whitelabel Error Page (expected — no Controller written yet)
- [ ] Write first test `@RestController` (`HelloController`, GET `/api/v1/hello`) — **in progress, next step**
- [ ] Frontend: record button + MediaRecorder + stop button
- [ ] Backend: endpoint to receive uploaded audio
- [ ] Backend: call OpenAI `/v1/audio/transcriptions` (API key from `OPENAI_API_KEY` env var — must never leak to frontend/logs)
- [ ] Frontend: display transcription result, auto-reset for next recording
- [ ] Additional endpoints specified in the assignment YAML (uptime, runtime stats, graceful shutdown) — need to locate and read that YAML spec
- [ ] Concurrency testing: must handle 200+ concurrent requests
- [ ] Package as Fat JAR, test on TITAN
- [ ] Final submission: GitHub link to Gradescope

---

## Core Assignment Requirements (recap)

- Web page served at `http://localhost:8080/`, with a clear recording-in-progress indicator and a stop control
- Transcription displayed within 5 seconds of stopping; page auto-resets for the next recording
- API key must only be read from an environment variable on the backend — never logged, printed, or exposed to the frontend
- Must handle 200+ concurrent blocking HTTP requests within a single Java process without blocking
- Use Spring profiles/environment variables to switch between local and TITAN configs — not commented-out code
- Due Sunday, September 13; worth 20% of final grade; requires at least one TITAN hand-in plus a final GitHub link submission via Gradescope
- Viva demonstration required — must be able to explain and defend every line of code written

---

*Last updated: 2026-08-20*
