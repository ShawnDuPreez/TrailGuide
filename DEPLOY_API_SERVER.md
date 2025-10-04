# Deploy API Server for Testing

## Goal
Deploy your API server to the cloud so testers can use the APK without running a local server.

---

## ⚡ Quick Deploy Options

### Option 1: Railway (Recommended - Easiest) ⭐

**Free Tier**: $5 credit/month (enough for testing)

#### Steps:

1. **Sign up at [Railway.app](https://railway.app)**
   - Use GitHub account for easy setup

2. **Create New Project**
   - Click "New Project"
   - Select "Deploy from GitHub repo"
   - Connect your GitHub account
   - Select your TrailGuide repository

3. **Configure Build**
   - Railway will auto-detect Node.js
   - Set root directory: `api-proxy`
   - Start command: `npm start`

4. **Add Environment Variables**
   ```
   SUPABASE_URL=https://fvlxrbovmybdbhiwskde.supabase.co
   SUPABASE_KEY=your-anon-key-here
   PORT=3000
   ```

5. **Deploy**
   - Railway will give you a URL like: `https://your-app.railway.app`
   - Copy this URL!

6. **Update Android App**
   - Edit `app/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"https://your-app.railway.app/\"")
   ```

7. **Build Release APK**
   ```bash
   ./gradlew assembleRelease
   ```

---

### Option 2: Render (Free Forever)

**Free Tier**: Always free with some limitations

#### Steps:

1. **Sign up at [Render.com](https://render.com)**

2. **New Web Service**
   - Connect GitHub repo
   - Root directory: `api-proxy`
   - Build command: `npm install`
   - Start command: `npm start`

3. **Environment Variables**
   ```
   SUPABASE_URL=https://fvlxrbovmybdbhiwskde.supabase.co
   SUPABASE_KEY=your-anon-key-here
   ```

4. **Deploy**
   - Get URL: `https://trailguide-api.onrender.com`

5. **Update APK**
   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"https://trailguide-api.onrender.com/\"")
   ```

---

### Option 3: Vercel (Free with Limits)

**Free Tier**: 100GB bandwidth/month

#### Steps:

1. **Sign up at [Vercel.com](https://vercel.com)**

2. **Import Project**
   - Connect GitHub
   - Select repository
   - Framework: Other
   - Root directory: `api-proxy`

3. **Configure**
   - Build command: `npm install`
   - Output directory: (leave empty)

4. **Create `vercel.json` in `api-proxy/`:**
   ```json
   {
     "version": 2,
     "builds": [
       {
         "src": "server.js",
         "use": "@vercel/node"
       }
     ],
     "routes": [
       {
         "src": "/(.*)",
         "dest": "server.js"
       }
     ]
   }
   ```

5. **Add Environment Variables** in Vercel dashboard

6. **Deploy & Get URL**

---

## 🚀 Automated Deployment Script

Create `deploy-and-build.bat` in project root:

```batch
@echo off
echo ========================================
echo   Deploy API & Build Release APK
echo ========================================

REM Check if Railway CLI is installed
where railway >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Railway CLI not found. Install: npm install -g @railway/cli
    pause
    exit /b 1
)

echo [1/3] Deploying API to Railway...
cd api-proxy
call railway up
cd ..

echo [2/3] Getting Railway URL...
cd api-proxy
for /f "delims=" %%i in ('railway domain') do set RAILWAY_URL=%%i
cd ..

echo [3/3] Building APK with URL: %RAILWAY_URL%
call gradlew assembleRelease

echo.
echo ========================================
echo   SUCCESS!
echo ========================================
echo.
echo API Server: https://%RAILWAY_URL%
echo APK Location: app/build/outputs/apk/release/app-release.apk
echo.
echo APK is ready for testing!
pause
```

---

## 📱 Build Configurations

### Development Build (Local Server)
```kotlin
// app/build.gradle.kts
buildTypes {
    debug {
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3000/\"")
    }
}
```

### Release Build (Cloud Server)
```kotlin
// app/build.gradle.kts
buildTypes {
    release {
        buildConfigField("String", "API_BASE_URL", "\"https://your-app.railway.app/\"")
    }
}
```

### Build Commands
```bash
# Development APK (local server)
./gradlew assembleDebug

# Release APK (cloud server)
./gradlew assembleRelease

# Signed APK for Google Play
./gradlew bundleRelease
```

---

## ✅ Testing Checklist

Before sending APK to testers:

- [ ] API server deployed to cloud
- [ ] API server is accessible (test in browser)
- [ ] `build.gradle.kts` points to cloud URL
- [ ] Built release APK with cloud URL
- [ ] Tested APK on clean device
- [ ] Verified all features work
- [ ] No local server needed ✅

---

## 🧪 Test Your Deployment

### 1. Test API Server
```bash
# Test trails endpoint
curl https://your-app.railway.app/api/trails

# Should return JSON with trails data
```

### 2. Build & Test APK
```bash
# Build release APK
./gradlew assembleRelease

# Install on device
adb install -r app/build/outputs/apk/release/app-release.apk

# Launch app
adb shell am start -n com.trailguide.android/.presentation.MainActivity
```

### 3. Check Logs
```bash
# Monitor app logs
adb logcat | grep -i "Retrofit\|TrailApiService"

# Should show requests to your cloud URL
```

---

## 💰 Cost Comparison

| Service | Free Tier | Best For |
|---------|-----------|----------|
| **Railway** | $5 credit/month | Easy setup, student projects |
| **Render** | Free forever* | Long-term free hosting |
| **Vercel** | 100GB bandwidth | Serverless, fast deploys |
| **Heroku** | 550 hours/month** | Traditional hosting |

*With sleep on inactivity  
**Requires credit card verification

---

## 🔐 Security Tips

### For Production:
1. **Don't commit `.env` files**
2. **Use environment variables** in deployment platform
3. **Enable CORS** only for your app domain
4. **Use HTTPS** (automatically provided by all platforms)
5. **Rate limit** API endpoints
6. **Monitor usage** in platform dashboard

### Update `server.js`:
```javascript
// Add CORS for your domain
app.use(cors({
  origin: ['https://your-domain.com', 'capacitor://localhost']
}));

// Add rate limiting
const rateLimit = require('express-rate-limit');
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});
app.use(limiter);
```

---

## 📦 Distribution Workflow

### For Beta Testing:

1. **Deploy API Server**
   ```bash
   cd api-proxy
   railway up  # or your chosen platform
   ```

2. **Update API URL**
   ```kotlin
   // In build.gradle.kts
   buildConfigField("String", "API_BASE_URL", "\"https://your-railway-url.railway.app/\"")
   ```

3. **Build Release APK**
   ```bash
   ./gradlew assembleRelease
   ```

4. **Share APK**
   - Upload to Google Drive, Dropbox, or Firebase App Distribution
   - Send link to testers
   - Include install instructions

### For Google Play:
```bash
# Build signed bundle
./gradlew bundleRelease

# Upload to Google Play Console
```

---

## 🚨 Common Issues

### Issue: "API not responding"
**Solution**: Check deployment logs in Railway/Render dashboard

### Issue: "CORS error"
**Solution**: Add CORS middleware in `server.js`:
```javascript
const cors = require('cors');
app.use(cors());
```

### Issue: "APK still tries to connect to localhost"
**Solution**: Verify `build.gradle.kts` has correct URL and rebuild

### Issue: "Server sleeps after inactivity (Render)"
**Solution**: Add a keep-alive ping or upgrade to paid tier

---

## 📖 Quick Reference

### Railway Commands
```bash
# Install CLI
npm install -g @railway/cli

# Login
railway login

# Deploy
railway up

# View logs
railway logs

# Get URL
railway domain
```

### Render
- Deploy via GitHub integration (auto-deploys on push)
- View logs in dashboard
- Custom domain available

### Vercel
```bash
# Install CLI
npm install -g vercel

# Deploy
vercel

# Production deploy
vercel --prod
```

---

## ✨ Recommended Setup

**For your use case (testing APK):**

1. ✅ Use **Railway** for quick deployment
2. ✅ Free $5 credit is enough for testing phase
3. ✅ Auto-deploys from GitHub
4. ✅ Easy environment variable management
5. ✅ Good logs and monitoring

**Setup time**: ~10 minutes  
**Cost**: Free (with $5 credit)  
**Maintenance**: Zero - auto-deploys from GitHub

---

## 🎯 Next Steps

1. **Choose a platform** (Railway recommended)
2. **Deploy your API server**
3. **Get the deployed URL**
4. **Update `build.gradle.kts`** with the URL
5. **Build release APK**: `./gradlew assembleRelease`
6. **Test the APK** without local server
7. **Share with testers** 🚀

---

**Your testers will just need to:**
1. Download the APK
2. Install it
3. Open the app
4. Everything works! ✅

No server setup required on their end!

---

**Need help?** Follow this guide: [Railway Deployment Tutorial](https://docs.railway.app/getting-started)

