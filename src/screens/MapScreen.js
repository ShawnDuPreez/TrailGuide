
import React, { useMemo, useState } from 'react';
import { View, Text, Switch } from 'react-native';
import { Feather } from '@expo/vector-icons';
import MapView, { UrlTile, PROVIDER_GOOGLE } from 'react-native-maps';
import * as FileSystem from 'expo-file-system';
import NetInfo from '@react-native-community/netinfo';
import { DICT } from '../constants';
import { s } from '../styles';
import { getTilesBaseDir } from '../utils/tileDownloader';

const MapScreen = ({ lang, trail }) => {
  const L = DICT[lang];
  const [useOffline, setUseOffline] = useState(false);
  const [isConnected, setIsConnected] = useState(true);

  React.useEffect(() => {
    const sub = NetInfo.addEventListener((state) => setIsConnected(!!state.isConnected));
    return () => sub && sub();
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

  return (
    <View style={{ padding: 16 }}>
      <View style={s.panel}>
        <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
          <Text style={s.panelTitle}><Feather name="map-pin" size={16} /> {L.map}</Text>
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
            <Text style={{ color: '#94a3b8', fontSize: 12 }}>Offline</Text>
            <Switch
              value={useOffline}
              onValueChange={setUseOffline}
            />
          </View>
        </View>
        <View style={s.mapBox}>
          <MapView
            provider={PROVIDER_GOOGLE}
            style={{ flex: 1 }}
            initialRegion={region}
          >
            {useOffline && (
              <UrlTile
                /* file:// scheme works implicitly with local files in RN Maps */
                urlTemplate={localTileTemplate}
                maximumZ={19}
                flipY={false}
              />
            )}
          </MapView>
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
