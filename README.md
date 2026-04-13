# SVT API

Project board: https://github.com/users/joe-bor/projects/10

## Running Locally

### Backend

```bash
./mvnw spring-boot:run
# Serves on http://localhost:8080
```

### Frontend (bonus — optional)

A thin React client lives in `frontend/`. It's not required to evaluate the API (Postman works fine), but it provides a visual way to play the game.

```bash
cd frontend
npm install
npm run dev
# Serves on http://localhost:5173, expects the backend at :8080
```

Override the API base with `VITE_API_BASE_URL=http://elsewhere:8080 npm run dev`.

## Documentation

- Database ERD source: [docs/diagrams/svt-flyway-erd.dbml](docs/diagrams/svt-flyway-erd.dbml)
- Interactive dbdiagram: https://dbdiagram.io/d/svt-69d7f39780896296845dfb1b

## Database Diagram

![SVT Flyway ERD](docs/diagrams/svt-flyway-erd.svg)
