
import React from 'react';
import { View, Text } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { s } from '../styles';

const Stat = ({ icon, label, value }) => {
  return (
    <View style={s.stat}>
      <Feather name={icon} size={16} color="#94a3b8" />
      <View>
        <Text style={s.statLabel}>{label}</Text>
        <Text style={s.statValue}>{value}</Text>
      </View>
    </View>
  );
};

export default Stat;
