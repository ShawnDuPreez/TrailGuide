// Seed Supabase `trails` table from OpenStreetMap (Overpass API)
// Usage:
//   SUPABASE_URL=... SUPABASE_SERVICE_ROLE=... node scripts/seed-trails.js
// Optional args: bbox as west,south,east,north (default: Pretoria-ish box)

import 'react-native-url-polyfill/auto';
import fetch from 'node-fetch';
import slugify from 'slugify';
import { createClient } from '@supabase/supabase-js';
import * as turf from '@turf/turf';

const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_SERVICE_ROLE = process.env.SUPABASE_SERVICE_ROLE;
if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE) {
  console.error('Missing SUPABASE_URL or SUPABASE_SERVICE_ROLE env vars');
  process.exit(1);
}

const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE, {
  auth: { persistSession: false, autoRefreshToken: false },
});

// Default bbox around Pretoria [west,south,east,north]
const bboxArg = process.argv[2];
const BBOX = bboxArg ? bboxArg.split(',').map((n) => Number(n)) : [28.0, -25.95, 28.45, -25.55];

const overpassQuery = (west, south, east, north) => `
  [out:json][timeout:60];
  (
    way["highway"~"path|footway|track|bridleway|steps"](${south},${west},${north},${east});
    relation["route"="hiking"](${south},${west},${north},${east});
  );
  out body;
  >;
  out skel qt;
`;

function computeLengthKm(geojson) {
  try {
    const lengthMeters = turf.length(geojson, { units: 'kilometers' });
    return Math.round(lengthMeters * 10) / 10;
  } catch {
    return null;
  }
}

function pickCity(tags) {
  return tags?.name || tags?.ref || 'Unknown';
}

function estimateDifficulty(km, ascentM) {
  if (km == null) return 'moderate';
  if (km <= 5) return 'easy';
  if (km <= 12) return 'moderate';
  return 'hard';
}

async function main() {
  const [west, south, east, north] = BBOX;
  console.log('Fetching OSM trails in bbox:', { west, south, east, north });
  const body = overpassQuery(west, south, east, north);
  const res = await fetch('https://overpass-api.de/api/interpreter', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `data=${encodeURIComponent(body)}`,
  });
  const json = await res.json();

  // Convert Overpass JSON to GeoJSON
  const nodesById = new Map();
  for (const el of json.elements) {
    if (el.type === 'node') nodesById.set(el.id, el);
  }

  const features = [];
  for (const el of json.elements) {
    if (el.type === 'way' && el.nodes) {
      const coords = el.nodes
        .map((id) => nodesById.get(id))
        .filter(Boolean)
        .map((n) => [n.lon, n.lat]);
      if (coords.length >= 2) {
        features.push({
          type: 'Feature',
          geometry: { type: 'LineString', coordinates: coords },
          properties: el.tags || {},
        });
      }
    }
  }

  const items = features.slice(0, 200).map((f, idx) => {
    const line = f;
    const km = computeLengthKm(line);
    const center = turf.center(line).geometry.coordinates; // [lon, lat]
    const name = f.properties.name || `Trail ${idx + 1}`;
    const id = slugify(name + '-' + idx, { lower: true, strict: true });
    return {
      id,
      name,
      city: pickCity(f.properties),
      distanceKm: km ?? 5,
      elevationM: 0,
      difficulty: estimateDifficulty(km, 0),
      rating: 4.5,
      image: 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?q=80&w=1200&auto=format&fit=crop',
      lat: center[1],
      lon: center[0],
    };
  });

  console.log(`Upserting ${items.length} trails to Supabase…`);
  const { error } = await supabase.from('trails').upsert(items, { onConflict: 'id' });
  if (error) {
    console.error('Upsert error:', error);
    process.exit(1);
  }
  console.log('Done.');
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});





