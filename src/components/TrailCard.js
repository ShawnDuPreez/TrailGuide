
import React from 'react';
import { View, Text, Image, Pressable } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { s } from '../styles';

const TrailCard = ({ t, onOpen }) => {
  return (
    <Pressable onPress={() => onOpen(t)} style={s.card}>
      <Image source={{ uri: t.image }} style={s.cardImage} />
      <View style={{ padding: 12 }}>
        <View style={s.cardTitleRow}>
          <Text style={s.cardTitle} numberOfLines={1}>{t.name}</Text>
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 4 }}>
            <Feather name="star" size={14} color="#facc15" />
            <Text style={s.cardMeta}>{t.rating}</Text>
          </View>
        </View>
        <View style={s.row}>
          <Feather name="map-pin" size={14} color="#94a3b8" />
          <Text style={s.cardSub}>{t.city}</Text>
        </View>
        <View style={[s.row, { justifyContent: 'space-between', marginTop: 8 }]}>
          <View style={s.row}>
            <Feather name="flag" size={14} color="#94a3b8" />
            <Text style={s.cardMeta}>{t.distanceKm} km</Text>
          </View>
          <View style={s.row}>
            <Feather name="bar-chart-2" size={14} color="#94a3b8" />
            <Text style={s.cardMeta}>{t.elevationM} m</Text>
          </View>
          <View style={s.badge}>
            <Text style={s.badgeText}>{t.difficulty}</Text>
          </View>
          {t.downloaded && (
            <View style={[s.badge, { backgroundColor: '#059669' }]}>
              <Text style={[s.badgeText, { textTransform: 'none' }]}>Offline</Text>
            </View>
          )}
        </View>
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: 8 }}>
          {t.tags.map((tag) => (
            <View key={tag} style={s.tag}><Text style={s.tagText}>#{tag}</Text></View>
          ))}
        </View>
      </View>
    </Pressable>
  );
};

export default TrailCard;
