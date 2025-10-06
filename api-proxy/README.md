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

## Quick Start

### Prerequisites

- Node.js 18.x or higher
- npm or yarn
- Supabase account with database set up

### Installation

1. **Clone the repository**
   ```bash
   cd TrailGuide/api-proxy
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Configure environment variables**
   ```bash
   cp .env_template .env
   ```

4. **Edit `.env` file with your Supabase credentials**
   ```env
   SUPABASE_URL=https://yourproject.supabase.co
   SUPABASE_KEY=your-supabase-anon-key
   PORT=3000
   ```

5. **Start the server**
   ```bash
   npm start
   ```

The server will start on `http://localhost:3000`

## API Endpoints

### Health Check
```
GET /health
```
Returns server status and basic information.

### Trails
```
GET /api/trails
GET /api/trails/:id
GET /api/trails/search
```

**Search Parameters:**
- `q` - Search query (trail name)
- `difficulty` - Filter by difficulty (easy, moderate, hard)
- `maxDistance` - Maximum distance in kilometers

### Favorites
```
POST   /api/users/:id/favourites
GET    /api/users/:id/favourites
DELETE /api/users/:id/favourites/:trailId
```

### Reviews
```
POST /api/trails/:id/reviews
GET  /api/trails/:id/reviews
```

## Database Setup

### Required Tables

The API expects the following Supabase tables:

#### Trails Table
```sql
CREATE TABLE trails (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    difficulty TEXT NOT NULL,
    distance_km REAL NOT NULL,
    elevation_m REAL NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    route_coordinates JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);
```

#### Favourites Table
```sql
CREATE TABLE favourites (
    id SERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    trail_id TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, trail_id)
);
```

### Row Level Security (RLS)

Enable RLS and create policies for the favourites table:

```sql
-- Enable RLS
ALTER TABLE favourites ENABLE ROW LEVEL SECURITY;

-- Create policies
CREATE POLICY "Users can view their own favourites" ON favourites
    FOR SELECT USING (auth.uid()::text = user_id);

CREATE POLICY "Users can insert their own favourites" ON favourites
    FOR INSERT WITH CHECK (auth.uid()::text = user_id);

CREATE POLICY "Users can delete their own favourites" ON favourites
    FOR DELETE USING (auth.uid()::text = user_id);
```

## Environment Variables

Create a `.env` file in the `api-proxy` directory:

```env
# Supabase Configuration
SUPABASE_URL=https://yourproject.supabase.co
SUPABASE_KEY=your-supabase-anon-key

# Server Configuration
PORT=3000
NODE_ENV=development

# Optional: CORS origins (comma-separated)
CORS_ORIGINS=http://localhost:3000,http://10.0.2.2:3000
```

## Development

### Local Development

1. **Start the server in development mode**
   ```bash
   npm run dev
   ```

2. **The server will restart automatically on file changes**

### Testing

```bash
# Test API endpoints
curl http://localhost:3000/health
curl http://localhost:3000/api/trails
```

### Logs

The server uses Morgan middleware for request logging. You'll see logs like:
```
GET /health 200 2.869 ms - 92
GET /api/trails 200 2298.560 ms - 3077
POST /api/users/123/favourites 200 15.234 ms - 45
```

## Deployment

### Local Development
```bash
npm start
```

### Production Deployment

The API server is designed to be deployed on platforms like:
- **Render** (recommended)
- **Heroku**
- **Railway**
- **DigitalOcean**

#### Render Deployment

1. Connect your GitHub repository to Render
2. Create a new Web Service
3. Set build command: `npm install`
4. Set start command: `npm start`
5. Add environment variables in Render dashboard

#### Environment Variables for Production

```env
SUPABASE_URL=https://yourproject.supabase.co
SUPABASE_KEY=your-supabase-anon-key
PORT=3000
NODE_ENV=production
```

## Troubleshooting

### Common Issues

**Database Connection Errors**
- Verify Supabase URL and key are correct
- Check Supabase project is active
- Ensure database tables exist

**CORS Errors**
- Add your app's origin to CORS_ORIGINS
- Check browser developer tools for specific errors

**Authentication Issues**
- Verify Supabase authentication is properly configured
- Check user permissions and RLS policies

### Debug Mode

Enable debug logging by setting:
```env
DEBUG=trailguide:*
```

## API Response Format

### Success Response
```json
{
  "success": true,
  "data": [...],
  "message": "Operation successful"
}
```

### Error Response
```json
{
  "success": false,
  "error": "Error message",
  "code": "ERROR_CODE"
}
```

## Security Considerations

- **API Keys**: Never commit `.env` files to version control
- **CORS**: Configure allowed origins for production
- **Rate Limiting**: Consider adding rate limiting for production use
- **Input Validation**: Validate all input parameters
- **SQL Injection**: Using parameterized queries with Supabase client

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is licensed under the MIT License.