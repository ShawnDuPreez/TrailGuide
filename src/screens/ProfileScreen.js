
import React, { useEffect, useState } from 'react';
import { ScrollView, View, Text, Pressable, Switch } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { DICT } from '../constants';
import { s } from '../styles';
import * as WebBrowser from 'expo-web-browser';
import * as Linking from 'expo-linking';
import { signInWithGoogle, signOut, getCurrentUser, exchangeCodeForSession, getRedirectUri } from '../lib/auth';
import { supabase } from '../lib/supabase';

const ProfileScreen = ({ lang, setLang }) => {
  const L = DICT[lang];
  const [biometrics, setBiometrics] = useState(false);
  const [notif, setNotif] = useState(true);
  const [isSignedIn, setIsSignedIn] = useState(false);
  const [email, setEmail] = useState('');

  useEffect(() => {
    const sub = supabase.auth.onAuthStateChange(async (_event, session) => {
      const user = session?.user ?? null;
      setIsSignedIn(!!user);
      setEmail(user?.email ?? '');
    });
    getCurrentUser().then((u) => {
      setIsSignedIn(!!u);
      setEmail(u?.email ?? '');
    });
    return () => {
      sub.data.subscription.unsubscribe();
    };
  }, []);

  useEffect(() => {
    // Handle deep link callback for OAuth PKCE
    const redirectPath = getRedirectUri();
    const handler = async ({ url }) => {
      if (url && url.startsWith(redirectPath)) {
        await exchangeCodeForSession(url);
      }
    };
    const subscription = Linking.addEventListener('url', handler);
    Linking.getInitialURL().then((url) => url && handler({ url }));
    return () => subscription.remove();
  }, []);

  async function handleSignIn() {
    try {
      const { authUrl } = await signInWithGoogle();
      if (authUrl) await WebBrowser.openAuthSessionAsync(authUrl);
    } catch (e) {
      // noop
    }
  }

  async function handleSignOut() {
    try {
      await signOut();
    } catch (_) {}
  }

  return (
    <ScrollView contentContainerStyle={{ padding: 16, gap: 12 }}>
      <View style={s.panel}>
        <Text style={s.panelTitle}><Feather name="log-in" size={16} /> {L.sso}</Text>
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}>
          <View style={{ width: 34, height: 34, borderRadius: 999, backgroundColor: '#9ae6b4', alignItems: 'center', justifyContent: 'center' }}>
            <Text style={{ color: '#0f172a', fontWeight: '700' }}>U</Text>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={{ color: 'white', fontWeight: '600' }}>{isSignedIn ? (email || 'User') : 'Guest'}</Text>
            <Text style={{ color: '#94a3b8', fontSize: 12 }}>Google</Text>
          </View>
          {!isSignedIn ? (
            <Pressable style={s.btnPrimary} onPress={handleSignIn}>
              <Feather name="log-in" size={14} color="white" />
              <Text style={s.btnPrimaryText}>{L.signIn}</Text>
            </Pressable>
          ) : (
            <Pressable style={s.btnSoft} onPress={handleSignOut}>
              <Feather name="log-out" size={14} color="#e2e8f0" />
              <Text style={s.btnSoftText}>{L.signOut}</Text>
            </Pressable>
          )}
        </View>
      </View>

      <View style={s.panel}>
        <Text style={s.panelTitle}><Feather name="fingerprint" size={16} /> {L.biometrics}</Text>
        <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
          <Text style={{ color: '#94a3b8', fontSize: 12 }}>{L.enable}</Text>
          <Switch value={biometrics} onValueChange={setBiometrics} />
        </View>
      </View>

      <View style={s.panel}>
        <Text style={s.panelTitle}><Feather name="bell" size={16} /> {L.notifications}</Text>
        <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
          <Text style={{ color: '#94a3b8', fontSize: 12 }}>Trail reminders & safety alerts</Text>
          <Switch value={notif} onValueChange={setNotif} />
        </View>
      </View>

      <View style={s.panel}>
        <Text style={s.panelTitle}><Feather name="globe" size={16} /> {L.language}</Text>
        <View style={{ flexDirection: 'row', gap: 8 }}>
          {['en', 'af', 'zu'].map((code) => (
            <Pressable key={code} onPress={() => setLang(code)} style={[s.chip, lang === code && s.chipActive]}>
              <Text style={[s.chipText, lang === code && s.chipTextActive]}>{code.toUpperCase()}</Text>
            </Pressable>
          ))}
        </View>
      </View>
    </ScrollView>
  );
};

export default ProfileScreen;
