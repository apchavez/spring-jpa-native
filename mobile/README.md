# Product Service Mobile

React Native + Expo (Expo Router, TypeScript) client for the Product Service
API backend (`../api`, Spring Boot / `com.apchavez.products`).

## Setup

```
npm install
```

## Running

- Expo Go / simulator / emulator:
  ```
  npx expo start
  ```
  Scan the QR code with Expo Go, or press `i` / `a` for iOS simulator / Android emulator.

- Web preview:
  ```
  npx expo start --web
  ```

## Pointing at the backend

The API base URL is read from `EXPO_PUBLIC_API_URL` (see `src/api/client.ts`)
and defaults to `http://localhost:8080`.

Copy `.env.example` to `.env` and adjust if needed:

```
cp .env.example .env
```

- Web preview, iOS simulator, Android emulator running on the **same
  machine** as the backend: the default `http://localhost:8080` works.
- A **physical device** in Expo Go cannot resolve `localhost` to your
  computer - point it at the backend machine's LAN IP instead, e.g.
  `EXPO_PUBLIC_API_URL=http://192.168.1.50:8080`, then restart `expo start`.

## Demo credentials

- `admin` / `admin123` - ADMIN role (sees Inactive Products and CSV Import tabs)
- `user` / `user123` - USER role (Products and Reports tabs only)

## Notes / known limitations

- `expo-secure-store` has no web implementation; the storage layer
  (`src/api/storage.ts`) automatically falls back to
  `@react-native-async-storage/async-storage` (which itself uses
  `localStorage` under the hood) when running on web, so the app still works
  in a browser.
- The CSV Import and Inventory Reports screens call endpoints
  (`POST /api/v1/products/import`, `GET /api/v1/reports/inventory-summary`)
  that may not exist on the backend yet. Both screens detect a 404 and show a
  friendly "not available yet" message instead of crashing.
