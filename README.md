# Course Content Studio — Jakarta Agentic AI sample

An **education** sample for the [Jakarta Agentic AI](https://github.com/) API, running on
**Payara Server**. A teacher pastes a chapter and picks a subject; the agent
generates an **introduction**, a **quiz** and a **conclusion**, and the teacher
can **approve** the result or **refine** any part by chat.

It is a deliberately "advanced" sample — it exercises features the basic
quickstart does not:

| Feature | Where |
|---|---|
| **Ordered phases** (`@Decision`/`@Action` with `order`) | `CourseContentAgent` — intro → quiz → conclusion |
| **Typed JSON-B output** (`query(prompt, Quiz.class, …)`) | `writeQuiz` returns a `Quiz` record |
| **Per-workflow conversational memory** | `writeConclusion` does not re-send the chapter |
| **Human-in-the-loop** (generate vs. refine, per section) | event carries the mode; `refine-section` |
| **Resilience** (`@HandleException`) | `onBadQuiz` re-prompts with a strict schema, then falls back |
| **Per-subject specialisation** | `SubjectRubric` prepends a subject rubric to prompts |

## How it works

`CourseResource` fires a `CoursePacketRequest` CDI event. `Event.fire(...)` is
**synchronous**, so the whole agent workflow (every LLM call) finishes before it
returns; the resource then reads the finished `CoursePacket` back from
`PacketStore` and returns it as JSON.

The agent has **no scope annotation**, so the runtime applies `@WorkflowScoped`
(the spec default). That is what makes its `draft` instance field safe as
per-workflow state across the ordered phases.

```
POST /course/api/packet/generate        { subject, chapterTitle, chapterBody }
POST /course/api/packet/refine-section  { section: intro|quiz|conclusion|all, instruction }
POST /course/api/packet/approve
GET  /course/api/packet                 current packet
GET  /course/api/subjects               subject list
GET  /course/                           the UI
```

## Prerequisites

- JDK 17+
- Maven 3.9+
- A **Payara Server 7** build that includes the `agentic-ai-core` runtime module
  (the module that provides the `jakarta.ai.agent` API and the LLM backends).
- An LLM backend:
  - **Ollama** (default, free, local): `ollama pull gemma3:12b`
  - or **Anthropic/Claude**: set `ANTHROPIC_API_KEY` in the server environment.

## Configure the LLM

Edit `src/main/resources/META-INF/microprofile-config.properties`. The default
uses Ollama with `gemma3:12b` (recommended over `4b` because the quiz step needs
clean JSON). To use Claude, uncomment the Anthropic block and export the key
**before** starting the domain:

```bash
export ANTHROPIC_API_KEY=sk-ant-...
asadmin restart-domain
```

## Build & deploy

```bash
mvn clean package
asadmin deploy --contextroot /course target/course.war
```

Then open <http://localhost:8080/course/>.

## Try it

1. Pick **Physics**, title `Newton's Second Law`, paste a few paragraphs, click
   **Generate packet**. Watch `server.log` for
   `[TRIGGER] → [DECISION] → [ACTION] writeIntro → writeQuiz → writeConclusion → [OUTCOME]`.
2. In **Refine**, choose **Quiz**, type *"make the questions harder and add one
   real-world application"*, click **Refine** — only the quiz changes.
3. Click **Approve** — the packet is flagged approved.

## Notes

- This is a single-author demo: `PacketStore` keeps one latest packet. A
  multi-user app would key packets by author/session.
- The quiz gate (`hasTeachableContent`) requires ≥ 80 characters of chapter text
  in generate mode; short snippets stop the workflow at the decision phase.
- If the configured provider is `none` (no backend), the runtime's no-op LLM
  returns empty content and the packet will be blank — set a real provider.