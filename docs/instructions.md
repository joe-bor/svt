# Silicon Valley Trail - REACH Take Home Assessment

## Summary

Build a small, replayable game called Silicon Valley Trail (similar to the game "Oregon Trail"). You’ll guide a scrappy startup team on a journey between any two locations, such as from San Jose to San Francisco. Each “day,” the team travels, consumes resources, encounters events, and makes choices. The twist: your game must incorporate at least one public API to influence gameplay.

We’re not judging art or UI polish (you can make your application a CLI, web-based, mobile-based, VR-based, etc.); we’re looking for thoughtful design, clean code, good tradeoffs, and how you integrate external data. Creative additions are a bonus.

---

## Core Requirements

### 1. Game loop

- A sequence of “days” or “turns.”
- Each turn: choose an action with a minimum of 3 options (travel, rest, hackathon, pitch VCs, detour for supplies, etc.), then resolve outcomes.

### 2. Resources & state

- Track at least 3 meaningful stats. Examples:
  - Cash
  - Compute Credits
  - Team Morale
  - Coffee
  - Bug Count
  - Tech Debt
  - Hype
  - Culture
- Winning happens when the team successfully reaches the destination.
- Define losing conditions (e.g., cash hits zero, morale collapse, run out of coffee causing a loss in two turns if not replenished, etc.).

### 3. Map

- Represent progress across at least 10 real, physical locations.
- A displayed map is not a requirement.

### 4. Events & choices

- Implement a diverse set of events (some conditional on API data).
- Provide consequential choices (trade one resource for another, risk vs. reward, etc.).
- An event should happen at each location after movement. Events should be at least semi-random, if not completely randomized

### 5. Public Web API integration (at least 1 required)

- Use live or cached responses to change gameplay (not just display data).
- Provide a simple fallback (mock data) so the game runs without secrets or when offline.

---

## Same Public APIs

You can choose anything public and free-tier friendly. A few ideas:

### Weather (e.g., Open-Meteo, OpenWeatherMap):

- Weather affects travel distance, bug rate (on-call stress), or coffee consumption.
- Example: Rainy day → slower travel; heat wave → morale drop unless you “buy snacks.”

### Mapping / Geocoding / Routing (e.g., OpenStreetMap/Nominatim, Mapbox, Google Maps Directions):

- Distance between checkpoints sets daily travel cost, traffic or terrain changes resource drains.
- Example: Long leg today → extra compute credits to auto-scale, or risk "downtime" leading to increased bugs and decreased hype.

### Flight / Plane tracking (e.g., OpenSky Network):

- “Supply drops” arrive only if a nearby aircraft is detected, bigger drops if a cargo flight passes.
- Example: If an aircraft at or near your current latitude/longitude in the last hour → increase coffee and cash.

### News / Trends (e.g., Hacker News Algolia):

- Spikes in “Hype” if your keyword trends, risk burnout if hype is high and morale is low.
- Example: "tacos" are trending on Hacker News → increase hype and culture.

**Note:** Keep API keys out of source control. Use environment variables and supply a .env.example.

---

## Required Features

### 1. Testing:

at least a few unit tests covering critical logic (events, resource updates, win/lose).

### 2. Documentation:

a concise README (see Deliverables) and Design Notes explaining key choices and tradeoffs.

### 3. Decency & safety:

handle API errors/timeouts gracefully, no hard-coded secrets, no collection of any personal user information.

---

## Deliverables

- Source code in a public repo (any language/framework)
- Screen recording of or url to the working application.

### README.md including:

- Quick start - fully explain how to get your application running from a fresh machine.
- How to set API keys, how to run with mocks
- Brief architecture overview and dependency list
- How to run tests
- Example commands/inputs
- How, if any, AI was utilized in the creation of the code and/or how the code utilizes AI, if at all

---

### Design Notes (can be a README section):

- Game loop & balance approach
- Why you chose your API(s) and how they affect gameplay
- Data modeling (state, events, persistence)
- Error handling (network failures, rate limits)
- Tradeoffs and “if I had more time”

---

- Tests (unit or integration) for core mechanics
