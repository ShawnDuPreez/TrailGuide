
import React, { useEffect, useState } from 'react';
import { ScrollView, View, Text, Image, TouchableOpacity } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { DICT, MOCK_TRAILS } from '../constants';
import NetInfo from '@react-native-community/netinfo';
import { s } from '../styles';
import { bboxAroundPoint, downloadTilesForBBox, readOfflinePacks, writeOfflinePacks } from '../utils/tileDownloader';

const DownloadsScreen = ({ lang }) => {
  const L = DICT[lang];
  const [isConnected, setIsConnected] = useState(true);
  const [progress, setProgress] = useState(null);
  const [packs, setPacks] = useState([]);

  useEffect(() => {
    const sub = NetInfo.addEventListener((state) => setIsConnected(!!state.isConnected));
    readOfflinePacks().then(setPacks);
    return () => sub && sub();
  }, []);

  async function startDownloadExample() {
    if (!isConnected) return;
    const exampleLat = -25.7479;
    const exampleLon = 28.2293;
    const bbox = bboxAroundPoint(exampleLat, exampleLon, 5); // ~5km radius
    setProgress({ completed: 0, total: 1, percent: 0 });
    const result = await downloadTilesForBBox(bbox, 12, 15, setProgress);
    const pack = { id: `pretoria-5km`, bbox, zoomMin: 12, zoomMax: 15, createdAt: Date.now(), ...result };
    const next = [...packs.filter((p) => p.id !== pack.id), pack];
    setPacks(next);
    await writeOfflinePacks(next);
    setProgress(null);
  }

  return (
    <ScrollView
      style={{ flex: 1 }}
      contentContainerStyle={{ flexGrow: 1, padding: 16 }}
    >
      <View
        style={{
          flex: 1,
          borderRadius: 16,
          borderWidth: 1,
          borderColor: 'rgba(255,255,255,0.08)',
          backgroundColor: 'rgba(255,255,255,0.03)',
          padding: 16,
        }}
      >
        <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 12 }}>
          <Feather name="download" size={18} color="#9CA3AF" />
          <Text style={{ marginLeft: 8, fontWeight: '600', fontSize: 16, color: '#E5E7EB' }}>
            {L.offlinePacks}
          </Text>
        </View>

        <View style={{ gap: 12 }}>
          {!isConnected && (
            <View style={s.downloadRow}>
              <Feather name="wifi-off" size={16} color="#ef4444" />
              <Text style={{ color: '#e5e7eb', marginLeft: 8 }}>You're offline. Downloads disabled.</Text>
            </View>
          )}
          <TouchableOpacity
            disabled={!isConnected}
            onPress={startDownloadExample}
            style={[s.btnSoftFull, !isConnected && { opacity: 0.5 }]}
          >
            <Text style={s.btnSoftText}>Download example 5km area (z12-15)</Text>
          </TouchableOpacity>

          {progress && (
            <View style={{ gap: 6 }}>
              <Text style={{ color: '#94a3b8' }}>Downloading tiles: {progress.percent}%</Text>
              <View style={s.progressOuter}>
                <View style={[s.progressInner, { width: `${progress.percent}%` }]} />
              </View>
            </View>
          )}

          <View style={{ gap: 8 }}>
            {packs.length === 0 && <Text style={{ color: '#9CA3AF' }}>No offline packs yet.</Text>}
            {packs.map((p) => (
              <View key={p.id} style={s.downloadRow}>
                <Feather name="map" size={16} color="#10B981" />
                <Text style={{ color: '#e5e7eb', marginLeft: 8, flex: 1 }}>{p.id} — tiles: {p.totalTiles}</Text>
              </View>
            ))}
          </View>
        </View>
      </View>
    </ScrollView>
  );
};

export default DownloadsScreen;
