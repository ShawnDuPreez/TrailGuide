
import React, { useState } from 'react';
import { View, Text, Image, ScrollView, Pressable } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { DICT } from '../constants';
import { s } from '../styles';
import Stat from '../components/Stat';

const TrailDetails = ({ trail, lang }) => {
  const L = DICT[lang];
  const [fav, setFav] = useState(false);
  if (!trail) return null;

  return (
    <ScrollView contentContainerStyle={{ paddingBottom: 24 }}>
      <Image source={{ uri: trail.image }} style={{ width: '100%', height: 220 }} />
      <View style={{ padding: 16, gap: 10 }}>
        <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
          <View style={{ flex: 1, paddingRight: 12 }}>
            <Text style={{ color: 'white', fontWeight: '700', fontSize: 22 }}>
              {trail.name} <Text style={s.smallBadge}>{trail.difficulty}</Text>
            </Text>
            <View style={s.row}>
              <Feather name="map-pin" size={14} color="#94a3b8" />
              <Text style={s.cardSub}>{trail.city}</Text>
            </View>
          </View>
          <View style={{ flexDirection: 'row', gap: 8 }}>
            <Pressable style={s.btnPrimary}>
              <Feather name="flag" size={14} color="white" />
              <Text style={s.btnPrimaryText}>{L.startHike}</Text>
            </Pressable>
            <Pressable style={s.btnSoft}>
              <Feather name="download" size={14} color="#e2e8f0" />
              <Text style={s.btnSoftText}>{L.download}</Text>
            </Pressable>
            <Pressable
              onPress={() => setFav((f) => !f)}
              style={[s.btnSoft, fav && { backgroundColor: '#0b1324', borderColor: '#f43f5e' }]}
            >
              <Feather name="heart" size={14} color={fav ? '#f43f5e' : '#e2e8f0'} />
              <Text style={s.btnSoftText}>{fav ? L.addedFav : L.addFav}</Text>
            </Pressable>
          </View>
        </View>

        <View style={s.panel}>
          <Text style={s.panelTitle}>{L.stats}</Text>
          <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
            <Stat icon="flag" label={L.length} value={`${trail.distanceKm} km`} />
            <Stat icon="bar-chart-2" label={L.elevation} value={`${trail.elevationM} m`} />
            <Stat icon="star" label={L.rating} value={`${trail.rating}`} />
          </View>
        </View>

        <View style={s.panel}>
          <Text style={s.panelTitle}>Segments</Text>
          {['Trailhead → River Crossing', 'River Crossing → Ridge', 'Ridge → Summit'].map((sname, i) => (
            <View key={sname} style={s.segment}>
              <View style={s.row}>
                <Feather name="chevron-right" size={16} color="#cbd5e1" />
                <Text style={{ color: '#e2e8f0' }}>{sname}</Text>
              </View>
              <View style={s.badge}>
                <Text style={s.badgeText}>{i === 2 ? 'Exposed' : i === 1 ? 'Steep' : 'Family'}</Text>
              </View>
            </View>
          ))}
        </View>
      </View>
    </ScrollView>
  );
};

export default TrailDetails;
