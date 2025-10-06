# TrailGuide Setup Guide 🚀

This guide will help you set up TrailGuide from scratch in under 30 minutes.

## Prerequisites Checklist

Before you begin, ensure you have:

- [ ] **Android Studio** (latest version) or Android SDK
- [ ] **Node.js** 18.x or higher
- [ ] **Java 17** or higher
- [ ] **Git** installed
- [ ] **Android device** or **emulator** for testing

## Step-by-Step Setup

### 1. Clone the Repository (2 minutes)

```bash
git clone https://github.com/ShawnDuPreez/TrailGuide.git
cd TrailGuide
```

### 2. Get API Keys (10 minutes)

#### A. Supabase Setup (5 minutes)
1. Go to [app.supabase.com](https://app.supabase.com)
2. Click "New Project"
3. Choose organization and enter project name
4. Set database password (save this!)
5. Wait for project creation (2-3 minutes)
6. Go to **Settings** → **API**
7. Copy **Project URL** and **anon public** key

#### B. Google Maps Setup (3 minutes)
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create new project or select existing
3. Enable APIs:
   - Maps SDK for Android
   - Places API
   - Geocoding API
4. Go to **Credentials** → **Create Credentials** → **API Key**
5. Copy the API key

#### C. OpenWeather Setup (2 minutes)
1. Go to [OpenWeatherMap](https://openweathermap.org/api)
2. Sign up for free account
3. Go to **API Keys** section
4. Copy your API key

### 3. Configure the App (5 minutes)

Create `local.properties` in the project root:

```properties
# Android SDK Path (adjust for your system)
sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk

# Supabase Configuration
SUPABASE_URL=https://yourproject.supabase.co
SUPABASE_KEY=your-supabase-anon-key

# Google Maps Configuration  
GOOGLE_MAPS_API_KEY=your-google-maps-api-key

# OpenWeather Configuration
OPENWEATHER_API_KEY=your-openweather-api-key
```

### 4. Set Up Database (5 minutes)

1. Go to your Supabase project dashboard
2. Click **SQL Editor**
3. Run this SQL to create tables:

```sql
-- Create trails table
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

-- Create favourites table
CREATE TABLE favourites (
    id SERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    trail_id TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, trail_id)
);

-- Enable Row Level Security
ALTER TABLE favourites ENABLE ROW LEVEL SECURITY;

-- Create RLS policies
CREATE POLICY "Users can view their own favourites" ON favourites
    FOR SELECT USING (auth.uid()::text = user_id);

CREATE POLICY "Users can insert their own favourites" ON favourites
    FOR INSERT WITH CHECK (auth.uid()::text = user_id);

CREATE POLICY "Users can delete their own favourites" ON favourites
    FOR DELETE USING (auth.uid()::text = user_id);

-- Insert sample trail data
INSERT INTO trails (id, name, description, difficulty, distance_km, elevation_m, latitude, longitude) VALUES
('cedar-loop', 'Cedar Loop Trail', 'A beautiful forest trail through cedar groves', 'moderate', 5.2, 150, -34.0522, 18.4241),
('mountain-peak', 'Mountain Peak Trail', 'Challenging hike to the summit', 'hard', 8.5, 450, -34.0622, 18.4341),
('lakeside-walk', 'Lakeside Walk', 'Easy walk around the lake', 'easy', 2.1, 25, -34.0422, 18.4141);
```

4. Go to **Authentication** → **URL Configuration**
5. Add redirect URL: `trailguide://auth-callback`
6. Set site URL: `trailguide://auth-callback`

### 5. Start API Server (3 minutes)

```bash
# Navigate to API directory
cd api-proxy

# Install dependencies
npm install

# Configure environment
cp .env_template .env
```

Edit `.env` file:
```env
SUPABASE_URL=https://yourproject.supabase.co
SUPABASE_KEY=your-supabase-anon-key
PORT=3000
```

Start the server:
```bash
npm start
```

You should see:
```
🚀 TrailGuide REST API Server
Server running on: http://localhost:3000
```

### 6. Build and Run the App (5 minutes)

```bash
# Go back to project root
cd ..

# Build the app
./gradlew assembleDebug

# Install on device/emulator
./gradlew installDebug
```

### 7. Test the App

1. Launch TrailGuide on your device
2. Allow location permissions
3. Try creating an account or signing in with Google
4. Browse trails and test the favorite functionality

## Quick Verification

### Check API Server
```bash
curl http://localhost:3000/health
```
Should return: `{"status":"ok","message":"TrailGuide API Server"}`

### Check Trails Endpoint
```bash
curl http://localhost:3000/api/trails
```
Should return a JSON array of trails.

### Check App Build
```bash
./gradlew assembleDebug
```
Should complete with "BUILD SUCCESSFUL".

## Troubleshooting

### Common Issues

**❌ "Build failed" errors**
- Check that `local.properties` exists and has correct API keys
- Verify Android SDK path is correct
- Run `./gradlew clean` and try again

**❌ "API connection failed"**
- Ensure API server is running on port 3000
- Check that Supabase URL and key are correct
- Verify network connectivity

**❌ "Maps not loading"**
- Verify Google Maps API key is valid
- Check that Maps SDK for Android is enabled in Google Cloud
- Ensure API key restrictions are set correctly

**❌ "Authentication errors"**
- Check Supabase redirect URL is set to `trailguide://auth-callback`
- Verify Google OAuth is enabled in Supabase (if using Google Sign-In)

### Getting Help

1. Check the [main README](README.md) for detailed documentation
2. Look at [API documentation](api-proxy/README.md)
3. Check GitHub Issues for known problems
4. Create a new issue if you're still stuck

## Next Steps

Once you have the app running:

1. **Explore the codebase** - Check out the MVVM architecture
2. **Add new features** - See the contributing guide
3. **Deploy to production** - Follow the deployment guide
4. **Join the community** - Star the repo and share your experience!

## Development Tips

- Use `watch-and-run.bat` for faster development cycles
- Check `docs/` folder for additional documentation
- Use Android Studio's built-in debugger for troubleshooting
- Monitor API server logs for backend issues

---

**🎉 Congratulations! You now have TrailGuide running locally!**

Happy coding and happy hiking! 🥾
