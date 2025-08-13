
import React from 'react';
import { View, Text } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { DICT } from '../constants';
import { s } from '../styles';

const TopBar = ({ lang }) => {
  const L = DICT[lang];
  return (
    <View style={s.topbar}>
      <Text style={s.brand}>
        <Feather name="compass" size={18} color="#9ae6b4" /> TrailGuide
      </Text>
      <View style={{ flexDirection: 'row', gap: 18 }}>
        <Text style={s.toplink}>{L.trails}</Text>
        <Text style={s.toplink}>{L.map}</Text>
        <Text style={s.toplink}>{L.downloads}</Text>
        <Text style={s.toplink}>{L.profile}</Text>
      </View>
    </View>
  );
};

export default TopBar;
