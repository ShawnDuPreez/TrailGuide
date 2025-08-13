
import React, { useEffect, useMemo, useState } from 'react';
import { View, Text, TextInput, ScrollView, Pressable } from 'react-native';
import Slider from '@react-native-community/slider';
import { Feather } from '@expo/vector-icons';
import { DICT, DIFFICULTY_ORDER, MOCK_TRAILS } from '../constants';
import { s } from '../styles';
import TrailCard from '../components/TrailCard';
import AsyncStorage from '@react-native-async-storage/async-storage';
import NetInfo from '@react-native-community/netinfo';
import { fetchTrailsFromSupabase } from '../lib/supabase';

const TrailsScreen = ({ lang, onOpenDetails }) => {
  const L = DICT[lang];
  const [query, setQuery] = useState('');
  const [difficulty, setDifficulty] = useState('any');
  const [distance, setDistance] = useState(20);
  const [trails, setTrails] = useState(MOCK_TRAILS);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    async function loadTrails() {
      setLoading(true);
      try {
        const net = await NetInfo.fetch();
        const cachedRaw = await AsyncStorage.getItem('trails');
        if (!net.isConnected && cachedRaw) {
          setTrails(JSON.parse(cachedRaw));
          return;
        }
        if (net.isConnected) {
          const remote = await fetchTrailsFromSupabase();
          if (remote && remote.length) {
            setTrails(remote);
            await AsyncStorage.setItem('trails', JSON.stringify(remote));
            return;
          }
        }
        if (cachedRaw) setTrails(JSON.parse(cachedRaw));
      } catch (_) {
        // ignore and keep defaults
      } finally {
        setLoading(false);
      }
    }
    loadTrails();
  }, []);

  const filtered = useMemo(() => {
    return trails.filter((t) => {
      const q = query.trim().toLowerCase();
      const byQuery = !q || t.name.toLowerCase().includes(q) || t.city.toLowerCase().includes(q);
      const byDifficulty = difficulty === 'any' || t.difficulty === difficulty;
      const byDistance = t.distanceKm <= distance;
      return byQuery && byDifficulty && byDistance;
    }).sort((a, b) => DIFFICULTY_ORDER[a.difficulty] - DIFFICULTY_ORDER[b.difficulty]);
  }, [query, difficulty, distance]);

  const DiffButton = ({ value, label }) => (
    <Pressable onPress={() => setDifficulty(value)} style={[s.chip, difficulty === value && s.chipActive]}>
      <Text style={[s.chipText, difficulty === value && s.chipTextActive]}>{label}</Text>
    </Pressable>
  );

  return (
    <ScrollView contentContainerStyle={{ padding: 16, gap: 12 }}>
      <View style={s.panel}>
        <Text style={s.panelTitle}><Feather name="search" size={16} /> Filters</Text>
        <View style={{ gap: 8 }}>
          <Text style={s.label}>{L.searchTrails}</Text>
          <TextInput
            placeholder="Magalies, Drakensberg…"
            placeholderTextColor="#64748b"
            value={query}
            onChangeText={setQuery}
            style={s.input}
          />
        </View>

        <View style={{ gap: 8, marginTop: 8 }}>
          <Text style={s.label}>{L.difficulty}</Text>
          <View style={{ flexDirection: 'row', gap: 8, flexWrap: 'wrap' }}>
            <DiffButton value="any" label={L.any} />
            <DiffButton value="easy" label={L.easy} />
            <DiffButton value="moderate" label={L.moderate} />
            <DiffButton value="hard" label={L.hard} />
          </View>
        </View>

        <View style={{ gap: 8, marginTop: 8 }}>
          <Text style={s.label}>{L.distance}: ≤ {distance} km</Text>
          <Slider
            value={distance}
            onValueChange={setDistance}
            minimumValue={1}
            maximumValue={30}
            step={1}
            minimumTrackTintColor="#22c55e"
            maximumTrackTintColor="#334155"
            thumbTintColor="#22c55e"
          />
        </View>
      </View>

      <View style={{ gap: 12 }}>
        {loading && (
          <View style={s.downloadRow}>
            <Feather name="loader" size={16} color="#93c5fd" />
            <Text style={{ color: '#e5e7eb', marginLeft: 8 }}>Loading trails…</Text>
          </View>
        )}
        {filtered.map((t) => (
          <TrailCard key={t.id} t={t} onOpen={onOpenDetails} />
        ))}
      </View>
    </ScrollView>
  );
};

export default TrailsScreen;
