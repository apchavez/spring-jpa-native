import { Redirect, Tabs } from 'expo-router';
import React from 'react';
import { ActivityIndicator, View } from 'react-native';

import { useAuth } from '../../src/context/AuthContext';

export default function TabsLayout() {
  const { token, isLoading, isAdmin } = useAuth();

  if (isLoading) {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
        <ActivityIndicator />
      </View>
    );
  }

  if (!token) {
    return <Redirect href="/login" />;
  }

  return (
    <Tabs screenOptions={{ headerTitleAlign: 'center' }}>
      <Tabs.Screen name="index" options={{ title: 'Products' }} />
      <Tabs.Screen name="reports" options={{ title: 'Reports' }} />
      <Tabs.Screen
        name="inactive"
        options={{ title: 'Inactive', href: isAdmin ? undefined : null }}
      />
      <Tabs.Screen
        name="import"
        options={{ title: 'Import CSV', href: isAdmin ? undefined : null }}
      />
    </Tabs>
  );
}
