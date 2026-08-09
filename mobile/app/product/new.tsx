import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { Alert } from 'react-native';

import { api, ApiError } from '../../src/api/client';
import ProductForm from '../../src/components/ProductForm';
import type { ProductRequest } from '../../src/api/types';

export default function NewProductScreen() {
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (body: ProductRequest) => {
    setSubmitting(true);
    try {
      await api.createProduct(body);
      Alert.alert('Success', 'Product created.');
      router.back();
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        Alert.alert('Duplicate SKU', 'A product with this SKU already exists.');
      } else if (e instanceof ApiError && e.status === 400) {
        Alert.alert('Validation error', e.message);
      } else if (e instanceof ApiError && e.status === 404) {
        Alert.alert('Category not found', 'The selected category no longer exists.');
      } else if (e instanceof ApiError) {
        Alert.alert('Error', e.message);
      } else {
        Alert.alert('Error', 'Unexpected error creating the product.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return <ProductForm submitLabel="Create Product" submitting={submitting} onSubmit={handleSubmit} />;
}
