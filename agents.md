# Agent Guidelines

## Environment & Tooling

### Java Management with SDKMAN

When working with Java in this project (which targets Java 25):

1. **Initialize SDKMAN**:

   ```bash
   source "$HOME/.sdkman/bin/sdkman-init.sh"
   ```

2. **Select Java Version**:

   ```bash
   sdk use java 25-tem
   ```

3. **Check or List Installed Versions**:

   ```bash
   sdk list java
   ```

4. **Set Default Version (if needed)**:
   ```bash
   sdk default java 25-tem
   ```

### Node.js Management with NVM

When working with Node.js and frontend tooling:

1. **Initialize NVM**:

   ```bash
   export NVM_DIR="$HOME/.nvm"
   [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
   ```

2. **Select Node Version**:

   ```bash
   nvm use 22
   ```

3. **Check or List Installed Versions**:

   ```bash
   nvm ls
   ```

4. **Set Default Version (if needed)**:
   ```bash
   nvm alias default 22
   ```

## Development & Verification Workflow

### Backend (Spring Boot & Java 25)

- Ensure Java 25 is active via SDKMAN.
- Run tests:
  ```bash
  ./mvnw test
  ```
- Run full build and verification:
  ```bash
  ./mvnw clean verify -Popenai -B
  ```

### Frontend

- Ensure Node 22 is active via NVM.
- Run unit tests:
  ```bash
  npm test
  ```
- Run Prettier formatting:
  ```bash
  npx prettier --write .
  ```

## Quality Standards

- Ensure all tests pass before completing tasks.
- Keep test coverage at 90% or higher.
- Maintain self-documenting code with clear naming and structure.
- Never add line, doc, or inline comments explaining WHAT code does (only explain non-obvious WHY decisions).
- Adhere to SOLID, DRY, KISS, YAGNI, Law of Demeter, and Tell, Don't Ask principles.
- Always run linting and Prettier upon finishing changes.
