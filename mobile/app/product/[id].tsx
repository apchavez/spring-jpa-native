import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import { ActivityIndicator, Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native';

import { api, ApiError } from '../../src/api/client';
import { fetchProductDetail } from '../../src/api/graphqlClient';
import ProductForm from '../../src/components/ProductForm';
import type { ProductRequest, ProductResponse } from '../../src/api/types';

export default function EditProductScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const productId = Number(id);

  const [product, setProduct] = useState<ProductResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        // Detail screen fetched via GraphQL (product + full category in one round trip) — the
        // under-fetching counterpart to the list screen's over-fetching demo. Writes (update/
        // delete) stay on REST: the GraphQL schema only defines create mutations so far.
        const data = await fetchProductDetail(productId);
        if (!cancelled) setProduct(data);
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof ApiError ? e.message : 'Failed to load product.');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [productId]);

  const handleSubmit = async (body: ProductRequest) => {
    setSubmitting(true);
    try {
      await api.updateProduct(productId, body);
      Alert.alert('Success', 'Product updated.');
      router.back();
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        Alert.alert(
          'Update conflict',
          'This product was changed by someone else, please retry.',
        );
      } else if (e instanceof ApiError && e.status === 400) {
        Alert.alert('Validation error', e.message);
      } else if (e instanceof ApiError) {
        Alert.alert('Error', e.message);
      } else {
        Alert.alert('Error', 'Unexpected error updating the product.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = () => {
    Alert.alert('Delete product', 'Are you sure you want to delete this product?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          try {
            await api.deleteProduct(productId);
            router.back();
          } catch (e) {
            Alert.alert('Error', e instanceof ApiError ? e.message : 'Failed to delete product.');
          }
        },
      },
    ]);
  };

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator />
      </View>
    );
  }

  if (error || !product) {
    return (
      <View style={styles.center}>
        <Text style={styles.error}>{error ?? 'Product not found.'}</Text>
      </View>
    );
  }

  return (
    <View style={{ flex: 1 }}>
      <ProductForm
        initial={product}
        submitLabel="Save Changes"
        submitting={submitting}
        onSubmit={handleSubmit}
      />
      <TouchableOpacity style={styles.deleteButton} onPress={handleDelete}>
        <Text style={styles.deleteText}>Delete Product</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 16 },
  error: { color: '#b00020', textAlign: 'center' },
  deleteButton: {
    margin: 16,
    borderWidth: 1,
    borderColor: '#b00020',
    borderRadius: 8,
    paddingVertical: 12,
    alignItems: 'center',
  },
  deleteText: { color: '#b00020', fontWeight: '700' },
});
