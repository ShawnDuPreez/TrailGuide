
import React from 'react';
import { Pressable, Text } from 'react-native';
import { Feather } from '@expo/vector-icons';
import { s } from '../styles';

const TabButton = ({ label, icon, active, onPress }) => {
  const iconName = icon === 'mountain' ? 'triangle' : icon;
  return (
    <Pressable onPress={onPress} style={s.tabBtn}>
      <Feather name={iconName} size={18} color={active ? '#22c55e' : '#94a3b8'} />
      <Text style={[s.tabTxt, active && { color: '#22c55e' }]}>{label}</Text>
    </Pressable>
  );
};

export default TabButton;
