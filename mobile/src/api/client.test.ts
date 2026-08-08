import { api, ApiError, setAuthToken } from './client';

describe('api client', () => {
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
      text: async () => (body === undefined ? '' : JSON.stringify(body)),
    }) as unknown as typeof fetch;
  }

  it('sends the Authorization header once a token is set', async () => {
    mockFetch(200, { id: 1, sku: 'ABC', name: 'Widget' });
    setAuthToken('token-123');

    await api.getProduct(1);

    const [, init] = (global.fetch as jest.Mock).mock.calls[0];
    expect(init.headers.Authorization).toBe('Bearer token-123');
  });

  it('does not send an Authorization header when no token is set', async () => {
    mockFetch(200, []);

    await api.getCategories();

    const [, init] = (global.fetch as jest.Mock).mock.calls[0];
    expect(init.headers.Authorization).toBeUndefined();
  });

  it('returns undefined for 204 No Content responses without parsing the body', async () => {
    mockFetch(204, undefined);

    const result = await api.deleteProduct(1);

    expect(result).toBeUndefined();
  });

  it('throws ApiError with the server message on a non-2xx response', async () => {
    mockFetch(409, { message: 'SKU already exists' });

    await expect(
      api.createProduct({ sku: 'DUP', name: 'x', description: 'x', categoryId: 1, price: 1, stock: 1, active: true }),
    ).rejects.toMatchObject({
      status: 409,
      message: 'SKU already exists',
    });
  });

  it('falls back to statusText when the error body has no message/error field', async () => {
    mockFetch(500, { detail: 'unrelated field' });

    await expect(api.getProduct(1)).rejects.toMatchObject({
      status: 500,
      message: 'status text',
    });
  });

  it('wraps network failures (fetch throwing) in an ApiError with status 0', async () => {
    global.fetch = jest.fn().mockRejectedValue(new Error('ECONNREFUSED')) as unknown as typeof fetch;

    await expect(api.getCategories()).rejects.toBeInstanceOf(ApiError);
    await expect(api.getCategories()).rejects.toMatchObject({ status: 0 });
  });

  it('builds paginated query strings with page/size and URL-encodes search prefixes', async () => {
    mockFetch(200, { content: [], totalElements: 0 });

    await api.searchProducts('a b&c', 2, 10);

    const [url] = (global.fetch as jest.Mock).mock.calls[0];
    expect(url).toContain('/api/v1/products/search?prefix=a%20b%26c&page=2&size=10');
  });

  it('sends multipart bodies without a Content-Type override, letting fetch set the boundary', async () => {
    mockFetch(200, { imported: 1, skipped: 0 });
    const form = new FormData();

    await api.importProductsCsv(form);

    const [, init] = (global.fetch as jest.Mock).mock.calls[0];
    expect(init.headers['Content-Type']).toBeUndefined();
    expect(init.body).toBe(form);
  });
});
