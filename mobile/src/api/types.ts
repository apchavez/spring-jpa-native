export type Role = 'ADMIN' | 'USER';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  roles: Role[];
}

export interface Category {
  id: number;
  name: string;
}

export interface CategoryRequest {
  name: string;
}

export interface ProductResponse {
  id: number;
  sku: string;
  name: string;
  description: string;
  categoryId: number;
  categoryName: string;
  price: number;
  stock: number;
  active: boolean;
}

export interface ProductRequest {
  sku: string;
  name: string;
  description: string;
  categoryId: number;
  price: number;
  stock: number;
  active: boolean;
}

export type ProductUpdateRequest = ProductRequest;

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ImportRowError {
  rowNumber: number;
  sku: string;
  message: string;
}

export interface ImportSummary {
  totalRows: number;
  imported: number;
  failed: number;
  errors: ImportRowError[];
}

export interface CategoryBreakdown {
  categoryId: number;
  categoryName: string;
  productCount: number;
  totalStock: number;
  totalValue: number;
}

export interface InventorySummary {
  activeProductCount: number;
  inactiveProductCount: number;
  totalActiveStock: number;
  totalActiveValue: number;
  byCategory: CategoryBreakdown[];
}
