import * as DocumentPicker from 'expo-document-picker';
import { Redirect } from 'expo-router';
import React, { useState } from 'react';
import { ActivityIndicator, Platform, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';

import { api, ApiError } from '../../src/api/client';
import type { ImportSummary } from '../../src/api/types';
import { useAuth } from '../../src/context/AuthContext';

export default function ImportScreen() {
  const { isAdmin, isLoading } = useAuth();
  const [pickedName, setPickedName] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [summary, setSummary] = useState<ImportSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [unavailable, setUnavailable] = useState(false);

  if (!isLoading && !isAdmin) {
    return <Redirect href="/(tabs)" />;
  }

  const pickAndUpload = async () => {
    setError(null);
    setSummary(null);
    setUnavailable(false);

    const result = await DocumentPicker.getDocumentAsync({
      type: ['text/csv', 'text/comma-separated-values', 'application/vnd.ms-excel', '*/*'],
      copyToCacheDirectory: true,
    });

    if (result.canceled || !result.assets || result.assets.length === 0) {
      return;
    }

    const asset = result.assets[0];
    setPickedName(asset.name ?? 'file.csv');
    setUploading(true);

    try {
      const form = new FormData();
      if (Platform.OS === 'web') {
        // On web, DocumentPicker gives us a real File/Blob via `file`.
        const webFile = (asset as unknown as { file?: File }).file;
        if (webFile) {
          form.append('file', webFile, asset.name ?? 'import.csv');
        } else {
          const response = await fetch(asset.uri);
          const blob = await response.blob();
          form.append('file', blob, asset.name ?? 'import.csv');
        }
      } else {
        // @ts-expect-error React Native FormData accepts {uri,name,type}.
        form.append('file', {
          uri: asset.uri,
          name: asset.name ?? 'import.csv',
          type: asset.mimeType || 'text/csv',
        });
      }

      const result2 = await api.importProductsCsv(form);
      setSummary(result2);
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        setUnavailable(true);
      } else if (e instanceof ApiError) {
        setError(e.message);
      } else {
        setError('Unexpected error while importing the file.');
      }
    } finally {
      setUploading(false);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>CSV Import</Text>
      <Text style={styles.hint}>
        Upload a CSV with columns: sku, name, description, categoryId, price, stock, active
      </Text>

      <TouchableOpacity style={styles.button} onPress={pickAndUpload} disabled={uploading}>
        {uploading ? <ActivityIndicator color="#fff" /> : <Text style={styles.buttonText}>Choose CSV file</Text>}
      </TouchableOpacity>

      {pickedName ? <Text style={styles.fileName}>Selected: {pickedName}</Text> : null}

      {unavailable ? (
        <Text style={styles.warning}>
          The import endpoint (POST /api/v1/products/import) is not available on this backend yet. This
          screen will start working automatically once it ships.
        </Text>
      ) : null}

      {error ? <Text style={styles.error}>{error}</Text> : null}

      {summary ? (
        <View style={styles.summaryBox}>
          <Text style={styles.summaryTitle}>Import result</Text>
          <Text>Total rows: {summary.totalRows}</Text>
          <Text>Imported: {summary.imported}</Text>
          <Text>Failed: {summary.failed}</Text>
          {summary.errors.length > 0 ? (
            <View style={{ marginTop: 8 }}>
              <Text style={{ fontWeight: '700' }}>Errors:</Text>
              {summary.errors.map((err, idx) => (
                <Text key={idx} style={styles.errorRow}>
                  Row {err.rowNumber} ({err.sku}): {err.message}
                </Text>
              ))}
            </View>
          ) : null}
        </View>
      ) : null}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 16, gap: 8 },
  title: { fontSize: 20, fontWeight: '700' },
  hint: { color: '#666' },
  button: {
    marginTop: 16,
    backgroundColor: '#2563eb',
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
  },
  buttonText: { color: '#fff', fontWeight: '700' },
  fileName: { marginTop: 12, color: '#374151' },
  warning: { marginTop: 16, color: '#92400e', backgroundColor: '#fef3c7', padding: 10, borderRadius: 8 },
  error: { marginTop: 16, color: '#b00020' },
  summaryBox: { marginTop: 20, padding: 12, borderWidth: 1, borderColor: '#e5e5e5', borderRadius: 8 },
  summaryTitle: { fontWeight: '700', marginBottom: 6 },
  errorRow: { color: '#b00020', marginTop: 2 },
});
