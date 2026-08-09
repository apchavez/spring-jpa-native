import React, { useState } from 'react';
import { ActivityIndicator, Alert, ScrollView, StyleSheet, Switch, Text, TextInput, TouchableOpacity, View } from 'react-native';

import CategoryPicker from './CategoryPicker';
import type { ProductRequest, ProductResponse } from '../api/types';

interface Props {
  initial?: ProductResponse;
  submitLabel: string;
  submitting: boolean;
  onSubmit: (body: ProductRequest) => void;
}

export default function ProductForm({ initial, submitLabel, submitting, onSubmit }: Props) {
  const [sku, setSku] = useState(initial?.sku ?? '');
  const [name, setName] = useState(initial?.name ?? '');
  const [description, setDescription] = useState(initial?.description ?? '');
  const [price, setPrice] = useState(initial ? String(initial.price) : '');
  const [stock, setStock] = useState(initial ? String(initial.stock) : '');
  const [active, setActive] = useState(initial?.active ?? true);
  const [categoryId, setCategoryId] = useState<number | null>(initial?.categoryId ?? null);
  const [categoryName, setCategoryName] = useState<string>(initial?.categoryName ?? '');

  const handleSubmit = () => {
    if (!sku.trim() || !name.trim()) {
      Alert.alert('Missing fields', 'SKU and name are required.');
      return;
    }
    if (categoryId == null) {
      Alert.alert('Missing category', 'Please select a category.');
      return;
    }
    const priceNum = Number(price);
    const stockNum = Number(stock);
    if (Number.isNaN(priceNum) || Number.isNaN(stockNum)) {
      Alert.alert('Invalid number', 'Price and stock must be numeric.');
      return;
    }
    onSubmit({
      sku: sku.trim(),
      name: name.trim(),
      description: description.trim(),
      categoryId,
      price: priceNum,
      stock: stockNum,
      active,
    });
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.label}>SKU</Text>
      <TextInput style={styles.input} value={sku} onChangeText={setSku} autoCapitalize="characters" placeholder="e.g. WIDGET-001" />

      <Text style={styles.label}>Name</Text>
      <TextInput style={styles.input} value={name} onChangeText={setName} placeholder="Product name" />

      <Text style={styles.label}>Description</Text>
      <TextInput
        style={[styles.input, styles.multiline]}
        value={description}
        onChangeText={setDescription}
        placeholder="Description"
        multiline
      />

      <Text style={styles.label}>Category</Text>
      <CategoryPicker
        value={categoryId}
        onChange={(id, catName) => {
          setCategoryId(id);
          setCategoryName(catName);
        }}
      />
      {categoryName ? <Text style={styles.hint}>Selected: {categoryName}</Text> : null}

      <Text style={styles.label}>Price</Text>
      <TextInput style={styles.input} value={price} onChangeText={setPrice} keyboardType="decimal-pad" placeholder="0.00" />

      <Text style={styles.label}>Stock</Text>
      <TextInput style={styles.input} value={stock} onChangeText={setStock} keyboardType="number-pad" placeholder="0" />

      <View style={styles.switchRow}>
        <Text style={styles.label}>Active</Text>
        <Switch value={active} onValueChange={setActive} />
      </View>

      <TouchableOpacity testID="submit-button" style={styles.submitButton} onPress={handleSubmit} disabled={submitting}>
        {submitting ? <ActivityIndicator color="#fff" /> : <Text style={styles.submitText}>{submitLabel}</Text>}
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 16, gap: 4 },
  label: { fontWeight: '600', marginTop: 12, marginBottom: 4 },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 6,
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
  multiline: { minHeight: 80, textAlignVertical: 'top' },
  hint: { color: '#666', marginTop: 4 },
  switchRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 16 },
  submitButton: {
    marginTop: 24,
    backgroundColor: '#2563eb',
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
  },
  submitText: { color: '#fff', fontWeight: '700', fontSize: 16 },
});
