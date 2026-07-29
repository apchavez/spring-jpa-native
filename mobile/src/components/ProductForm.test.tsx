import React from 'react';
import { Alert } from 'react-native';
import { fireEvent, render, screen } from '@testing-library/react-native';
import ProductForm from './ProductForm';
import type { ProductResponse } from '../api/types';

jest.mock('./CategoryPicker', () => {
  const { View, Text, TouchableOpacity } = require('react-native');
  return function MockCategoryPicker({ onChange }: { value: number | null; onChange: (id: number, name: string) => void }) {
    return (
      <View>
        <TouchableOpacity testID="pick-category" onPress={() => onChange(7, 'Electronics')}>
          <Text>Pick Electronics</Text>
        </TouchableOpacity>
      </View>
    );
  };
});

const product: ProductResponse = {
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

describe('ProductForm', () => {
  let alertSpy: jest.SpyInstance;

  beforeEach(() => {
    alertSpy = jest.spyOn(Alert, 'alert').mockImplementation(() => {});
  });

  afterEach(() => {
    alertSpy.mockRestore();
  });

  it('pre-fills fields from the initial product when editing', () => {
    render(<ProductForm initial={product} submitLabel="Save" submitting={false} onSubmit={jest.fn()} />);

    expect(screen.getByDisplayValue('WIDGET-001')).toBeTruthy();
    expect(screen.getByDisplayValue('Widget')).toBeTruthy();
    expect(screen.getByDisplayValue('9.99')).toBeTruthy();
    expect(screen.getByDisplayValue('42')).toBeTruthy();
    expect(screen.getByText('Selected: Electronics')).toBeTruthy();
  });

  it('renders blank fields with no initial product (create mode)', () => {
    render(<ProductForm submitLabel="Create" submitting={false} onSubmit={jest.fn()} />);

    expect(screen.getByPlaceholderText('e.g. WIDGET-001').props.value).toBe('');
    expect(screen.queryByText(/Selected:/)).toBeNull();
  });

  it('blocks submit and alerts when SKU is missing', () => {
    const onSubmit = jest.fn();
    render(<ProductForm submitLabel="Create" submitting={false} onSubmit={onSubmit} />);

    fireEvent.changeText(screen.getByPlaceholderText('Product name'), 'Widget');
    fireEvent.press(screen.getByText('Create'));

    expect(alertSpy).toHaveBeenCalledWith('Missing fields', 'SKU and name are required.');
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('blocks submit and alerts when no category is selected', () => {
    const onSubmit = jest.fn();
    render(<ProductForm submitLabel="Create" submitting={false} onSubmit={onSubmit} />);

    fireEvent.changeText(screen.getByPlaceholderText('e.g. WIDGET-001'), 'SKU-1');
    fireEvent.changeText(screen.getByPlaceholderText('Product name'), 'Name');
    fireEvent.press(screen.getByText('Create'));

    expect(alertSpy).toHaveBeenCalledWith('Missing category', 'Please select a category.');
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('blocks submit and alerts when price/stock are not numeric', () => {
    const onSubmit = jest.fn();
    render(<ProductForm submitLabel="Create" submitting={false} onSubmit={onSubmit} />);

    fireEvent.changeText(screen.getByPlaceholderText('e.g. WIDGET-001'), 'SKU-1');
    fireEvent.changeText(screen.getByPlaceholderText('Product name'), 'Name');
    fireEvent.press(screen.getByTestId('pick-category'));
    fireEvent.changeText(screen.getByPlaceholderText('0.00'), 'not-a-number');
    fireEvent.changeText(screen.getByPlaceholderText('0'), '5');
    fireEvent.press(screen.getByText('Create'));

    expect(alertSpy).toHaveBeenCalledWith('Invalid number', 'Price and stock must be numeric.');
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('submits trimmed, parsed values once all fields are valid', () => {
    const onSubmit = jest.fn();
    render(<ProductForm submitLabel="Create" submitting={false} onSubmit={onSubmit} />);

    fireEvent.changeText(screen.getByPlaceholderText('e.g. WIDGET-001'), '  sku-1  ');
    fireEvent.changeText(screen.getByPlaceholderText('Product name'), '  Name  ');
    fireEvent.changeText(screen.getByPlaceholderText('Description'), '  Desc  ');
    fireEvent.press(screen.getByTestId('pick-category'));
    fireEvent.changeText(screen.getByPlaceholderText('0.00'), '12.5');
    fireEvent.changeText(screen.getByPlaceholderText('0'), '3');
    fireEvent.press(screen.getByText('Create'));

    expect(alertSpy).not.toHaveBeenCalled();
    expect(onSubmit).toHaveBeenCalledWith({
      sku: 'sku-1',
      name: 'Name',
      description: 'Desc',
      categoryId: 7,
      price: 12.5,
      stock: 3,
      active: true,
    });
  });

  it('toggles the active switch', () => {
    const onSubmit = jest.fn();
    render(<ProductForm submitLabel="Create" submitting={false} onSubmit={onSubmit} />);

    fireEvent.changeText(screen.getByPlaceholderText('e.g. WIDGET-001'), 'SKU-1');
    fireEvent.changeText(screen.getByPlaceholderText('Product name'), 'Name');
    fireEvent.press(screen.getByTestId('pick-category'));
    fireEvent.changeText(screen.getByPlaceholderText('0.00'), '1');
    fireEvent.changeText(screen.getByPlaceholderText('0'), '1');

    const toggle = screen.UNSAFE_getByType(require('react-native').Switch);
    fireEvent(toggle, 'valueChange', false);

    fireEvent.press(screen.getByText('Create'));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ active: false }));
  });

  it('shows a spinner and disables the button while submitting', () => {
    render(<ProductForm submitLabel="Create" submitting onSubmit={jest.fn()} />);

    expect(screen.queryByText('Create')).toBeNull();
    expect(screen.getByTestId('submit-button').props.accessibilityState?.disabled).toBe(true);
  });
});
