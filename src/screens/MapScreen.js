import React, { useMemo, useState } from 'react';
import { View, Text, Switch, Alert } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { WebView } from 'react-native-webview';
import * as FileSystem from 'expo-file-system';
import * as Location from 'expo-location';
import NetInfo from '@react-native-community/netinfo';
import { DICT } from '../constants';
import { s } from '../styles';
import { getTilesBaseDir } from '../utils/tileDownloader';

const MapScreen = ({ lang, trail }) => {
  const L = DICT[lang];
  const [useOffline, setUseOffline] = useState(false);
  const [isConnected, setIsConnected] = useState(true);
  const [hasOfflineTiles, setHasOfflineTiles] = useState(false);
  const webviewRef = React.useRef(null);

  React.useEffect(() => {
    const sub = NetInfo.addEventListener((state) => setIsConnected(!!state.isConnected));
    return () => sub && sub();
  }, []);

  React.useEffect(() => {
    (async () => {
      try {
        const info = await FileSystem.getInfoAsync(`${FileSystem.documentDirectory}tiles`);
        setHasOfflineTiles(!!info.exists);
      } catch {
        setHasOfflineTiles(false);
      }
    })();
  }, []);

  React.useEffect(() => {
    let locationWatch = null;
    (async () => {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') return;
      locationWatch = await Location.watchPositionAsync({ accuracy: Location.Accuracy.Balanced, timeInterval: 3000, distanceInterval: 5 }, (loc) => {
        const coords = { lat: loc.coords.latitude, lon: loc.coords.longitude };
        webviewRef.current?.postMessage(JSON.stringify({ type: 'position', payload: coords }));
      });
    })();
    return () => { locationWatch && locationWatch.remove && locationWatch.remove(); };
  }, []);

  const region = useMemo(() => {
    const lat = trail?.lat ?? -25.7479; // Pretoria fallback
    const lon = trail?.lon ?? 28.2293;
    return {
      latitude: lat,
      longitude: lon,
      latitudeDelta: 0.08,
      longitudeDelta: 0.08,
    };
  }, [trail]);

  const localTileTemplate = `${FileSystem.documentDirectory}tiles/{z}/{x}/{y}.png`;

  const leafletHtml = useMemo(() => {
    const { latitude, longitude } = region;
    const initialZoom = 12;
    const onlineTemplate = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
    const chosenTemplate = useOffline ? localTileTemplate : onlineTemplate;

    // Minimal Leaflet page
    return `<!DOCTYPE html>
    <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" />
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <style>
          html, body, #map { height: 100%; margin: 0; padding: 0; }
        </style>
      </head>
      <body>
        <div id="map"></div>
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <script>
          const lat = ${latitude};
          const lon = ${longitude};
          const template = ${JSON.stringify(chosenTemplate)};
           const map = L.map('map', { zoomControl: true, attributionControl: true }).setView([lat, lon], ${initialZoom});
          L.tileLayer(template, {
            maxZoom: 19,
            attribution: '© OpenStreetMap contributors'
          }).addTo(map);

          let userMarker = null;
          document.addEventListener('message', (e) => {
            try {
              const msg = JSON.parse(e.data);
              if (msg.type === 'position' && msg.payload) {
                const p = msg.payload;
                if (!userMarker) {
                  userMarker = L.marker([p.lat, p.lon]).addTo(map);
                } else {
                  userMarker.setLatLng([p.lat, p.lon]);
                }
                map.setView([p.lat, p.lon]);
              }
            } catch (_) {}
          });
        </script>
      </body>
    </html>`;
  }, [region, useOffline, localTileTemplate]);

  return (
    <View style={{ padding: 16 }}>
      <View style={s.panel}>
        <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
          <Text style={s.panelTitle}><Feather name="map-pin" size={16} /> {L.map}</Text>
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
            <Text style={{ color: '#94a3b8', fontSize: 12 }}>Offline</Text>
            <Switch
              value={useOffline}
              onValueChange={(val) => {
                if (val && !hasOfflineTiles) {
                  Alert.alert('Offline tiles missing', 'Download a pack first in Downloads, then enable Offline. Staying online.');
                  return;
                }
                setUseOffline(val);
              }}
            />
          </View>
        </View>
        <View style={s.mapBox}>
          <WebView
            originWhitelist={["*"]}
            allowFileAccess
            allowFileAccessFromFileURLs
            javaScriptEnabled
            domStorageEnabled
            style={{ flex: 1 }}
            source={{ html: leafletHtml }}
            ref={webviewRef}
          />
          {!isConnected && !useOffline && (
            <View style={s.mockBadge}>
              <Feather name="wifi-off" size={14} color="#0f172a" />
              <Text style={{ color: '#0f172a', marginLeft: 6 }}>No network. Enable Offline</Text>
            </View>
          )}
        </View>
      </View>
    </View>
  );
};

export default MapScreen;
