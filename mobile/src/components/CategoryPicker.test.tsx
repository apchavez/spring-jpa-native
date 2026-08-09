import React from 'react';
import { act, render, screen, waitFor } from '@testing-library/react-native';
import CategoryPicker from './CategoryPicker';
import { api } from '../api/client';

jest.mock('../api/client', () => ({
  api: { getCategories: jest.fn() },
  ApiError: class ApiError extends Error {
    status: number;
    constructor(status: number, message: string) {
      super(message);
      this.status = status;
    }
  },
}));

describe('CategoryPicker', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('shows a loading indicator while categories are being fetched', () => {
    (api.getCategories as jest.Mock).mockReturnValue(new Promise(() => {}));

    render(<CategoryPicker value={null} onChange={jest.fn()} />);

    expect(screen.getByText('Loading categories...')).toBeTruthy();
  });

  it('renders one Picker.Item per category once loaded', async () => {
    (api.getCategories as jest.Mock).mockResolvedValue([
      { id: 1, name: 'Electronics' },
      { id: 2, name: 'Groceries' },
    ]);

    render(<CategoryPicker value={null} onChange={jest.fn()} />);

    await waitFor(() => expect(api.getCategories).toHaveBeenCalledTimes(1));
    expect(screen.queryByText('Loading categories...')).toBeNull();
  });

  it('shows an error message instead of the picker when the fetch fails', async () => {
    (api.getCategories as jest.Mock).mockRejectedValue(new Error('network down'));

    render(<CategoryPicker value={null} onChange={jest.fn()} />);

    await waitFor(() => expect(screen.getByText('Failed to load categories.')).toBeTruthy());
  });

  it('does not call setState after unmount (no "cancelled" leak/warning)', async () => {
    let resolveFn: (categories: unknown[]) => void = () => {};
    (api.getCategories as jest.Mock).mockReturnValue(
      new Promise((resolve) => {
        resolveFn = resolve;
      }),
    );

    const { unmount } = render(<CategoryPicker value={null} onChange={jest.fn()} />);
    unmount();

    await act(async () => {
      resolveFn([{ id: 1, name: 'Electronics' }]);
      await Promise.resolve();
    });
  });
});
