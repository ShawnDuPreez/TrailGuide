# TrailGuide REST API Proxy

A Node.js/Express REST API server that acts as a proxy between the TrailGuide Android app and Supabase database.

## Overview

This API provides RESTful endpoints for managing hiking trails, user favorites, and authentication. It connects to a Supabase PostgreSQL database and exposes a clean REST interface for the mobile app.

## Architecture

```
Android App (Kotlin) → REST API (Node.js/Express) → Supabase (PostgreSQL)
```

## Features

- ✅ **CRUD Operations**: Create, Read, Update, Delete trails
- ✅ **Search & Filtering**: Search trails by name, difficulty, and distance
- ✅ **Favorites Management**: Mark trails as favorites
- ✅ **RESTful Design**: Standard HTTP methods and status codes
- ✅ **Error Handling**: Comprehensive error messages
- ✅ **CORS Enabled**: Cross-origin requests supported
- ✅ **Security**: Helmet middleware for security headers
- ✅ **Logging**: Morgan middleware for request logging

## Installation

### Prerequisites

- Node.js 18.x or higher
- npm or yarn
- Supabase account with database set up

### Setup

1. **Clone the repository**
   ```bash
   cd TrailGuide_Android/api-proxy
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Configure environment variables**
   ```bash
   cp .env.example .env
   ```
   
   Edit `.env` file with your Supabase credentials:
   ```
   SUPABASE_URL=your-supabase-url
   SUPABASE_ANON_KEY=your-anon-key
   PORT=3000
   NODE_ENV=development
   ```

4. **Start the server**
   ```bash
   # Development mode with auto-reload
   npm run dev
   
   # Production mode
   npm start
   ```

5. **Verify installation**
   ```bash
   curl http://localhost:3000/health
   ```

## API Endpoints

### Health Check

```http
GET /health
```

Response:
```json
{
  "status": "ok",
  "message": "TrailGuide API is running",
  "timestamp": "2024-10-04T12:00:00.000Z",
  "version": "v1"
}
```

### Trails

#### Get All Trails
```http
GET /api/trails
```

#### Get Trail by ID
```http
GET /api/trails/:id
```

#### Search Trails
```http
GET /api/trails/search?q=magalies&difficulty=moderate&maxDistance=15
```

Query parameters:
- `q` (optional): Search query for name or city
- `difficulty` (optional): `easy`, `moderate`, or `hard`
- `maxDistance` (optional): Maximum distance in kilometers

#### Create Trail
```http
POST /api/trails
Content-Type: application/json

{
  "name": "Mountain View Trail",
  "city": "Pretoria, GP",
  "latitude": -25.8,
  "longitude": 28.2,
  "distanceKm": 12.5,
  "elevationM": 350,
  "difficulty": "moderate",
  "rating": 4.7,
  "imageUrl": "https://example.com/image.jpg",
  "tags": ["scenic", "challenging"],
  "description": "Beautiful mountain trail with panoramic views"
}
```

#### Update Trail
```http
PUT /api/trails/:id
Content-Type: application/json

{
  "name": "Updated Trail Name",
  ...
}
```

#### Delete Trail
```http
DELETE /api/trails/:id
```

### Favorites

#### Toggle Favorite
```http
POST /api/trails/:id/favorite
Content-Type: application/json

{
  "favorite": true
}
```

#### Get Favorite Trails
```http
GET /api/trails/favorites
```

## Database Schema

The API expects a Supabase table named `trails` with the following structure:

```sql
CREATE TABLE trails (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  city TEXT NOT NULL,
  lat DOUBLE PRECISION NOT NULL,
  lon DOUBLE PRECISION NOT NULL,
  distance_km DOUBLE PRECISION NOT NULL,
  elevation_m INTEGER NOT NULL,
  difficulty TEXT NOT NULL CHECK (difficulty IN ('easy', 'moderate', 'hard')),
  rating DOUBLE PRECISION NOT NULL CHECK (rating >= 0 AND rating <= 5),
  image TEXT,
  tags TEXT[],
  description TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);
```

## Error Handling

The API uses standard HTTP status codes:

- `200 OK`: Successful GET, PUT requests
- `201 Created`: Successful POST (resource created)
- `204 No Content`: Successful DELETE
- `400 Bad Request`: Invalid request data
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

Error response format:
```json
{
  "error": "Error type",
  "message": "Detailed error message"
}
```

## Testing

Run the test suite:
```bash
npm test
```

## Deployment

### Deploy to Heroku

1. Create Heroku app:
   ```bash
   heroku create trailguide-api
   ```

2. Set environment variables:
   ```bash
   heroku config:set SUPABASE_URL=your-url
   heroku config:set SUPABASE_ANON_KEY=your-key
   ```

3. Deploy:
   ```bash
   git push heroku main
   ```

### Deploy to Railway/Render

1. Connect your GitHub repository
2. Set environment variables in dashboard
3. Deploy automatically on push

## Development

### Project Structure

```
api-proxy/
├── server.js          # Main Express server
├── package.json       # Dependencies and scripts
├── .env.example       # Environment template
├── .env              # Environment variables (git-ignored)
└── README.md         # This file
```

### Adding New Endpoints

1. Add route handler in `server.js`
2. Implement Supabase query
3. Add error handling
4. Document in this README
5. Add tests

## Security Considerations

- 🔒 API keys stored in environment variables
- 🔒 Helmet middleware for security headers
- 🔒 CORS configured (restrict origins in production)
- 🔒 Input validation on POST/PUT requests
- 🔒 SQL injection prevention via Supabase client

## License

MIT License - Part of TrailGuide Android application

## Support

For issues or questions, please open an issue in the GitHub repository.

