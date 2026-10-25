# AI Provider API Keys & Cost Safety Guide

This guide provides instructions on how to obtain API keys for all supported Spring AI models in **Battle Contender**, prioritize free-tier options, configure spending limits, and safely clean up credentials after the workshop.

---

## 🎯 Quick Comparison of AI Providers

| Profile          | Provider             |     Free Option Available?     | Best For                                  | Required Configuration           |
| :--------------- | :------------------- | :----------------------------: | :---------------------------------------- | :------------------------------- |
| `github-copilot` | **GitHub Models**    |      **YES (100% Free)**       | Zero setup cost, no credit card required  | `GITHUB_TOKEN`                   |
| `ollama`         | **Ollama**           |      **YES (100% Free)**       | Offline / local execution on your machine | None (`localhost:11434`)         |
| `vertex-ai`      | **Google Vertex AI** |   **YES ($300 Free Trial)**    | Google Cloud ecosystem                    | `GCP_PROJECT_ID`, `GCP_LOCATION` |
| `openai`         | **OpenAI**           | Pay-as-you-go (~$0.01 per run) | High availability and fast completions    | `OPENAI_API_KEY`                 |
| `anthropic`      | **Anthropic**        | Pay-as-you-go (~$0.01 per run) | Reasoning-heavy models                    | `ANTHROPIC_API_KEY`              |

---

## 1. 🆓 GitHub Models (`github-copilot` profile) — Recommended Free Option

GitHub provides **free access** to AI models (including `gpt-4o-mini`) via Azure AI inference endpoints for personal GitHub accounts. **No credit card is required.**

### How to Get Your Token:

1. Log in to [GitHub](https://github.com).
2. Go to [GitHub Personal Access Tokens (Classic)](https://github.com/settings/tokens).
3. Click **Generate new token** -> **Generate new token (classic)**.
4. Give it a note (e.g., `battle-contender-workshop`).
5. Set expiration to **7 days** (or 1 day).
6. Under scopes: no special private repository permissions are strictly required for the model endpoint, but selecting `read:user` or basic public access is standard.
7. Click **Generate token** and copy the resulting string (`ghp_...`).

### How to Configure:

In your `.env` file at the root of `battle-squad`:

```properties
GITHUB_TOKEN=ghp_yourCopilotTokenHere
```

Run with:

```bash
./mvnw spring-boot:run -Pgithub-copilot
```

---

## 2. 💻 Ollama (`ollama` profile) — 100% Free & Local

If you want to run models completely offline on your own GPU/CPU without any cloud accounts:

### How to Install and Run:

1. Download and install Ollama from [https://ollama.com/download](https://ollama.com/download).
2. Open your terminal and pull the recommended lightweight model:
   ```bash
   ollama pull llama3.2
   ```
3. Start the Ollama server:
   ```bash
   ollama serve
   ```
4. Verify Ollama is listening by visiting [http://localhost:11434](http://localhost:11434).

### How to Configure:

No `.env` keys are needed. The default port is `11434`.

Run with:

```bash
./mvnw spring-boot:run -Pollama
```

---

## 3. ☁️ Google Cloud Vertex AI (`vertex-ai` profile)

Google Cloud provides a \$300 free trial credit for new accounts across 90 days.

### How to Get Your Access:

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new Google Cloud Project (e.g. `battle-contender-ai`).
3. Enable the **Vertex AI API**:
   - In the search bar, search for **Vertex AI API** and click **Enable**.
4. Authenticate your local development terminal using Application Default Credentials (ADC):
   ```bash
   gcloud auth application-default login
   ```
5. Set your active quota project:
   ```bash
   gcloud auth application-default set-quota-project YOUR_PROJECT_ID
   ```

### How to Configure:

In your `.env` file:

```properties
GCP_PROJECT_ID=your-project-id
GCP_LOCATION=europe-west1
```

Run with:

```bash
./mvnw spring-boot:run -Pvertex-ai
```

---

## 4. ⚡ OpenAI (`openai` profile)

OpenAI models (`gpt-5.4-mini`) are very low cost (fractions of a cent per request), but require an active OpenAI platform account.

### How to Obtain an API Key:

1. Sign up or log in at [https://platform.openai.com/](https://platform.openai.com/).
2. Navigate to [API Keys](https://platform.openai.com/api-keys).
3. Click **Create new secret key**.
4. Name the key (e.g., `battle-squad-workshop`) and click **Create secret key**.
5. Copy the key (`sk-proj-...`).

### 🛡️ How to Set Spending Limits (Mandatory Safety Step):

1. Navigate to [Billing Limits Settings](https://platform.openai.com/account/limits).
2. Set a **Monthly Budget / Usage Limit** of **$5.00**.
3. Set an **Email Notification threshold** of **$3.00**.
4. Save settings. This guarantees that your account will never be charged beyond \$5.

### How to Configure:

In your `.env` file:

```properties
OPENAI_API_KEY=sk-proj-yourOpenAiKeyHere
```

Run with:

```bash
./mvnw spring-boot:run -Popenai
```

---

## 5. 🧠 Anthropic (`anthropic` profile)

Anthropic powers Claude models (`claude-3-5-haiku-latest`).

### How to Obtain an API Key:

1. Sign up or log in at [https://console.anthropic.com/](https://console.anthropic.com/).
2. Go to **Settings** -> [API Keys](https://console.anthropic.com/settings/keys).
3. Click **Create Key**.
4. Copy the secret key (`sk-ant-...`).

### 🛡️ How to Set Spending Limits:

1. Go to [Billing](https://console.anthropic.com/settings/billing).
2. Under **Spend Limits**, set a **Spend Limit** of **$5.00**.
3. Ensure auto-reload is turned **OFF**.

### How to Configure:

In your `.env` file:

```properties
ANTHROPIC_API_KEY=sk-ant-yourAnthropicKeyHere
```

Run with:

```bash
./mvnw spring-boot:run -Panthropic
```

---

## 🔒 Crucial Security Best Practices

> [!CAUTION]
> Treat API keys like passwords. Anyone with access to your key can consume your quota.

1. **Never commit `.env` to Git**:
   Ensure `.env` remains in `.gitignore`. The repository is already configured to ignore `.env`.
2. **Revoke Keys After the Workshop**:
   Immediately after the workshop concludes:
   - Go to your provider's API key dashboard.
   - Click **Revoke** / **Delete** on the workshop key.
3. **Delete Test Projects**:
   If you created a new Google Cloud project or sandbox account, delete or disable the project once done.
