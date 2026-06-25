# Battle Contender: Spring Boot & AI Workshop

![Battle Contender Banner](src/main/resources/static/banner-v2.png)

Welcome to the **Battle Contender** workshop template! In this workshop, you will learn how to build an AI-powered agent using **Spring Boot 4**, **Spring AI 2.0**, and **Java 25**. Your agent will interact with the "Superhero Battle Arena" API to register a team, analyze game constraints, and use an LLM advisor to pick an optimal hero squad within a budget!

## 🚀 Workshop Structure

This repository contains the `main` branch, which serves as the starter project. The backend scaffolding and the HTML frontend (`src/main/resources/static/index.html`) are already built.

Your goal is to implement the **Spring AI** layer.

The `main` branch now includes the multi-phase drafting workflow as TODO scaffolding. The `solution` branch contains the full working example, including a visible draft-option list and the complete orchestration flow.

Before wiring `TodoWriteTool`, treat `MessageChatMemoryAdvisor` as a prerequisite in both branches. The todo checklist only remains coherent if the optimization run has a dedicated memory channel scoped by `teamId`, `sessionId`, and `roundNo`.

### What You Will Build

1. **AI Tools (`@Tool`)**: Wrap the Arena API endpoints so the LLM can search for heroes and manage arena sessions.
2. **AI Service Layer**: Configure the Spring AI `ChatClient` with a system prompt and your `@Tool` components.
3. **Structured Output**: Direct the AI to parse its reasoning and squad selection into a Java Record (`SquadRecommendation`).

### TODO Locations

Look for `UnsupportedOperationException("TODO:...")` in the codebase. The primary files you need to modify are:

* `src/main/java/org/barcelonajug/battlecontender/ai/tools/HeroSearchTool.java`
* `src/main/java/org/barcelonajug/battlecontender/ai/tools/ArenaManagementTool.java`
* `src/main/java/org/barcelonajug/battlecontender/ai/BattleAdvisorService.java`

You can verify your implementation by running the provided tests:

```bash
./mvnw clean test
```

### Multi-Phase Workshop Flow

Implement the drafting workflow in these phases:

1. Configure `MessageChatMemoryAdvisor` and scope it by `teamId`, `sessionId`, and `roundNo`.
2. Add `TodoWriteTool` so the agent writes a visible optimization checklist into that shared memory stream.
3. Keep `HeroSearchTool` and `ArenaManagementTool` stateless and focused on API access only.
4. Define multiple drafting strategies and execute them separately.
5. Validate every generated squad deterministically against round constraints.
6. Return `DraftOptionsResponse` so the UI can show several candidate squads and highlight one recommended option.

The `main` branch leaves these phases as TODOs inside [`src/main/java/org/barcelonajug/battlecontender/ai/BattleAdvisorService.java`](/Users/anyulled/Documents/Battle Squad/battle-squad-main/src/main/java/org/barcelonajug/battlecontender/ai/BattleAdvisorService.java). The `solution` branch implements the full workflow.

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
