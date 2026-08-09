import type {
  Category,
  CategoryRequest,
  ImportSummary,
  InventorySummary,
  LoginRequest,
  LoginResponse,
  Page,
  ProductRequest,
  ProductResponse,
  ProductUpdateRequest,
} from './types';

export const API_BASE_URL =
  process.env.EXPO_PUBLIC_API_URL?.replace(/\/$/, '') || 'http://localhost:8080';

export class ApiError extends Error {
  status: number;
  body: unknown;

  constructor(status: number, message: string, body?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

let authToken: string | null = null;
export function setAuthToken(token: string | null): void {
  authToken = token;
}
export function getAuthToken(): string | null {
  return authToken;
}

async function parseBody(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) return undefined;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function extractMessage(body: unknown, fallback: string): string {
  if (body && typeof body === 'object') {
    const anyBody = body as Record<string, unknown>;
    if (typeof anyBody.message === 'string') return anyBody.message;
    if (typeof anyBody.error === 'string') return anyBody.error;
  }
  if (typeof body === 'string' && body.length > 0) return body;
  return fallback;
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  headers?: Record<string, string>;
  isMultipart?: boolean;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, headers = {}, isMultipart = false } = options;

  const finalHeaders: Record<string, string> = { ...headers };
  if (!isMultipart) {
    finalHeaders['Content-Type'] = 'application/json';
  }
  if (authToken) {
    finalHeaders['Authorization'] = `Bearer ${authToken}`;
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers: finalHeaders,
      body: isMultipart ? (body as FormData) : body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (networkError) {
    throw new ApiError(
      0,
      `Could not reach the API at ${API_BASE_URL}. Check EXPO_PUBLIC_API_URL and that the backend is running.`,
      networkError,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const parsed = await parseBody(response);

  if (!response.ok) {
    throw new ApiError(response.status, extractMessage(parsed, response.statusText), parsed);
  }

  return parsed as T;
}

export const api = {
  login(credentials: LoginRequest): Promise<LoginResponse> {
    return request<LoginResponse>('/api/v1/auth/login', { method: 'POST', body: credentials });
  },

  getCategories(): Promise<Category[]> {
    return request<Category[]>('/api/v1/categories');
  },

  createCategory(body: CategoryRequest): Promise<Category> {
    return request<Category>('/api/v1/categories', { method: 'POST', body });
  },

  getActiveProducts(page = 0, size = 20): Promise<Page<ProductResponse>> {
    return request<Page<ProductResponse>>(`/api/v1/products/active?page=${page}&size=${size}`);
  },

  getInactiveProducts(page = 0, size = 20): Promise<Page<ProductResponse>> {
    return request<Page<ProductResponse>>(`/api/v1/products/inactive?page=${page}&size=${size}`);
  },

  searchProducts(prefix: string, page = 0, size = 20): Promise<Page<ProductResponse>> {
    return request<Page<ProductResponse>>(
      `/api/v1/products/search?prefix=${encodeURIComponent(prefix)}&page=${page}&size=${size}`,
    );
  },

  getProduct(id: number): Promise<ProductResponse> {
    return request<ProductResponse>(`/api/v1/products/${id}`);
  },

  createProduct(body: ProductRequest): Promise<ProductResponse> {
    return request<ProductResponse>('/api/v1/products', { method: 'POST', body });
  },

  updateProduct(id: number, body: ProductUpdateRequest): Promise<ProductResponse> {
    return request<ProductResponse>(`/api/v1/products/${id}`, { method: 'PUT', body });
  },

  deleteProduct(id: number): Promise<void> {
    return request<void>(`/api/v1/products/${id}`, { method: 'DELETE' });
  },

  importProductsCsv(form: FormData): Promise<ImportSummary> {
    return request<ImportSummary>('/api/v1/products/import', {
      method: 'POST',
      body: form,
      isMultipart: true,
    });
  },

  getInventorySummary(): Promise<InventorySummary> {
    return request<InventorySummary>('/api/v1/reports/inventory-summary');
  },
};
