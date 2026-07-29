import { Redirect } from 'expo-router';
import React from 'react';

import { api } from '../../src/api/client';
import ProductList from '../../src/components/ProductList';
import { useAuth } from '../../src/context/AuthContext';

export default function InactiveProductsScreen() {
  const { isAdmin, isLoading } = useAuth();

  if (!isLoading && !isAdmin) {
    // Direct navigation guard in case the tab bar hid this screen but a
    // deep link or router.push still targets it.
    return <Redirect href="/(tabs)" />;
  }

  return (
    <ProductList
      fetchPage={(page, size) => api.getInactiveProducts(page, size)}
      emptyLabel="No inactive products."
    />
  );
}
