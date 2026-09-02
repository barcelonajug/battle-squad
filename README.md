# Battle Contender: Spring Boot & AI Workshop

![Battle Contender Banner](src/main/resources/static/banner-v2.png)

Welcome to the **Battle Contender** workshop template! In this workshop, you will learn how to build an AI-powered agent using **Spring Boot 4**, **Spring AI 2.0**, and **Java 25**. This project is set to interact with the superhero battle arena deployed at [https://superhero-battle-arena.barcelonajug.org/](https://superhero-battle-arena.barcelonajug.org/) to register a team, analyze game constraints, and use an LLM advisor to pick an optimal hero squad within a budget!

## 🚀 Workshop Structure

This repository contains the `main` branch, which serves as the starter project. The backend scaffolding and the HTML frontend (`src/main/resources/static/index.html`) are already built.

Your goal is to implement the **Spring AI** layer.

The `main` branch now includes the multi-phase drafting workflow as TODO scaffolding. The `solution` branch contains the full working example, including a visible draft-option list and the complete orchestration flow.

Before wiring `TodoWriteTool`, treat `MessageChatMemoryAdvisor` as a prerequisite in both branches. The todo checklist only remains coherent if the optimization run has a dedicated memory channel scoped by `teamId`, `sessionId`, and `roundNo`.

### Workshop Steps

This workshop is meant to be followed in this exact order. The README and the TODO comments in `BattleAdvisorService` use the same step names.

1. **Build the tools**
   Implement the Arena API wrappers in [`HeroSearchTool.java`](src/main/java/org/barcelonajug/battlecontender/ai/tools/HeroSearchTool.java) and [`ArenaManagementTool.java`](src/main/java/org/barcelonajug/battlecontender/ai/tools/ArenaManagementTool.java). Keep them stateless and focused on API access only.

2. **Configure `BattleAdvisorService`**
   Wire those tools into [`BattleAdvisorService.java`](src/main/java/org/barcelonajug/battlecontender/ai/BattleAdvisorService.java), define the drafting strategies, and return the structured response that the UI can render.

3. **Add chat memory first**
   Before using `TodoWriteTool`, configure `MessageChatMemoryAdvisor` and scope it by `teamId`, `sessionId`, and `roundNo`. This is required so the optimization run has one dedicated memory channel.

4. **Add `TodoWriteTool` on top of chat memory**
   Once chat memory is in place, register `TodoWriteTool` so the agent can maintain a visible optimization checklist while it works. The intent is: memory stores the conversation for the run, and TodoWrite exposes the current task list inside that same run.

5. **Use round-aware hero search**
   The arena v3 API adds allowed roles, genders, races, publishers, and alignments to `RoundSpec`, plus `/api/heroes/search/advanced`. In this branch, the model and client already support those fields. The remaining TODO is to make the tools use round-aware advanced search so the drafter starts from candidates that already fit the round.

6. **Validate every draft deterministically**
   After the model drafts a squad, validate it against team size, budget, required roles, banned tags, and the new allowed-value constraints before presenting the result or submitting it.

7. **Expose multiple draft options**
   Return `DraftOptionsResponse` so the UI can show several candidate squads and highlight the recommended one.

### How To Read The Code

On the `main` branch, the TODO comments inside `BattleAdvisorService` are not a second set of phases. They are the same workshop steps above, restated from the point of view of that one service.

### Starter Branch Notes

Look for `UnsupportedOperationException("TODO:...")` in the codebase. The main workshop files are:

- `src/main/java/org/barcelonajug/battlecontender/ai/tools/HeroSearchTool.java`
- `src/main/java/org/barcelonajug/battlecontender/ai/tools/ArenaManagementTool.java`
- `src/main/java/org/barcelonajug/battlecontender/ai/BattleAdvisorService.java`

The `main` branch keeps these steps as attendee-facing TODOs. The `solution` branch implements the same workflow end to end.

You can verify your implementation by running:

```bash
./mvnw clean test
```

## ⚙️ Setup Instructions

### Prerequisites

- **Java 25** installed on your machine.
- An API Key for your preferred AI Provider.

### 1. Choose Your AI Provider

This project is configured with Maven profiles to support multiple AI providers. You only need to use **one**.

| Profile          | Provider                          | Required Config                            |
| :--------------- | :-------------------------------- | :----------------------------------------- |
| `openai`         | OpenAI                            | `OPENAI_API_KEY`                           |
| `vertex-ai`      | Google Vertex AI                  | `GCP_PROJECT_ID` & `GCP_LOCATION`          |
| `anthropic`      | Anthropic                         | `ANTHROPIC_API_KEY`                        |
| `ollama`         | Ollama (Local)                    | _Make sure Ollama is running (`llama3.2`)_ |
| `github-copilot` | GitHub Models (OpenAI-compatible) | `GITHUB_TOKEN`                             |

### 2. Configure Credentials

Create a `.env` file in the root of the project to set your API key.

**For OpenAI:**

Create a `.env` file and add:

```properties
OPENAI_API_KEY=your-api-key
```

**For Vertex AI:**

Create a `.env` file and add:

```properties
GCP_PROJECT_ID=your-project
GCP_LOCATION=europe-west1
# Ensure you are authenticated with gcloud: `gcloud auth application-default login`
```

**For Anthropic:**

Create a `.env` file and add:

```properties
ANTHROPIC_API_KEY=your-api-key
```

**For GitHub Models (GitHub Copilot):**

Create a `.env` file and add:

```properties
GITHUB_TOKEN=your-github-token
```

### 3. Run the Application

Use the Maven wrapper to run the project with the desired profile:

```bash
./mvnw spring-boot:run -Popenai
./mvnw spring-boot:run -Pvertex-ai
./mvnw spring-boot:run -Panthropic
./mvnw spring-boot:run -Pollama
./mvnw spring-boot:run -Pgithub-copilot
```

### 4. Play the Game

Once the application starts, navigate to:
**<http://localhost:8080>**

This application connects to the live Superhero Battle Arena deployed at [https://superhero-battle-arena.barcelonajug.org/](https://superhero-battle-arena.barcelonajug.org/):

1. Register your team (the Team ID is persisted in local storage across browser refreshes).
2. Ensure the active Session is loaded.
3. Select an Open Round.
4. Click the **"✨ Optimize with AI"** button to see your AI code in action!

Good luck, Contender!
