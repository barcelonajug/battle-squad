# Battle Contender: Spring Boot & AI Workshop

![Battle Contender Banner](src/main/resources/static/banner-v2.png)

Welcome to the **Battle Contender** workshop template! In this workshop, you will learn how to build an AI-powered agent using **Spring Boot 4**, **Spring AI 2.0**, and **Java 25**. Your agent will interact with the "Superhero Battle Arena" API to register a team, analyze game constraints, and use an LLM advisor to pick an optimal hero squad within a budget!

## 🚀 Workshop Structure

This repository contains the `main` branch, which serves as the starter project. The backend scaffolding and the HTML frontend (`src/main/resources/static/index.html`) are already built.

Your goal is to implement the **Spring AI** layer.

The branches show three stages of the same workshop:

- `main` contains the attendee-facing TODO implementation.
- `solution` contains the complete single-agent, multi-strategy workflow.
- `solution-subagents` moves each drafting strategy into an isolated Spring AI subagent.

### Workshop Steps

The first seven steps are shared with the other branches. Steps 8-10 show the subagent extension.

1. **Build the tools**
`HeroSearchTool` and `ArenaManagementTool` wrap the arena API. They stay stateless and focused on API access, including round-aware hero search through `/api/heroes/search/advanced`.

2. **Configure `BattleAdvisorService`**
`BattleAdvisorService` coordinates drafting, validates every returned squad, and returns the structured response rendered by the UI.

3. **Add chat memory first**
`MessageChatMemoryAdvisor` is configured before `TodoWriteTool` and scoped by `teamId`, `sessionId`, and `roundNo`, so each optimization run has one durable memory channel.

4. **Add `TodoWriteTool` on top of chat memory**
With that memory in place, `TodoWriteTool` keeps the visible optimization checklist for the current run. In practice: memory holds the run context, and TodoWrite exposes the evolving task list inside that same run.

5. **Use round-aware advanced search**
The solution branch supports the arena v3 round constraints. `RoundSpec` includes allowed roles, genders, races, publishers, and alignments, and `HeroSearchTool` exposes round-aware advanced search so the drafter starts from candidates that already fit the round.

6. **Validate every draft deterministically**
`SquadValidationService` checks team size, budget, required roles, banned tags, and the new allowed-value constraints before a draft is accepted.

7. **Expose multiple draft options**
The final response is `DraftOptionsResponse`, which lets the UI present several candidate squads and mark one recommended option.

8. **Describe specialized drafters in Markdown**
The `agents` resource directory contains balanced, budget, synergy, and aggressive drafter definitions. Each file owns one strategy prompt and declares `model: mini`.

9. **Configure restricted subagents**
`DraftingSubagentConfiguration` initializes the subagents from `ChatClient.Builder`. Their executor receives only `findHeroesForRound`, `getHeroDetails`, `getRoundConstraints`, and `validateSquadForRound`; it does not receive filesystem, shell, web, TodoWrite, or nested Task tools.

10. **Orchestrate parallel drafts**
The parent agent uses Spring AI `TaskTool` to launch all four drafters as background tasks in one turn and `TaskOutputTool` to collect them. The parent keeps `MessageChatMemoryAdvisor` and the visible TodoWrite checklist, while every subagent gets an isolated context window. See the [Spring AI Task Subagents pattern](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents/).

### Subagent Workflow

```text
Browser
  -> BattleAdvisorService
  -> parent ChatClient (memory + TodoWrite)
  -> TaskTool launches four isolated drafting subagents
  -> TaskOutputTool collects four recommendations
  -> SquadValidationService validates each recommendation
  -> DraftOptionsResponse returns four UI options
```

The LLM proposes squads, but it does not decide whether they are legal. Each drafter calls the stateless `validateSquadForRound` tool and gets at most one correction attempt. `SquadValidationService` remains the authoritative check for team size, budget, roles, banned tags, and allowed round attributes both inside that feedback loop and after orchestration.

TodoWrite remains on the parent agent so the UI can show orchestration progress without spending four subagent contexts on duplicate checklists. Missing, duplicate, malformed, or still-invalid results remain visible as invalid options. If every strategy fails validation, the response still contains all four options and their violations, with no recommended strategy.

### Agent Definitions

The Markdown definitions live in `src/main/resources/agents`:

- `balanced-drafter.md`
- `budget-drafter.md`
- `synergy-drafter.md`
- `aggressive-drafter.md`

The parent model is configured by the active Spring AI provider profile. Subagents default to `gpt-5-mini`; override that model for another provider or workshop environment with:

```bash
export BATTLE_SUBAGENT_MODEL=your-provider-model
```

You can verify the implementation by running:

```bash
./mvnw clean test
```

## ⚙️ Setup Instructions

### Prerequisites

* **Java 25** installed on your machine.
* An API Key for your preferred AI Provider.

### 1. Choose Your AI Provider

This project is configured with Maven profiles to support multiple AI providers. You only need to use **one**.

| Profile | Provider | Required Config |
| :--- | :--- | :--- |
| `openai` | OpenAI | `OPENAI_API_KEY` |
| `vertex-ai` | Google Vertex AI | `GCP_PROJECT_ID` & `GCP_LOCATION` |
| `anthropic` | Anthropic | `ANTHROPIC_API_KEY` |
| `ollama` | Ollama (Local) | *Make sure Ollama is running (`llama3.2`)* |
| `github-copilot` | GitHub Models (OpenAI-compatible) | `GITHUB_TOKEN` |

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

1. Register your team.
2. Ensure the active Session is loaded.
3. Select an Open Round.
4. Click the **"✨ Optimize with AI"** button to see your AI code in action!

Good luck, Contender!
