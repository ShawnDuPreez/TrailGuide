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
    <ScrollView contentContainerStyle={{ paddingBottom: 12 }}>
      <Image source={{ uri: trail.image }} style={{ width: '100%', height: 220 }} />

      {/* Main content under the image */}
      <View style={{ paddingHorizontal: 20, marginTop: -360 }}>
        {/* Title + actions row */}
        <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
          <View style={{ flex: 1, paddingRight: 12 }}>
            <Text style={{ color: 'white', fontWeight: '700', fontSize: 22 }}>
              {trail.name} <Text style={s.smallBadge}>{trail.difficulty}</Text>
            </Text>
            <View style={s.row}>
              <Feather name="map-pin" size={14} color="#94a3b8" />
              <Text style={s.cardSub}>{trail.city}</Text>
            </View>
          </View>

          {/* === Responsive buttons: always fit in one row === */}
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <Pressable
              style={[
                s.btnPrimary,
                {
                  flex: 1,
                  minWidth: 0,          // allow shrinking
                  paddingHorizontal: 8,
                  paddingVertical: 6,
                  marginRight: 6,
                  flexDirection: 'row',
                  alignItems: 'center',
                },
              ]}
            >
              <Feather name="flag" size={13} color="white" />
              <Text
                numberOfLines={1}
                ellipsizeMode="tail"
                style={[s.btnPrimaryText, { fontSize: 12, marginLeft: -1, flexShrink: 1}]}
              >
                {L.startHike}
              </Text>
            </Pressable>

            <Pressable
              style={[
                s.btnSoft,
                {
                  flex: 1,
                  minWidth: 0,
                  paddingHorizontal: 8,
                  paddingVertical: 6,
                  marginRight: 6,
                  flexDirection: 'row',
                  alignItems: 'center',
                },
              ]}
            >
              <Feather name="download" size={13} color="#e2e8f0" />
              <Text
                numberOfLines={1}
                ellipsizeMode="tail"
                style={[s.btnSoftText, { fontSize: 12, marginLeft: 4, flexShrink: 1 }]}
              >
                {L.download}
              </Text>
            </Pressable>

            <Pressable
              onPress={() => setFav((f) => !f)}
              style={[
                s.btnSoft,
                {
                  flex: 1,
                  minWidth: 0,
                  paddingHorizontal: 8,
                  paddingVertical: 6,
                  flexDirection: 'row',
                  alignItems: 'center',
                },
                fav && { backgroundColor: '#0b1324', borderColor: '#f43f5e' },
              ]}
            >
              <Feather name="heart" size={13} color={fav ? '#f43f5e' : '#e2e8f0'} />
              <Text
                numberOfLines={1}
                ellipsizeMode="tail"
                style={[s.btnSoftText, { fontSize: 12, marginLeft: 4, flexShrink: 1 }]}
              >
                {fav ? L.addedFav : L.addFav}
              </Text>
            </Pressable>
          </View>
          {/* === /Responsive buttons === */}
        </View>

        {/* Stats directly under the header row with minimal spacing */}
        <View style={[s.panel, { paddingHorizontal: 10, paddingVertical: 8, marginTop: -360 }]}>
          <Text style={s.panelTitle}>{L.stats}</Text>
          <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
            <Stat icon="flag" label={L.length} value={`${trail.distanceKm} km`} />
            <Stat icon="bar-chart-2" label={L.elevation} value={`${trail.elevationM} m`} />
            <Stat icon="star" label={L.rating} value={`${trail.rating}`} />
          </View>
        </View>

        {/* Segments panel, small separation from stats */}
        <View style={[s.panel, { paddingHorizontal: 10, paddingVertical: 8, marginTop: 8 }]}>
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
