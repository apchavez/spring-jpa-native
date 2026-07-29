import { useRouter } from 'expo-router';
import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, RefreshControl, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';

import { ApiError } from '../api/client';
import type { Page, ProductResponse } from '../api/types';

interface Props {
  fetchPage: (page: number, size: number) => Promise<Page<ProductResponse>>;
  searchPage?: (prefix: string, page: number, size: number) => Promise<Page<ProductResponse>>;
  emptyLabel?: string;
}

const PAGE_SIZE = 20;

export default function ProductList({ fetchPage, searchPage, emptyLabel }: Props) {
  const router = useRouter();
  const [items, setItems] = useState<ProductResponse[]>([]);
  const [page, setPage] = useState(0);
  const [last, setLast] = useState(true);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState('');

  const load = useCallback(
    async (targetPage: number, replace: boolean) => {
      setError(null);
      try {
        const fn = query.trim() && searchPage ? searchPage(query.trim(), targetPage, PAGE_SIZE) : fetchPage(targetPage, PAGE_SIZE);
        const result = await fn;
        setItems((prev) => (replace ? result.content : [...prev, ...result.content]));
        setPage(result.page);
        setLast(result.last);
      } catch (e) {
        setError(e instanceof ApiError ? e.message : 'Failed to load products.');
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [fetchPage, searchPage, query],
  );

  useEffect(() => {
    setLoading(true);
    load(0, true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query]);

  const onRefresh = () => {
    setRefreshing(true);
    load(0, true);
  };

  const loadMore = () => {
    if (!last && !loading) {
      load(page + 1, false);
    }
  };

  return (
    <View style={styles.container}>
      {searchPage ? (
        <TextInput
          style={styles.search}
          placeholder="Search by name prefix..."
          value={query}
          onChangeText={setQuery}
          autoCapitalize="none"
        />
      ) : null}

      {error ? <Text style={styles.error}>{error}</Text> : null}

      {loading && items.length === 0 ? (
        <ActivityIndicator style={styles.loader} />
      ) : (
        <FlatList
          data={items}
          keyExtractor={(item) => String(item.id)}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
          onEndReachedThreshold={0.4}
          onEndReached={loadMore}
          ListEmptyComponent={<Text style={styles.empty}>{emptyLabel ?? 'No products found.'}</Text>}
          renderItem={({ item }) => (
            <TouchableOpacity style={styles.card} onPress={() => router.push(`/product/${item.id}`)}>
              <View style={styles.cardHeader}>
                <Text style={styles.sku}>{item.sku}</Text>
                <View style={[styles.badge, item.active ? styles.badgeActive : styles.badgeInactive]}>
                  <Text style={styles.badgeText}>{item.active ? 'ACTIVE' : 'INACTIVE'}</Text>
                </View>
              </View>
              <Text style={styles.name}>{item.name}</Text>
              <Text style={styles.category}>{item.categoryName}</Text>
              <View style={styles.cardFooter}>
                <Text style={styles.price}>${item.price.toFixed(2)}</Text>
                <Text style={styles.stock}>Stock: {item.stock}</Text>
              </View>
            </TouchableOpacity>
          )}
          ListFooterComponent={!last && items.length > 0 ? <ActivityIndicator style={{ marginVertical: 12 }} /> : null}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  search: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
    margin: 12,
  },
  loader: { marginTop: 40 },
  error: { color: '#b00020', marginHorizontal: 12, marginBottom: 8 },
  empty: { textAlign: 'center', color: '#666', marginTop: 40 },
  card: {
    marginHorizontal: 12,
    marginBottom: 10,
    padding: 12,
    borderWidth: 1,
    borderColor: '#e5e5e5',
    borderRadius: 10,
  },
  cardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  sku: { fontWeight: '700', color: '#374151' },
  name: { fontSize: 16, fontWeight: '600', marginTop: 4 },
  category: { color: '#6b7280', marginTop: 2 },
  cardFooter: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 8 },
  price: { fontWeight: '600' },
  stock: { color: '#374151' },
  badge: { paddingHorizontal: 8, paddingVertical: 2, borderRadius: 12 },
  badgeActive: { backgroundColor: '#dcfce7' },
  badgeInactive: { backgroundColor: '#fee2e2' },
  badgeText: { fontSize: 11, fontWeight: '700' },
});
