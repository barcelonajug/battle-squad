# Battle Contender: Spring Boot & AI Workshop

![Battle Contender Banner](src/main/resources/static/banner-v2.png)

Welcome to the **Battle Contender** workshop template! The project uses **Spring Boot 4**, **Spring AI 2.0**, and **Java 25** to compare AI-driven and deterministic squad drafting against the same Superhero Battle Arena API.

## 🚀 Workshop Structure

This repository contains the `main` branch, which serves as the starter project. The backend scaffolding and the HTML frontend (`src/main/resources/static/index.html`) are already built.

This branch implements the deterministic comparison: the browser and arena integration stay unchanged while Java constructs, scores, and validates squad options.

The `main` branch contains the phased TODO version of the drafting workflow. The `solution` branch contains the full AI implementation. The `deterministic-solution` branch is an alternative implementation that keeps the same controller and UI contract without using an LLM to construct squads.

### Deterministic Solver

`DeterministicSquadSolver` reads the current `RoundSpec`, loads the hero catalog, and applies every hard constraint currently represented by the model:

- exact team size and distinct heroes
- total budget
- required-role minimums and role maximums
- banned tags
- allowed roles, genders, races, publishers, and alignments

It then uses bounded beam search to produce distinct drafts for the same four strategies shown by the AI solution:

- **Balanced:** total power stats, low aggregate stat spread, role diversity, and tag modifiers
- **Budget:** lowest cost first, then total power stats
- **Synergy:** tag modifiers, repeated tag combinations, and total power stats
- **Aggressive:** strength, power, speed, and combat

Every completed squad is checked again by `SquadValidationService`. Equal strategy scores are resolved by lower total cost and then ascending hero IDs, making repeated runs stable. `mapType` is included in the explanation but is not scored because `RoundSpec` does not define map-specific rules.

The search is bounded to keep response times practical. It is deterministic and only marks validator-approved squads as valid or recommended, but it does not claim to find the mathematical global optimum across every possible hero combination. If no squad can be produced for a strategy, the UI receives an invalid diagnostic option with submission disabled.

### Deterministic Flow

This branch follows these steps:

1. **Load round data and heroes**
`DeterministicSquadSolver` reads `RoundSpec` and paginates through the hero catalog.

2. **Apply individual constraints**
Heroes outside the allowed roles, genders, races, publishers, alignments, budget, or tag rules are removed before search.

3. **Search valid combinations**
Bounded beam search constructs exact-size squads while pruning budget violations, role-cap violations, and partial squads that can no longer meet required roles.

4. **Score four strategies**
Balanced, budget, synergy, and aggressive comparators rank the remaining combinations with deterministic cost and hero-ID tie-breakers.

5. **Validate and expose options**
`SquadValidationService` authoritatively validates every selected draft. `DraftOptionsResponse` preserves the existing multi-option UI and marks the cheapest valid option as recommended.

### How To Read This Branch

Start with [`DeterministicSquadSolver`](src/main/java/org/barcelonajug/battlecontender/ai/DeterministicSquadSolver.java), then compare it with the AI orchestration in `solution` or `solution-subagents`. The controller, response records, validator, and browser UI are intentionally shared so the implementation trade-offs can be compared directly.

You can verify the implementation by running:

```bash
./mvnw clean test
```

## ⚙️ Setup Instructions

### Prerequisites

* **Java 25** installed on your machine.
* An API key is only required when running the AI-based branches.

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
4. Click the optimization button to generate deterministic squad options.

Good luck, Contender!
