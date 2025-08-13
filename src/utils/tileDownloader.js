import * as FileSystem from 'expo-file-system';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { OSM_TILE_URL_TEMPLATE } from '../config';

const TILES_DIR = FileSystem.documentDirectory + 'tiles';
const OFFLINE_PACKS_KEY = 'offlinePacks';

export function getTilesBaseDir() {
  return TILES_DIR;
}

export async function ensureDirAsync(dirUri) {
  const info = await FileSystem.getInfoAsync(dirUri);
  if (!info.exists) {
    await FileSystem.makeDirectoryAsync(dirUri, { intermediates: true });
  }
}

export function latLonToTileXY(latitude, longitude, zoom) {
  const latRad = (latitude * Math.PI) / 180;
  const n = Math.pow(2, zoom);
  const xTile = Math.floor(((longitude + 180) / 360) * n);
  const yTile = Math.floor(
    (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * n
  );
  return { x: xTile, y: yTile };
}

export function bboxToTileRange(bbox, zoom) {
  const { north, south, east, west } = bbox;
  const { x: xMin, y: yMin } = latLonToTileXY(north, west, zoom);
  const { x: xMax, y: yMax } = latLonToTileXY(south, east, zoom);
  return {
    xMin: Math.min(xMin, xMax),
    xMax: Math.max(xMin, xMax),
    yMin: Math.min(yMin, yMax),
    yMax: Math.max(yMin, yMax),
  };
}

function replaceTemplate(template, z, x, y) {
  return template.replace('{z}', String(z)).replace('{x}', String(x)).replace('{y}', String(y));
}

async function downloadTile(z, x, y) {
  const src = replaceTemplate(OSM_TILE_URL_TEMPLATE, z, x, y);
  const zDir = `${TILES_DIR}/${z}`;
  const xDir = `${zDir}/${x}`;
  const dest = `${xDir}/${y}.png`;
  await ensureDirAsync(xDir);
  const existing = await FileSystem.getInfoAsync(dest);
  if (existing.exists) return { skipped: true, uri: dest };
  await FileSystem.downloadAsync(src, dest);
  return { skipped: false, uri: dest };
}

export async function downloadTilesForBBox(bbox, zoomMin, zoomMax, onProgress) {
  await ensureDirAsync(TILES_DIR);
  let total = 0;
  let completed = 0;
  const tasks = [];
  for (let z = zoomMin; z <= zoomMax; z += 1) {
    const range = bboxToTileRange(bbox, z);
    for (let x = range.xMin; x <= range.xMax; x += 1) {
      for (let y = range.yMin; y <= range.yMax; y += 1) {
        total += 1;
        tasks.push({ z, x, y });
      }
    }
  }

  for (const t of tasks) {
    try {
      await downloadTile(t.z, t.x, t.y);
    } catch (e) {
      // continue on individual tile errors
    }
    completed += 1;
    if (onProgress) onProgress({ completed, total, percent: Math.round((completed / total) * 100) });
  }

  const info = await FileSystem.getInfoAsync(TILES_DIR, { size: true });
  return { totalTiles: total, sizeBytes: info.size || 0 };
}

export async function removeAllTiles() {
  const info = await FileSystem.getInfoAsync(TILES_DIR);
  if (info.exists) {
    await FileSystem.deleteAsync(TILES_DIR, { idempotent: true });
  }
}

export async function readOfflinePacks() {
  const raw = await AsyncStorage.getItem(OFFLINE_PACKS_KEY);
  return raw ? JSON.parse(raw) : [];
}

export async function writeOfflinePacks(packs) {
  await AsyncStorage.setItem(OFFLINE_PACKS_KEY, JSON.stringify(packs));
}

export function bboxAroundPoint(latitude, longitude, radiusKm) {
  const deltaLat = radiusKm / 110.574; // ~ km per degree latitude
  const deltaLon = radiusKm / (111.32 * Math.cos((latitude * Math.PI) / 180));
  return {
    north: latitude + deltaLat,
    south: latitude - deltaLat,
    east: longitude + deltaLon,
    west: longitude - deltaLon,
  };
}


