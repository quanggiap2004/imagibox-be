# ImagiBox
> **Where Imagination Meets AI**

An AI-driven storytelling engine built with **Spring Boot** and **React**. Integrates **Gemini 2.0** and **Stable Diffusion** to convert simple sketches into immersive, illustrated narratives. Features real-time content generation, strict safety algorithms, and JWT-secured parent dashboards.

## Key Capabilities

- **Scalable Architecture**: Engineered with Spring Boot 3.2 and Redis to handle high-concurrency request rate limiting and caching.
- **Secure Environment**: Implements multi-layer content safety filtering (Text + Image) ensuring a 100% child-friendly digital experience.
- **Advanced AI Integration**: Seamlessly orchestrates multimodal AI pipelines, combining LLM narrative logic with generative image synthesis.

## FE Repository
- https://github.com/quanggiap2004/imagibox-fe

## Tech Stack

- Java 17 + Spring Boot 3.2.1
- PostgreSQL (using Supabase)
- Redis for caching/rate limiting
- Spring AI + Gemini 2.0
- Nanobanana for images
- Cloudinary storage
- Flyway migrations

## Setup

**Prerequisites:**
- Java 17+
- Maven 3.8+
- PostgreSQL (Supabase account)
- Redis running locally
- API keys for Gemini, Stable Diffusion, Cloudinary

**Quick start:**

```bash
git clone <repository-url>
cd ImagiBox_BE

# Copy and edit env file
cp .env.example .env
# Fill in your actual API keys and database credentials

# Run migrations
mvn flyway:migrate

# Build and run
mvn clean install
mvn spring-boot:run
```

Server starts at `http://localhost:8080`

## API Docs

Swagger UI: `http://localhost:8080/swagger-ui.html`

### Main Endpoints

**Auth:**
- POST `/api/v1/auth/register` - parent signup
- POST `/api/v1/auth/login` - login
- POST `/api/v1/auth/kids` - create kid account (parent only)
- GET `/api/v1/auth/kids` - list kids

**Stories:**
- POST `/api/v1/stories/generate-one-shot` - create story (with optional sketch upload)
- POST `/api/v1/stories/{id}/chapters/next` - continue interactive story
- GET `/api/v1/stories` - list stories (paginated)
- GET `/api/v1/stories/{id}` - get story details
- DELETE `/api/v1/stories/{id}` - delete story

**Analytics (Parents only):**
- GET `/api/v1/analytics/dashboard?userId={id}` - kid's dashboard
- GET `/api/v1/analytics/mood-distribution?userId={id}` - mood chart

## Database

Using PostgreSQL ENUMs for type safety:
- `users` - parent/kid accounts (self-referencing for parent_id)
- `stories` - ONE_SHOT or INTERACTIVE modes
- `chapters` - story chapters (JSONB content)
- `mood_tags` - for analytics
- `characters` - saved character profiles (WIP)

## Architecture

Standard 3-layer setup:
- **Controllers** - REST endpoints
- **Services** - business logic
- **Repositories** - data access

Key services:
- `AuthService` - user management
- `StoryService` - story generation flow
- `AiService` - Gemini integration
- `ImageService` - Cloudinary + SD image gen
- `ContentSafetyService` - content filtering
- `RateLimitService` - Redis quota tracking
- `AnalyticsService` - parent dashboard
