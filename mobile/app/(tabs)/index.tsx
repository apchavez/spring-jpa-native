import { Stack, useRouter } from 'expo-router';
import React from 'react';
import { Text, TouchableOpacity } from 'react-native';

import { api } from '../../src/api/client';
import { fetchProductsPage } from '../../src/api/graphqlClient';
import ProductList from '../../src/components/ProductList';
import { useAuth } from '../../src/context/AuthContext';

export default function ActiveProductsScreen() {
  const router = useRouter();
  const { username, logout } = useAuth();

  return (
    <>
      <Stack.Screen
        options={{
          headerRight: () => (
            <TouchableOpacity onPress={() => router.push('/product/new')} style={{ marginRight: 16 }}>
              <Text style={{ fontSize: 22, fontWeight: '700', color: '#2563eb' }}>+</Text>
            </TouchableOpacity>
          ),
          headerLeft: () => (
            <TouchableOpacity onPress={() => logout()} style={{ marginLeft: 16 }}>
              <Text style={{ color: '#b00020' }}>Log out</Text>
            </TouchableOpacity>
          ),
          title: username ? `Products (${username})` : 'Products',
        }}
      />
      {/*
        Active list fetched via GraphQL, not REST — this is the "same data, one round trip" screen
        the README's GraphQL section describes: it asks for exactly the 6 fields the card renders
        (id/sku/name/category.name/price/stock/active), unlike the REST equivalent below which
        always returns the full ProductResponse including `description`, whether the screen needs
        it or not. Search stays on REST (no `search` field exists in the GraphQL schema yet).
      */}
      <ProductList
        fetchPage={(page, size) => fetchProductsPage(page, size, true)}
        searchPage={(prefix, page, size) => api.searchProducts(prefix, page, size)}
        emptyLabel="No active products yet."
      />
    </>
  );
}
