import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';

import { api, ApiError } from '../../src/api/client';
import type { InventorySummary } from '../../src/api/types';

export default function ReportsScreen() {
  const [summary, setSummary] = useState<InventorySummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [unavailable, setUnavailable] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    setUnavailable(false);
    try {
      const data = await api.getInventorySummary();
      setSummary(data);
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        setUnavailable(true);
      } else if (e instanceof ApiError) {
        setError(e.message);
      } else {
        setError('Unexpected error while loading the report.');
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const onRefresh = () => {
    setRefreshing(true);
    load();
  };

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator />
      </View>
    );
  }

  return (
    <ScrollView
      contentContainerStyle={styles.container}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
    >
      <Text style={styles.title}>Inventory Summary</Text>

      {unavailable ? (
        <Text style={styles.warning}>
          The reports endpoint (GET /api/v1/reports/inventory-summary) is not available on this backend
          yet. Pull to refresh once it ships.
        </Text>
      ) : null}

      {error ? <Text style={styles.error}>{error}</Text> : null}

      {summary ? (
        <>
          <View style={styles.statsRow}>
            <View style={styles.statCard}>
              <Text style={styles.statValue}>{summary.activeProductCount}</Text>
              <Text style={styles.statLabel}>Active products</Text>
            </View>
            <View style={styles.statCard}>
              <Text style={styles.statValue}>{summary.inactiveProductCount}</Text>
              <Text style={styles.statLabel}>Inactive products</Text>
            </View>
          </View>
          <View style={styles.statsRow}>
            <View style={styles.statCard}>
              <Text style={styles.statValue}>{summary.totalActiveStock}</Text>
              <Text style={styles.statLabel}>Total active stock</Text>
            </View>
            <View style={styles.statCard}>
              <Text style={styles.statValue}>${summary.totalActiveValue.toFixed(2)}</Text>
              <Text style={styles.statLabel}>Total active value</Text>
            </View>
          </View>

          <Text style={styles.sectionTitle}>By category</Text>
          {summary.byCategory.length === 0 ? (
            <Text style={styles.hint}>No category data.</Text>
          ) : (
            summary.byCategory.map((cat) => (
              <View key={cat.categoryId} style={styles.categoryCard}>
                <Text style={styles.categoryName}>{cat.categoryName}</Text>
                <View style={styles.categoryRow}>
                  <Text style={styles.categoryDetail}>Products: {cat.productCount}</Text>
                  <Text style={styles.categoryDetail}>Stock: {cat.totalStock}</Text>
                  <Text style={styles.categoryDetail}>Value: ${cat.totalValue.toFixed(2)}</Text>
                </View>
              </View>
            ))
          )}
        </>
      ) : null}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 16, gap: 8 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  title: { fontSize: 20, fontWeight: '700', marginBottom: 8 },
  warning: { color: '#92400e', backgroundColor: '#fef3c7', padding: 10, borderRadius: 8, marginBottom: 12 },
  error: { color: '#b00020', marginBottom: 12 },
  hint: { color: '#666' },
  statsRow: { flexDirection: 'row', gap: 12, marginBottom: 12 },
  statCard: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#e5e5e5',
    borderRadius: 10,
    padding: 14,
    alignItems: 'center',
  },
  statValue: { fontSize: 22, fontWeight: '800', color: '#2563eb' },
  statLabel: { color: '#666', marginTop: 4, textAlign: 'center' },
  sectionTitle: { fontSize: 16, fontWeight: '700', marginTop: 12, marginBottom: 8 },
  categoryCard: {
    borderWidth: 1,
    borderColor: '#e5e5e5',
    borderRadius: 8,
    padding: 12,
    marginBottom: 8,
  },
  categoryName: { fontWeight: '700', marginBottom: 4 },
  categoryRow: { flexDirection: 'row', justifyContent: 'space-between' },
  categoryDetail: { color: '#374151' },
});
