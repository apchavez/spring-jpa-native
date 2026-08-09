import React, { useEffect, useState } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { Picker } from '@react-native-picker/picker';

import { api, ApiError } from '../api/client';
import type { Category } from '../api/types';

interface Props {
  value: number | null;
  onChange: (categoryId: number, categoryName: string) => void;
}

export default function CategoryPicker({ value, onChange }: Props) {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await api.getCategories();
        if (!cancelled) setCategories(data);
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof ApiError ? e.message : 'Failed to load categories.');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return (
      <View style={styles.row}>
        <ActivityIndicator />
        <Text style={styles.hint}>Loading categories...</Text>
      </View>
    );
  }

  if (error) {
    return <Text style={styles.error}>{error}</Text>;
  }

  return (
    <View style={styles.pickerWrapper}>
      <Picker
        selectedValue={value ?? undefined}
        onValueChange={(itemValue) => {
          const id = Number(itemValue);
          const found = categories.find((c) => c.id === id);
          onChange(id, found?.name ?? '');
        }}
      >
        <Picker.Item label="Select a category..." value={undefined} />
        {categories.map((c) => (
          <Picker.Item key={c.id} label={c.name} value={c.id} />
        ))}
      </Picker>
    </View>
  );
}

const styles = StyleSheet.create({
  pickerWrapper: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 6,
    overflow: 'hidden',
  },
  row: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  hint: { color: '#666' },
  error: { color: '#b00020' },
});
