import 'react-native-get-random-values';
import { polyfillWebCrypto } from 'expo-standard-web-crypto';
import * as WebBrowser from 'expo-web-browser';
import * as Linking from 'expo-linking';

import React, { useEffect, useState } from 'react';
import { View, Text } from 'react-native';

import TopBar from './src/components/TopBar';
import TabButton from './src/components/TabButton';
import TrailsScreen from './src/screens/TrailsScreen';
import TrailDetails from './src/screens/TrailDetails';
import MapScreen from './src/screens/MapScreen';
import DownloadsScreen from './src/screens/DownloadsScreen';
import ProfileScreen from './src/screens/ProfileScreen';
import { DICT, tabs, MOCK_TRAILS } from './src/constants';
import { s } from './src/styles';

import { getRedirectUri, exchangeCodeForSession } from './src/lib/auth';

// Run once at startup
polyfillWebCrypto();
WebBrowser.maybeCompleteAuthSession();

export default function App() {
  const [lang, setLang] = useState('en');
  const [tab, setTab] = useState(tabs.TRAILS);
  const [selected, setSelected] = useState(MOCK_TRAILS[0]);
  const L = DICT[lang];

  // Handle OAuth redirect - exchange code for session
  useEffect(() => {
    const redirect = getRedirectUri();

    const handler = async ({ url }) => {
      if (url?.startsWith(redirect)) {
        await exchangeCodeForSession(url);
      }
    };

    const sub = Linking.addEventListener('url', handler);
    Linking.getInitialURL().then((url) => url && handler({ url }));
    return () => sub.remove();
  }, []);

  return (
    <View style={{ flex: 1, backgroundColor: '#0b1020' }}>
      <TopBar lang={lang} />

      <View style={{ flex: 1 }}>
        {tab === tabs.TRAILS && (
          <TrailsScreen
            lang={lang}
            onOpenDetails={(t) => {
              setSelected(t);
              setTab(tabs.DETAILS);
            }}
          />
        )}
        {tab === tabs.DETAILS && <TrailDetails trail={selected} lang={lang} />}
        {tab === tabs.MAP && <MapScreen lang={lang} trail={selected} />}
        {tab === tabs.DOWNLOADS && <DownloadsScreen lang={lang} />}
        {tab === tabs.PROFILE && <ProfileScreen lang={lang} setLang={setLang} />}
      </View>

      <View style={s.tabs}>
        <TabButton label={L.trails} icon="compass"  active={tab === tabs.TRAILS}   onPress={() => setTab(tabs.TRAILS)} />
        <TabButton label={L.details} icon="triangle" active={tab === tabs.DETAILS} onPress={() => setTab(tabs.DETAILS)} />
        <TabButton label={L.map}     icon="map-pin"  active={tab === tabs.MAP}     onPress={() => setTab(tabs.MAP)} />
        <TabButton label={L.downloads} icon="download" active={tab === tabs.DOWNLOADS} onPress={() => setTab(tabs.DOWNLOADS)} />
        <TabButton label={L.profile} icon="settings" active={tab === tabs.PROFILE} onPress={() => setTab(tabs.PROFILE)} />
      </View>

      <Text style={s.footer}>UI mockups for Android app — React Native (Expo).</Text>
    </View>
  );
}
