import { API_BASE_URL, ApiError, getAuthToken } from './client';
import type { Category, Page, ProductResponse } from './types';

async function graphqlRequest<T>(query: string, variables?: Record<string, unknown>): Promise<T> {
  const token = getAuthToken();
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}/graphql`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ query, variables }),
    });
  } catch (networkError) {
    throw new ApiError(
      0,
      `Could not reach the GraphQL endpoint at ${API_BASE_URL}/graphql.`,
      networkError,
    );
  }

  if (!response.ok) {
    throw new ApiError(response.status, response.statusText);
  }

  const json = (await response.json()) as { data?: T; errors?: { message: string }[] };
  if (json.errors && json.errors.length > 0) {
    throw new ApiError(200, json.errors.map((e) => e.message).join('; '), json.errors);
  }
  return json.data as T;
}

interface ProductsQueryResult {
  products: {
    items: Array<Pick<ProductResponse, 'id' | 'sku' | 'name' | 'price' | 'stock' | 'active'> & {
      category: Category;
    }>;
    totalCount: number;
    page: number;
    size: number;
  };
}

const PRODUCTS_LIST_QUERY = `
  query ProductsList($page: Int!, $size: Int!, $activeOnly: Boolean!) {
    products(page: $page, size: $size, activeOnly: $activeOnly) {
      totalCount
      page
      size
      items {
        id
        sku
        name
        category { name }
        price
        stock
        active
      }
    }
  }
`;

/** Same {@link Page} shape the REST client returns, so `ProductList` needs no changes to consume it. */
export async function fetchProductsPage(
  page: number,
  size: number,
  activeOnly: boolean,
): Promise<Page<ProductResponse>> {
  const result = await graphqlRequest<ProductsQueryResult>(PRODUCTS_LIST_QUERY, {
    page,
    size,
    activeOnly,
  });
  const { items, totalCount } = result.products;
  const totalPages = Math.max(1, Math.ceil(totalCount / size));
  return {
    content: items.map((item) => ({
      id: item.id,
      sku: item.sku,
      name: item.name,
      description: '',
      categoryId: 0,
      categoryName: item.category.name,
      price: item.price,
      stock: item.stock,
      active: item.active,
    })),
    page,
    size,
    totalElements: totalCount,
    totalPages,
    last: page + 1 >= totalPages,
  };
}

interface ProductQueryResult {
  product: ProductResponse & { category: Category };
}

const PRODUCT_DETAIL_QUERY = `
  query ProductDetail($id: ID!) {
    product(id: $id) {
      id
      sku
      name
      description
      price
      stock
      active
      category { id name }
    }
  }
`;

export async function fetchProductDetail(id: number): Promise<ProductResponse> {
  const result = await graphqlRequest<ProductQueryResult>(PRODUCT_DETAIL_QUERY, { id });
  const { product } = result;
  return {
    id: product.id,
    sku: product.sku,
    name: product.name,
    description: product.description,
    categoryId: product.category.id,
    categoryName: product.category.name,
    price: product.price,
    stock: product.stock,
    active: product.active,
  };
}
