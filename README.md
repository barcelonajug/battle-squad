# Battle Contender: Spring Boot & AI Workshop

![Battle Contender Banner](src/main/resources/static/banner-v2.png)

Welcome to the **Battle Contender** workshop template! In this workshop, you will learn how to build an AI-powered agent using **Spring Boot 4**, **Spring AI 2.0**, and **Java 25**. Your agent will interact with the "Superhero Battle Arena" API to register a team, analyze game constraints, and use an LLM advisor to pick an optimal hero squad within a budget!

## 🚀 Workshop Structure

This repository contains the `main` branch, which serves as the starter project. The backend scaffolding and the HTML frontend (`src/main/resources/static/index.html`) are already built.

Your goal is to implement the **Spring AI** layer.

The `main` branch contains the phased TODO version of the drafting workflow. The `solution` branch contains the full working implementation of the same phases.

For the solution branch, configure `MessageChatMemoryAdvisor` first and then layer `TodoWriteTool` on top of that shared memory. The todo checklist only stays useful if the agent has a durable chat-memory channel backing the optimization run.

### Workshop Steps

This workshop is meant to be followed in this exact order. The README and the implementation now use the same step names and the same overall flow.

1. **Build the tools**
`HeroSearchTool` and `ArenaManagementTool` wrap the arena API. Round-aware hero discovery uses `/api/heroes/search/advanced` and caches the eligible pool for each session and round.

2. **Configure `BattleAdvisorService`**
`BattleAdvisorService` wires the tools into Spring AI, defines the drafting strategies, and returns the structured response that the UI renders.

3. **Scope each drafting conversation**
`MessageChatMemoryAdvisor` keeps the tool-call conversation scoped by team, session, round, and drafting strategy.

4. **Vary candidates across teams**
Each strategy sees a stable team-specific sample drawn from the wider round-eligible hero pool. On open rounds with a tight budget, the sample includes both low-cost and mid-cost heroes. If a model draft is invalid or ignores its sample, the app builds a team-specific alternative and passes it through the same round validator before considering it. Budget alternatives prefer the cheapest eligible heroes; aggressive alternatives prioritize offensive statistics.

5. **Use round-aware advanced search**
The solution branch supports the arena v3 round constraints. `RoundSpec` includes allowed roles, genders, races, publishers, and alignments, and `HeroSearchTool` exposes round-aware advanced search so the drafter starts from candidates that already fit the round.

6. **Validate every draft deterministically**
`SquadValidationService` checks team size, budget, required roles, banned tags, and the new allowed-value constraints before a draft is accepted.

7. **Expose multiple draft options**
The final response is `DraftOptionsResponse`, which lets the UI present several candidate squads and mark one recommended option.

The four drafting strategies run concurrently. The browser receives progress events as each draft is validated and reviewed, then displays the final recommendation. The original `/api/contender/optimize` JSON endpoint remains available; the UI uses `/api/contender/optimize/stream` for progress.

### How To Read This Branch

On `main`, these steps appear as attendee-facing TODOs in [`src/main/java/org/barcelonajug/battlecontender/ai/BattleAdvisorService.java`](src/main/java/org/barcelonajug/battlecontender/ai/BattleAdvisorService.java). On `solution`, the same workflow is implemented end to end.

You can verify the implementation by running:

```bash
./mvnw clean test
```

## ⚙️ Setup Instructions

### Prerequisites

* **Java 25** installed on your machine.
* An API Key for your preferred AI Provider.

To rate valid squad drafts, also set `TYPESAFE_API_KEY`. After deterministic validation, TypeSafe Jev gives each squad one competitiveness rating based on power statistics, roles, and explicit tag bonuses. A map name alone is not treated as a combat rule. The UI shows this single rating and whether it is tentative; confidence remains an internal reliability check. Registered teams are assigned different preferred strategies in registration order, cycling through Balanced, Budget, Synergy, and Aggressive. If a team's preferred draft is invalid or its registration cannot be found, the highest-rated valid draft with usable confidence is recommended; if none has a usable rating, the cheapest valid draft is recommended. Submission always applies the arena rules independently of Jev.

Put `TYPESAFE_API_KEY=...` in `.env.local` at the project root, or export it before starting the app. The local file is ignored by Git. Jev is optional; without a key, the app starts and uses the cost fallback.

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
