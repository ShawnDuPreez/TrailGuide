/**
 * TrailGuide REST API Server
 * 
 * This Express server acts as a REST API between the Android app and Supabase.
 * It provides authentication, trails, reviews, favorites, and offline sync.
 */

require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const bodyParser = require('body-parser');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const multer = require('multer');
const { createClient } = require('@supabase/supabase-js');

// Initialize Express app
const app = express();
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';

// Initialize Supabase client
const supabase = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_ANON_KEY
);

// Configure multer for image uploads (memory storage)
const upload = multer({ 
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 } // 5MB limit
});

// ============================================================================
// Middleware Configuration
// ============================================================================

app.use(helmet());
app.use(cors({
  origin: '*',
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));
app.use(morgan('dev'));
app.use(bodyParser.json({ limit: '10mb' }));
app.use(bodyParser.urlencoded({ extended: true, limit: '10mb' }));

// ============================================================================
// Authentication Middleware
// ============================================================================

const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];
  
  if (!token) {
    return res.status(401).json({ error: 'Access token required' });
  }
  
  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({ error: 'Invalid or expired token' });
    }
    req.user = user;
    next();
  });
};

// ============================================================================
// Authentication Endpoints
// ============================================================================

/**
 * POST /api/register
 * Register a new user account
 */
app.post('/api/register', async (req, res) => {
  try {
    const { name, email, password } = req.body;
    
    if (!name || !email || !password) {
      return res.status(400).json({ 
        error: 'Missing required fields',
        required: ['name', 'email', 'password']
      });
    }
    
    // Hash password
    const hashedPassword = await bcrypt.hash(password, 10);
    
    // Create user in Supabase
    const { data: user, error } = await supabase
      .from('users')
      .insert([{
        name: name,
        email: email,
        password: hashedPassword,
        created_at: new Date().toISOString()
      }])
      .select()
      .single();
    
    if (error) {
      if (error.code === '23505') { // Unique violation
        return res.status(409).json({ error: 'Email already exists' });
      }
      throw error;
    }
    
    // Generate JWT token
    const token = jwt.sign(
      { user_id: user.id, email: user.email },
      JWT_SECRET,
      { expiresIn: '7d' }
    );
    
    res.status(201).json({
      user_id: user.id,
      token: token,
      name: user.name,
      email: user.email
    });
  } catch (error) {
    console.error('Registration error:', error);
    res.status(500).json({ 
      error: 'Registration failed',
      message: error.message 
    });
  }
});

/**
 * POST /api/login
 * Login with email and password
 */
app.post('/api/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    
    if (!email || !password) {
      return res.status(400).json({ 
        error: 'Email and password required' 
      });
    }
    
    // Find user
    const { data: user, error } = await supabase
      .from('users')
      .select('*')
      .eq('email', email)
      .single();
    
    if (error || !user) {
      return res.status(401).json({ 
        error: 'Invalid credentials' 
      });
    }
    
    // Verify password
    const validPassword = await bcrypt.compare(password, user.password);
    if (!validPassword) {
      return res.status(401).json({ 
        error: 'Invalid credentials' 
      });
    }
    
    // Generate JWT token
    const token = jwt.sign(
      { user_id: user.id, email: user.email },
      JWT_SECRET,
      { expiresIn: '7d' }
    );
    
    res.json({
      token: token,
      user_id: user.id,
      name: user.name,
      email: user.email
    });
  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({ 
      error: 'Login failed',
      message: error.message 
    });
  }
});

// ============================================================================
// Trail Endpoints
// ============================================================================

/**
 * GET /api/trails
 * Fetch trails with optional proximity filter
 * Query params: ?near=lat,long&distance=20
 */
app.get('/api/trails', async (req, res) => {
  try {
    const { near, distance } = req.query;
    
    let query = supabase
      .from('trails')
      .select('*')
      .order('name', { ascending: true });
    
    const { data, error } = await query;
    
    if (error) throw error;
    
    let trails = data || [];
    
    // Apply proximity filter if coordinates provided
    if (near && distance) {
      const [lat, lon] = near.split(',').map(parseFloat);
      const maxDistance = parseFloat(distance);
      
      trails = trails.filter(trail => {
        const distanceKm = calculateDistance(lat, lon, trail.lat, trail.lon);
        return distanceKm <= maxDistance;
      }).map(trail => ({
        ...trail,
        distance_from_user: calculateDistance(lat, lon, trail.lat, trail.lon)
      }));
      
      // Sort by distance from user
      trails.sort((a, b) => a.distance_from_user - b.distance_from_user);
    }
    
    // Add duration_hours to each trail
    trails = trails.map(trail => ({
      ...trail,
      duration_hours: estimateHikingDuration(trail.distance_km, trail.elevation_m)
    }));
    
    res.json(trails);
  } catch (error) {
    console.error('Error fetching trails:', error);
    res.status(500).json({
      error: 'Failed to fetch trails',
      message: error.message
    });
  }
});

/**
 * GET /api/trails/:id
 * Fetch a specific trail with full details including POIs
 */
app.get('/api/trails/:id', async (req, res) => {
  try {
    const { id } = req.params;
    
    const { data, error } = await supabase
      .from('trails')
      .select('*')
      .eq('id', id)
      .single();
    
    if (error) throw error;
    
    if (!data) {
      return res.status(404).json({
        error: 'Trail not found'
      });
    }
    
    // Add duration
    const trail = {
      ...data,
      duration_hours: estimateHikingDuration(data.distance_km, data.elevation_m),
      pois: data.pois || []
    };
    
    res.json(trail);
  } catch (error) {
    console.error('Error fetching trail:', error);
    res.status(500).json({
      error: 'Failed to fetch trail',
      message: error.message
    });
  }
});

// ============================================================================
// Reviews & Photos Endpoints
// ============================================================================

/**
 * POST /api/trails/:id/reviews
 * Add a review with optional photos for a trail
 */
app.post('/api/trails/:id/reviews', authenticateToken, upload.array('photos', 5), async (req, res) => {
  try {
    const { id: trailId } = req.params;
    const { rating, comment } = req.body;
    const userId = req.user.user_id;
    
    if (!rating || !comment) {
      return res.status(400).json({ 
        error: 'Rating and comment required' 
      });
    }
    
    // Get user info
    const { data: user } = await supabase
      .from('users')
      .select('name')
      .eq('id', userId)
      .single();
    
    const photoUrls = [];
    
    // Upload photos to Supabase Storage if provided
    if (req.files && req.files.length > 0) {
      for (const file of req.files) {
        const fileName = `${userId}_${Date.now()}_${file.originalname}`;
        const filePath = `trail-photos/${trailId}/${fileName}`;
        
        const { data: uploadData, error: uploadError } = await supabase.storage
          .from('trail-images')
          .upload(filePath, file.buffer, {
            contentType: file.mimetype,
            upsert: false
          });
        
        if (!uploadError && uploadData) {
          const { data: urlData } = supabase.storage
            .from('trail-images')
            .getPublicUrl(filePath);
          
          photoUrls.push(urlData.publicUrl);
        }
      }
    }
    
    // Insert review
    const { data: review, error } = await supabase
      .from('reviews')
      .insert([{
        trail_id: trailId,
        user_id: userId,
        user_name: user?.name || 'Anonymous',
        rating: parseFloat(rating),
        comment: comment,
        photos: photoUrls,
        created_at: new Date().toISOString()
      }])
      .select()
      .single();
    
    if (error) throw error;
    
    res.status(201).json({
      review_id: review.id,
      status: 'saved',
      photo_count: photoUrls.length
    });
  } catch (error) {
    console.error('Error creating review:', error);
    res.status(500).json({
      error: 'Failed to create review',
      message: error.message
    });
  }
});

/**
 * GET /api/trails/:id/reviews
 * Get all reviews for a trail
 */
app.get('/api/trails/:id/reviews', async (req, res) => {
  try {
    const { id } = req.params;
    
    const { data, error } = await supabase
      .from('reviews')
      .select('*')
      .eq('trail_id', id)
      .order('created_at', { ascending: false });
    
    if (error) throw error;
    
    res.json(data || []);
  } catch (error) {
    console.error('Error fetching reviews:', error);
    res.status(500).json({
      error: 'Failed to fetch reviews',
      message: error.message
    });
  }
});

// ============================================================================
// Favourites & Collections Endpoints
// ============================================================================

/**
 * POST /api/users/:id/favourites
 * Add a trail to user's favourites
 */
app.post('/api/users/:id/favourites', async (req, res) => {
  try {
    const { id: userId } = req.params;
    const { trail_id } = req.body;
    
    // Authentication check removed for local development
    // if (req.user.user_id !== userId) {
    //   return res.status(403).json({ error: 'Unauthorized' });
    // }
    
    const { error } = await supabase
      .from('favourites')
      .insert([{
        user_id: userId,
        trail_id: trail_id,
        created_at: new Date().toISOString()
      }]);
    
    if (error) {
      if (error.code === '23505') {
        return res.json({ status: 'already_added' });
      }
      throw error;
    }
    
    res.json({ status: 'added' });
  } catch (error) {
    console.error('Error adding favourite:', error);
    res.status(500).json({
      error: 'Failed to add favourite',
      message: error.message
    });
  }
});

/**
 * GET /api/users/:id/favourites
 * Get user's favourite trails
 */
app.get('/api/users/:id/favourites', async (req, res) => {
  try {
    const { id: userId } = req.params;
    
    // Authentication check removed for local development
    // if (req.user.user_id !== userId) {
    //   return res.status(403).json({ error: 'Unauthorized' });
    // }
    
    // Get favorite trail IDs
    const { data: favorites, error } = await supabase
      .from('favourites')
      .select('trail_id')
      .eq('user_id', userId);
    
    if (error) throw error;
    
    if (!favorites || favorites.length === 0) {
      return res.json([]);
    }
    
    // Get full trail data for each favorite
    const trailIds = favorites.map(fav => fav.trail_id);
    const { data: trails, error: trailError } = await supabase
      .from('trails')
      .select('*')
      .in('id', trailIds);
    
    if (trailError) {
      console.error('Error fetching trail data:', trailError);
      // If trails table doesn't exist, return just the IDs
      // The app will need to fetch trail details separately
      return res.json(trailIds.map(id => ({ id })));
    }
    
    res.json(trails || []);
  } catch (error) {
    console.error('Error fetching favourites:', error);
    res.status(500).json({
      error: 'Failed to fetch favourites',
      message: error.message
    });
  }
});

/**
 * DELETE /api/users/:id/favourites/:trailId
 * Remove a trail from favourites
 */
app.delete('/api/users/:id/favourites/:trailId', async (req, res) => {
  try {
    const { id: userId, trailId } = req.params;
    
    // Authentication check removed for local development
    // if (req.user.user_id !== userId) {
    //   return res.status(403).json({ error: 'Unauthorized' });
    // }
    
    const { error } = await supabase
      .from('favourites')
      .delete()
      .eq('user_id', userId)
      .eq('trail_id', trailId);
    
    if (error) throw error;
    
    res.json({ status: 'removed' });
  } catch (error) {
    console.error('Error removing favourite:', error);
    res.status(500).json({
      error: 'Failed to remove favourite',
      message: error.message
    });
  }
});

// ============================================================================
// Offline Sync Endpoint
// ============================================================================

/**
 * POST /api/sync
 * Sync offline data (reviews, activities, favourites)
 */
app.post('/api/sync', authenticateToken, async (req, res) => {
  try {
    const { reviews, favourites, activities } = req.body;
    const userId = req.user.user_id;
    
    const results = {
      reviews_synced: 0,
      favourites_synced: 0,
      activities_synced: 0,
      errors: []
    };
    
    // Sync reviews
    if (reviews && reviews.length > 0) {
      for (const review of reviews) {
        try {
          await supabase.from('reviews').insert([{
            ...review,
            user_id: userId,
            created_at: review.created_at || new Date().toISOString()
          }]);
          results.reviews_synced++;
        } catch (error) {
          results.errors.push({ type: 'review', error: error.message });
        }
      }
    }
    
    // Sync favourites
    if (favourites && favourites.length > 0) {
      for (const fav of favourites) {
        try {
          await supabase.from('favourites').insert([{
            user_id: userId,
            trail_id: fav.trail_id,
            created_at: fav.created_at || new Date().toISOString()
          }]);
          results.favourites_synced++;
        } catch (error) {
          // Ignore duplicates
          if (error.code !== '23505') {
            results.errors.push({ type: 'favourite', error: error.message });
          }
        }
      }
    }
    
    // Sync activities (trail completions)
    if (activities && activities.length > 0) {
      for (const activity of activities) {
        try {
          await supabase.from('activities').insert([{
            user_id: userId,
            trail_id: activity.trail_id,
            duration_minutes: activity.duration_minutes,
            distance_km: activity.distance_km,
            completed_at: activity.completed_at || new Date().toISOString()
          }]);
          results.activities_synced++;
        } catch (error) {
          results.errors.push({ type: 'activity', error: error.message });
        }
      }
    }
    
    res.json({
      status: 'synced',
      ...results
    });
  } catch (error) {
    console.error('Sync error:', error);
    res.status(500).json({
      error: 'Sync failed',
      message: error.message
    });
  }
});

// ============================================================================
// Utility Functions
// ============================================================================

/**
 * Calculate distance between two coordinates using Haversine formula
 */
function calculateDistance(lat1, lon1, lat2, lon2) {
  const R = 6371; // Earth's radius in km
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

/**
 * Estimate hiking duration using Naismith's rule
 */
function estimateHikingDuration(distanceKm, elevationM) {
  const distanceHours = distanceKm / 5.0;
  const elevationHours = elevationM / 600.0;
  return Math.round((distanceHours + elevationHours) * 10) / 10;
}

// ============================================================================
// Health Check
// ============================================================================

app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    message: 'TrailGuide API is running',
    timestamp: new Date().toISOString()
  });
});

// ============================================================================
// Error Handling
// ============================================================================

app.use((req, res) => {
  res.status(404).json({
    error: 'Not Found',
    message: `Route ${req.method} ${req.url} not found`
  });
});

app.use((err, req, res, next) => {
  console.error('Unhandled error:', err);
  res.status(500).json({
    error: 'Internal Server Error',
    message: err.message
  });
});

// ============================================================================
// Server Startup
// ============================================================================

app.listen(PORT, () => {
  console.log('='.repeat(60));
  console.log('🚀 TrailGuide REST API Server');
  console.log('='.repeat(60));
  console.log(`Server running on: http://localhost:${PORT}`);
  console.log('='.repeat(60));
  console.log('📍 Authentication:');
  console.log('  POST   /api/register');
  console.log('  POST   /api/login');
  console.log('');
  console.log('🥾 Trails:');
  console.log('  GET    /api/trails?near=lat,long&distance=20');
  console.log('  GET    /api/trails/:id');
  console.log('');
  console.log('💬 Reviews:');
  console.log('  POST   /api/trails/:id/reviews (with photos)');
  console.log('  GET    /api/trails/:id/reviews');
  console.log('');
  console.log('⭐ Favourites:');
  console.log('  POST   /api/users/:id/favourites');
  console.log('  GET    /api/users/:id/favourites');
  console.log('  DELETE /api/users/:id/favourites/:trailId');
  console.log('');
  console.log('🔄 Offline Sync:');
  console.log('  POST   /api/sync');
  console.log('='.repeat(60));
});

module.exports = app;
