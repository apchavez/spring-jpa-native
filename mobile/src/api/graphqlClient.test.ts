import { ApiError, setAuthToken } from './client';
import { fetchProductDetail, fetchProductsPage } from './graphqlClient';

describe('graphqlClient', () => {
  const originalFetch = global.fetch;

  afterEach(() => {
    global.fetch = originalFetch;
    setAuthToken(null);
    jest.clearAllMocks();
  });

  function mockFetch(status: number, body: unknown, ok = status >= 200 && status < 300) {
    global.fetch = jest.fn().mockResolvedValue({
      ok,
      status,
      statusText: 'status text',
      json: async () => body,
    }) as unknown as typeof fetch;
  }

  it('posts to /graphql with the query and variables in the body', async () => {
    mockFetch(200, { data: { products: { totalCount: 0, page: 0, size: 20, items: [] } } });

    await fetchProductsPage(0, 20, true);

    const [url, init] = (global.fetch as jest.Mock).mock.calls[0];
    expect(url).toMatch(/\/graphql$/);
    const body = JSON.parse(init.body);
    expect(body.query).toContain('products(');
    expect(body.variables).toEqual({ page: 0, size: 20, activeOnly: true });
  });

  it('sends the Authorization header once a token is set, same as the REST client', async () => {
    mockFetch(200, { data: { products: { totalCount: 0, page: 0, size: 20, items: [] } } });
    setAuthToken('token-123');

    await fetchProductsPage(0, 20, true);

    const [, init] = (global.fetch as jest.Mock).mock.calls[0];
    expect(init.headers.Authorization).toBe('Bearer token-123');
  });

  it('maps a products query response into the same Page<ProductResponse> shape as REST', async () => {
    mockFetch(200, {
      data: {
        products: {
          totalCount: 1,
          page: 0,
          size: 20,
          items: [
            { id: 1, sku: 'ABC', name: 'Widget', category: { id: 9, name: 'Gadgets' }, price: 9.99, stock: 5, active: true },
          ],
        },
      },
    });

    const result = await fetchProductsPage(0, 20, true);

    expect(result.content).toHaveLength(1);
    expect(result.content[0]).toMatchObject({
      id: 1,
      sku: 'ABC',
      categoryName: 'Gadgets',
      price: 9.99,
    });
    expect(result.totalElements).toBe(1);
    expect(result.last).toBe(true);
  });

  it('maps a product-detail query response, including the full nested category', async () => {
    mockFetch(200, {
      data: {
        product: {
          id: 1,
          sku: 'ABC',
          name: 'Widget',
          description: 'A widget',
          price: 9.99,
          stock: 5,
          active: true,
          category: { id: 9, name: 'Gadgets' },
        },
      },
    });

    const result = await fetchProductDetail(1);

    expect(result).toMatchObject({
      id: 1,
      description: 'A widget',
      categoryId: 9,
      categoryName: 'Gadgets',
    });
  });

  it('throws ApiError when the GraphQL response contains errors, even with HTTP 200', async () => {
    mockFetch(200, { errors: [{ message: 'Unauthorized' }] });

    await expect(fetchProductsPage(0, 20, true)).rejects.toThrow(ApiError);
  });

  it('throws ApiError on a non-2xx HTTP response', async () => {
    mockFetch(401, {}, false);

    await expect(fetchProductsPage(0, 20, true)).rejects.toThrow(ApiError);
  });

  it('throws ApiError when the network request itself fails', async () => {
    global.fetch = jest.fn().mockRejectedValue(new TypeError('Network request failed')) as unknown as typeof fetch;

    await expect(fetchProductsPage(0, 20, true)).rejects.toThrow(ApiError);
  });
});
