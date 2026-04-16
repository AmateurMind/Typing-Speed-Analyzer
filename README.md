# Typing Speed Calculator (React + Spring Boot + MySQL)

## Overview
This project is a full-stack typing speed test app with:
- Frontend: React (Vite)
- Backend: Spring Boot (REST API)
- Database: MySQL (Railway in production)

It supports:
- Sentence mode and 30-second timed mode
- Live WPM + accuracy
- Auto-save score at test completion
- Toast notifications (React Toastify)
- Per-user score isolation using a browser-generated `userId`

---

## Frontend (What It Does)
Main file: `frontend/src/App.jsx`

### Core behavior
- Renders a prompt and typing input area
- Starts timed test counter when typing begins
- Calculates:
  - WPM in real-time
  - Word-level accuracy in real-time
- Ends test:
  - Sentence mode: when prompt text is completed
  - Timed mode: after 30 seconds

### Save + refresh flow
When a test ends:
1. Frontend computes final WPM and final accuracy
2. Sends `POST /result` with:
   - `wpm`
   - `accuracy`
   - `userId`
3. Refreshes latest scores via `GET /scores?userId=...`
4. Shows toast:
   - completion toast
   - save success toast

### Per-user logic
- Frontend creates/persists a `userId` in browser `localStorage`
- Same browser + same domain => same `userId`
- Scores shown are only for that `userId`

---

## Backend (API + PRD-Style Spec)
Main files:
- `backend/src/main/java/com/typing/backend/controller/TypingController.java`
- `backend/src/main/java/com/typing/backend/service/TypingService.java`

### Product requirements implemented
- Store score records with:
  - WPM
  - Accuracy
  - Timestamp (date + time)
  - User ID
- Return latest records for the requesting user only
- Support root health/info endpoint for deployed URL checks

### API endpoints
- `GET /`
  - Returns API status + route hints
- `GET /prompt`
  - Returns a random base prompt
- `POST /result`
  - Request JSON:
    ```json
    {
      "wpm": 58.42,
      "accuracy": 96.5,
      "userId": "user-171..."
    }
    ```
  - Saves record to DB
- `GET /scores?userId=<id>`
  - Returns latest 10 scores for that user

### Validation rules
- `wpm` must be numeric
- `accuracy` must be numeric
- `userId` must be non-empty string

---

## Database Design (Railway MySQL)
Database service: Railway MySQL (`MySQL` service)

### Table used
`typing_speed`

### Columns
- `id` (BIGINT, PK, auto increment)
- `user_id` (VARCHAR, required)
- `speed` (DOUBLE, required) -> WPM
- `accuracy` (DOUBLE, nullable but backfilled)
- `tested_at` (TIMESTAMP, required)

### Query behavior
- Insert:
  - stores `user_id`, `speed`, `accuracy`, current timestamp
- Fetch:
  - `SELECT ... WHERE user_id = ? ORDER BY tested_at DESC LIMIT 10`

### Migration/backfill safety in service
On startup/query, backend ensures schema and compatibility:
- creates table if missing
- adds missing columns when needed
- backfills null `accuracy` (legacy rows)
- backfills missing timestamp/user fields

---

## Deployment

### Frontend
- Hosted on Vercel
- Uses environment variable:
  - `VITE_API_BASE_URL`

Example:
```env
VITE_API_BASE_URL=https://typing-speed-backend-production.up.railway.app
```

### Backend
- Hosted on Railway(https://typing-speed-backend-production.up.railway.app/)
- Uses `Dockerfile` in `backend/`
- Uses Railway environment vars for datasource:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`

---

## Local Development

### 1. Run backend
From `backend/`:
```bash
./mvnw spring-boot:run
```

### 2. Run frontend
From `frontend/`:
```bash
npm install
npm run dev
```

Optional local env in `frontend/.env`:
```env
VITE_API_BASE_URL=http://localhost:8080
```

---

## Notes
- If deployed site shows empty latest scores, verify you are on the same domain/origin used before (because `userId` is browser-origin scoped).
- Root backend URL should return API info JSON, not Whitelabel.
