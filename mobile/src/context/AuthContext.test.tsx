import React from 'react';
import { act, render, renderHook, screen, waitFor } from '@testing-library/react-native';
import { Text } from 'react-native';
import { AuthProvider, useAuth } from './AuthContext';
import { api, ApiError, setAuthToken } from '../api/client';
import { storage } from '../api/storage';

jest.mock('../api/client', () => ({
  api: { login: jest.fn() },
  ApiError: class ApiError extends Error {
    status: number;
    constructor(status: number, message: string) {
      super(message);
      this.status = status;
    }
  },
  setAuthToken: jest.fn(),
}));

jest.mock('../api/storage', () => ({
  storage: { getItem: jest.fn(), setItem: jest.fn(), removeItem: jest.fn() },
}));

const wrapper = ({ children }: { children: React.ReactNode }) => <AuthProvider>{children}</AuthProvider>;

describe('AuthContext', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('starts with isLoading=true and no session when storage is empty', async () => {
    (storage.getItem as jest.Mock).mockResolvedValue(null);

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.token).toBeNull();
    expect(result.current.isAdmin).toBe(false);
  });

  it('restores a previously stored session on mount', async () => {
    (storage.getItem as jest.Mock).mockResolvedValue(
      JSON.stringify({ token: 'stored-token', username: 'admin', roles: ['ADMIN'] }),
    );

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.token).toBe('stored-token');
    expect(result.current.isAdmin).toBe(true);
    expect(setAuthToken).toHaveBeenCalledWith('stored-token');
  });

  it('does not crash and just logs the user out when stored data is corrupted JSON', async () => {
    (storage.getItem as jest.Mock).mockResolvedValue('not-valid-json{');

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.token).toBeNull();
  });

  it('login() persists the session and flips isAdmin for an ADMIN role', async () => {
    (storage.getItem as jest.Mock).mockResolvedValue(null);
    (api.login as jest.Mock).mockResolvedValue({ token: 'new-token', username: 'admin', roles: ['ADMIN'] });

    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    let success = false;
    await act(async () => {
      success = await result.current.login('admin', 'admin123');
    });

    expect(success).toBe(true);
    expect(result.current.token).toBe('new-token');
    expect(result.current.isAdmin).toBe(true);
    expect(storage.setItem).toHaveBeenCalledWith(
      'product-service-auth',
      JSON.stringify({ token: 'new-token', username: 'admin', roles: ['ADMIN'] }),
    );
  });

  it('login() sets a specific "invalid credentials" error on a 401, without touching storage', async () => {
    (storage.getItem as jest.Mock).mockResolvedValue(null);
    (api.login as jest.Mock).mockRejectedValue(new ApiError(401, 'Unauthorized'));

    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    let success = true;
    await act(async () => {
      success = await result.current.login('admin', 'wrong-password');
    });

    expect(success).toBe(false);
    expect(result.current.error).toBe('Invalid username or password.');
    expect(result.current.token).toBeNull();
    expect(storage.setItem).not.toHaveBeenCalled();
  });

  it('logout() clears in-memory state and removes the persisted session', async () => {
    (storage.getItem as jest.Mock).mockResolvedValue(
      JSON.stringify({ token: 'stored-token', username: 'admin', roles: ['ADMIN'] }),
    );

    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(async () => {
      await result.current.logout();
    });

    expect(result.current.token).toBeNull();
    expect(result.current.isAdmin).toBe(false);
    expect(storage.removeItem).toHaveBeenCalledWith('product-service-auth');
    expect(setAuthToken).toHaveBeenCalledWith(null);
  });

  it('useAuth() throws when used outside an AuthProvider', () => {
    const Bare = () => {
      useAuth();
      return <Text>never renders</Text>;
    };
    const spy = jest.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<Bare />)).toThrow('useAuth must be used within an AuthProvider');
    spy.mockRestore();
  });
});
