/**
 * TrailGuide REST API Proxy Server
 * 
 * This Express server acts as a REST API proxy between the Android app and Supabase.
 * It provides CRUD endpoints for trails, user authentication, and favorites management.
 * 
 * Architecture:
 * - Express.js for REST API routing
 * - Supabase client for database operations
 * - Middleware for logging, security, and CORS
 */

require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const bodyParser = require('body-parser');
const { createClient } = require('@supabase/supabase-js');

// Initialize Express app
const app = express();
const PORT = process.env.PORT || 3000;

// Initialize Supabase client
const supabase = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_ANON_KEY
);

// ============================================================================
// Middleware Configuration
// ============================================================================

// Security middleware
app.use(helmet());

// CORS configuration - allow Android app to access API
app.use(cors({
  origin: '*', // In production, restrict to specific origins
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));

// Request logging
app.use(morgan('dev'));

// Body parsing middleware
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// ============================================================================
// API Routes
// ============================================================================

/**
 * Health check endpoint
 * Also available at /api/health for consistency with other endpoints
 */
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    message: 'TrailGuide API is running',
    timestamp: new Date().toISOString(),
    version: process.env.API_VERSION || 'v1'
  });
});

// Health check at /api/health for Android app
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    message: 'TrailGuide API is running',
    timestamp: new Date().toISOString(),
    version: process.env.API_VERSION || 'v1'
  });
});

// ----------------------------------------------------------------------------
// Trail Endpoints
// ----------------------------------------------------------------------------

/**
 * GET /api/trails
 * Fetch all trails from the database
 */
app.get('/api/trails', async (req, res) => {
  try {
    const { data, error } = await supabase
      .from('trails')
      .select('*')
      .order('name', { ascending: true });
    
    if (error) throw error;
    
    res.json(data || []);
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
 * Fetch a specific trail by ID
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
        error: 'Trail not found',
        message: `No trail found with ID: ${id}`
      });
    }
    
    res.json(data);
  } catch (error) {
    console.error('Error fetching trail:', error);
    res.status(500).json({
      error: 'Failed to fetch trail',
      message: error.message
    });
  }
});

/**
 * GET /api/trails/search
 * Search trails with filters
 */
app.get('/api/trails/search', async (req, res) => {
  try {
    const { q, difficulty, maxDistance } = req.query;
    
    let query = supabase.from('trails').select('*');
    
    // Apply search query filter
    if (q) {
      query = query.or(`name.ilike.%${q}%,city.ilike.%${q}%`);
    }
    
    // Apply difficulty filter
    if (difficulty && difficulty !== 'any') {
      query = query.eq('difficulty', difficulty.toLowerCase());
    }
    
    // Apply distance filter
    if (maxDistance) {
      query = query.lte('distance_km', parseFloat(maxDistance));
    }
    
    const { data, error } = await query.order('name', { ascending: true });
    
    if (error) throw error;
    
    res.json(data || []);
  } catch (error) {
    console.error('Error searching trails:', error);
    res.status(500).json({
      error: 'Failed to search trails',
      message: error.message
    });
  }
});

/**
 * POST /api/trails
 * Create a new trail
 */
app.post('/api/trails', async (req, res) => {
  try {
    const trailData = req.body;
    
    // Validate required fields
    const requiredFields = ['name', 'city', 'latitude', 'longitude', 'distanceKm', 'elevationM', 'difficulty', 'rating'];
    const missingFields = requiredFields.filter(field => !trailData[field]);
    
    if (missingFields.length > 0) {
      return res.status(400).json({
        error: 'Missing required fields',
        missing: missingFields
      });
    }
    
    const { data, error } = await supabase
      .from('trails')
      .insert([{
        name: trailData.name,
        city: trailData.city,
        lat: trailData.latitude,
        lon: trailData.longitude,
        distance_km: trailData.distanceKm,
        elevation_m: trailData.elevationM,
        difficulty: trailData.difficulty.toLowerCase(),
        rating: trailData.rating,
        image: trailData.imageUrl,
        tags: trailData.tags || [],
        description: trailData.description
      }])
      .select()
      .single();
    
    if (error) throw error;
    
    res.status(201).json(data);
  } catch (error) {
    console.error('Error creating trail:', error);
    res.status(500).json({
      error: 'Failed to create trail',
      message: error.message
    });
  }
});

/**
 * PUT /api/trails/:id
 * Update an existing trail
 */
app.put('/api/trails/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const trailData = req.body;
    
    const { data, error } = await supabase
      .from('trails')
      .update({
        name: trailData.name,
        city: trailData.city,
        lat: trailData.latitude,
        lon: trailData.longitude,
        distance_km: trailData.distanceKm,
        elevation_m: trailData.elevationM,
        difficulty: trailData.difficulty.toLowerCase(),
        rating: trailData.rating,
        image: trailData.imageUrl,
        tags: trailData.tags,
        description: trailData.description
      })
      .eq('id', id)
      .select()
      .single();
    
    if (error) throw error;
    
    if (!data) {
      return res.status(404).json({
        error: 'Trail not found',
        message: `No trail found with ID: ${id}`
      });
    }
    
    res.json(data);
  } catch (error) {
    console.error('Error updating trail:', error);
    res.status(500).json({
      error: 'Failed to update trail',
      message: error.message
    });
  }
});

/**
 * DELETE /api/trails/:id
 * Delete a trail
 */
app.delete('/api/trails/:id', async (req, res) => {
  try {
    const { id } = req.params;
    
    const { error } = await supabase
      .from('trails')
      .delete()
      .eq('id', id);
    
    if (error) throw error;
    
    res.status(204).send();
  } catch (error) {
    console.error('Error deleting trail:', error);
    res.status(500).json({
      error: 'Failed to delete trail',
      message: error.message
    });
  }
});

// ----------------------------------------------------------------------------
// Favorites Endpoints
// ----------------------------------------------------------------------------

/**
 * POST /api/trails/:id/favorite
 * Toggle favorite status for a trail
 */
app.post('/api/trails/:id/favorite', async (req, res) => {
  try {
    const { id } = req.params;
    const { favorite } = req.body;
    
    // In a real implementation, this would be tied to user accounts
    // For now, we'll just acknowledge the request
    
    res.json({
      success: true,
      trailId: id,
      favorite: favorite
    });
  } catch (error) {
    console.error('Error toggling favorite:', error);
    res.status(500).json({
      error: 'Failed to toggle favorite',
      message: error.message
    });
  }
});

/**
 * GET /api/trails/favorites
 * Get user's favorite trails
 */
app.get('/api/trails/favorites', async (req, res) => {
  try {
    // In a real implementation, this would filter by user ID
    // For now, return a subset of trails
    const { data, error } = await supabase
      .from('trails')
      .select('*')
      .limit(5);
    
    if (error) throw error;
    
    res.json(data || []);
  } catch (error) {
    console.error('Error fetching favorites:', error);
    res.status(500).json({
      error: 'Failed to fetch favorites',
      message: error.message
    });
  }
});

// ----------------------------------------------------------------------------
// Authentication Endpoints (Basic implementation)
// ----------------------------------------------------------------------------

/**
 * POST /api/auth/login
 * Login endpoint (placeholder)
 */
app.post('/api/auth/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    
    // Firebase handles authentication in the Android app
    // This endpoint is for API documentation purposes
    
    res.json({
      message: 'Authentication is handled by Firebase in the Android app',
      email: email
    });
  } catch (error) {
    console.error('Error during login:', error);
    res.status(500).json({
      error: 'Login failed',
      message: error.message
    });
  }
});

/**
 * GET /api/auth/user
 * Get current user (placeholder)
 */
app.get('/api/auth/user', async (req, res) => {
  res.json({
    message: 'User authentication is handled by Firebase in the Android app'
  });
});

// ============================================================================
// Error Handling
// ============================================================================

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    error: 'Not Found',
    message: `Route ${req.method} ${req.url} not found`,
    availableRoutes: [
      'GET /api/trails',
      'GET /api/trails/:id',
      'GET /api/trails/search',
      'POST /api/trails',
      'PUT /api/trails/:id',
      'DELETE /api/trails/:id',
      'POST /api/trails/:id/favorite',
      'GET /api/trails/favorites'
    ]
  });
});

// Global error handler
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
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
  console.log(`Server running on: http://localhost:${PORT}`);
  console.log(`Health check: http://localhost:${PORT}/health`);
  console.log(`Supabase URL: ${process.env.SUPABASE_URL}`);
  console.log('='.repeat(60));
  console.log('Available endpoints:');
  console.log('  GET    /api/trails');
  console.log('  GET    /api/trails/:id');
  console.log('  GET    /api/trails/search?q=&difficulty=&maxDistance=');
  console.log('  POST   /api/trails');
  console.log('  PUT    /api/trails/:id');
  console.log('  DELETE /api/trails/:id');
  console.log('  POST   /api/trails/:id/favorite');
  console.log('  GET    /api/trails/favorites');
  console.log('='.repeat(60));
});

module.exports = app;

