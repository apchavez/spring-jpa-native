import { Platform } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { storage } from './storage';

jest.mock('@react-native-async-storage/async-storage', () => ({
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
}));

describe('storage', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('falls back to AsyncStorage on web', async () => {
    const originalOS = Platform.OS;
    Object.defineProperty(Platform, 'OS', { get: () => 'web' });

    (AsyncStorage.getItem as jest.Mock).mockResolvedValue('stored-value');

    const value = await storage.getItem('auth_token');

    expect(AsyncStorage.getItem).toHaveBeenCalledWith('auth_token');
    expect(value).toBe('stored-value');

    Object.defineProperty(Platform, 'OS', { get: () => originalOS });
  });

  it('setItem on web delegates to AsyncStorage.setItem with the same key/value', async () => {
    const originalOS = Platform.OS;
    Object.defineProperty(Platform, 'OS', { get: () => 'web' });

    await storage.setItem('auth_token', 'abc');

    expect(AsyncStorage.setItem).toHaveBeenCalledWith('auth_token', 'abc');

    Object.defineProperty(Platform, 'OS', { get: () => originalOS });
  });

  it('removeItem on web delegates to AsyncStorage.removeItem', async () => {
    const originalOS = Platform.OS;
    Object.defineProperty(Platform, 'OS', { get: () => 'web' });

    await storage.removeItem('auth_token');

    expect(AsyncStorage.removeItem).toHaveBeenCalledWith('auth_token');

    Object.defineProperty(Platform, 'OS', { get: () => originalOS });
  });
});
