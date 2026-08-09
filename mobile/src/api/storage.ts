import { Platform } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

let SecureStore: typeof import('expo-secure-store') | undefined;
if (Platform.OS !== 'web') {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  SecureStore = require('expo-secure-store');
}

export const storage = {
  async getItem(key: string): Promise<string | null> {
    if (Platform.OS !== 'web' && SecureStore) {
      return SecureStore.getItemAsync(key);
    }
    return AsyncStorage.getItem(key);
  },
  async setItem(key: string, value: string): Promise<void> {
    if (Platform.OS !== 'web' && SecureStore) {
      await SecureStore.setItemAsync(key, value);
      return;
    }
    await AsyncStorage.setItem(key, value);
  },
  async removeItem(key: string): Promise<void> {
    if (Platform.OS !== 'web' && SecureStore) {
      await SecureStore.deleteItemAsync(key);
      return;
    }
    await AsyncStorage.removeItem(key);
  },
};
