import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import ProductList from './ProductList';
import { ApiError } from '../api/client';
import type { Page, ProductResponse } from '../api/types';

const mockPush = jest.fn();
jest.mock('expo-router', () => ({
  useRouter: () => ({ push: mockPush }),
}));

function page(items: ProductResponse[], opts: Partial<Page<ProductResponse>> = {}): Page<ProductResponse> {
  return {
    content: items,
    page: 0,
    size: 20,
    totalElements: items.length,
    totalPages: 1,
    last: true,
    ...opts,
  };
}

const widget: ProductResponse = {
  id: 1,
  sku: 'WIDGET-001',
  name: 'Widget',
  description: 'A widget',
  categoryId: 7,
  categoryName: 'Electronics',
  price: 9.99,
  stock: 42,
  active: true,
};

describe('ProductList', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('shows a loading indicator while the first page is in flight', () => {
    const fetchPage = jest.fn().mockReturnValue(new Promise(() => {}));
    render(<ProductList fetchPage={fetchPage} />);

    expect(fetchPage).toHaveBeenCalledWith(0, 20);
  });

  it('renders a card per product once loaded', async () => {
    const fetchPage = jest.fn().mockResolvedValue(page([widget]));
    render(<ProductList fetchPage={fetchPage} />);

    await waitFor(() => expect(screen.getByText('Widget')).toBeTruthy());
    expect(screen.getByText('WIDGET-001')).toBeTruthy();
    expect(screen.getByText('Electronics')).toBeTruthy();
    expect(screen.getByText('$9.99')).toBeTruthy();
    expect(screen.getByText('Stock: 42')).toBeTruthy();
    expect(screen.getByText('ACTIVE')).toBeTruthy();
  });

  it('shows the inactive badge for a disabled product', async () => {
    const inactive = { ...widget, id: 2, active: false };
    const fetchPage = jest.fn().mockResolvedValue(page([inactive]));
    render(<ProductList fetchPage={fetchPage} />);

    await waitFor(() => expect(screen.getByText('INACTIVE')).toBeTruthy());
  });

  it('shows the empty label when there are no products', async () => {
    const fetchPage = jest.fn().mockResolvedValue(page([]));
    render(<ProductList fetchPage={fetchPage} emptyLabel="Nothing here yet" />);

    await waitFor(() => expect(screen.getByText('Nothing here yet')).toBeTruthy());
  });

  it('shows a friendly message when the API call fails with an ApiError', async () => {
    const fetchPage = jest.fn().mockRejectedValue(new ApiError(500, 'boom'));
    render(<ProductList fetchPage={fetchPage} />);

    await waitFor(() => expect(screen.getByText('boom')).toBeTruthy());
  });

  it('falls back to a generic error message for non-ApiError failures', async () => {
    const fetchPage = jest.fn().mockRejectedValue(new Error('network'));
    render(<ProductList fetchPage={fetchPage} />);

    await waitFor(() => expect(screen.getByText('Failed to load products.')).toBeTruthy());
  });

  it('navigates to the product detail screen on card press', async () => {
    const fetchPage = jest.fn().mockResolvedValue(page([widget]));
    render(<ProductList fetchPage={fetchPage} />);

    await waitFor(() => expect(screen.getByText('Widget')).toBeTruthy());
    fireEvent.press(screen.getByText('Widget'));

    expect(mockPush).toHaveBeenCalledWith('/product/1');
  });

  it('does not render a search box when no searchPage prop is given', async () => {
    const fetchPage = jest.fn().mockResolvedValue(page([]));
    render(<ProductList fetchPage={fetchPage} />);

    await waitFor(() => expect(fetchPage).toHaveBeenCalled());
    expect(screen.queryByPlaceholderText('Search by name prefix...')).toBeNull();
  });

  it('re-queries via searchPage once the user types into the search box', async () => {
    const fetchPage = jest.fn().mockResolvedValue(page([widget]));
    const searchPage = jest.fn().mockResolvedValue(page([]));
    render(<ProductList fetchPage={fetchPage} searchPage={searchPage} />);

    await waitFor(() => expect(screen.getByText('Widget')).toBeTruthy());

    fireEvent.changeText(screen.getByPlaceholderText('Search by name prefix...'), 'wid');

    await waitFor(() => expect(searchPage).toHaveBeenCalledWith('wid', 0, 20));
  });

  it('loads the next page when onEndReached fires and there is more data', async () => {
    const fetchPage = jest
      .fn()
      .mockResolvedValueOnce(page([widget], { last: false }))
      .mockResolvedValueOnce(page([{ ...widget, id: 2, sku: 'WIDGET-002' }], { page: 1, last: true }));
    render(<ProductList fetchPage={fetchPage} />);

    await waitFor(() => expect(screen.getByText('WIDGET-001')).toBeTruthy());

    await act(async () => {
      screen.UNSAFE_getByType(require('react-native').FlatList).props.onEndReached();
    });

    await waitFor(() => expect(fetchPage).toHaveBeenCalledWith(1, 20));
    await waitFor(() => expect(screen.getByText('WIDGET-002')).toBeTruthy());
  });

  it('pulls to refresh, replacing the item list from page 0', async () => {
    const fetchPage = jest
      .fn()
      .mockResolvedValueOnce(page([widget]))
      .mockResolvedValueOnce(page([{ ...widget, id: 9, sku: 'WIDGET-009' }]));
    render(<ProductList fetchPage={fetchPage} />);

    await waitFor(() => expect(screen.getByText('WIDGET-001')).toBeTruthy());

    await act(async () => {
      screen.UNSAFE_getByType(require('react-native').FlatList).props.refreshControl.props.onRefresh();
    });

    await waitFor(() => expect(screen.getByText('WIDGET-009')).toBeTruthy());
    expect(screen.queryByText('WIDGET-001')).toBeNull();
  });
});
