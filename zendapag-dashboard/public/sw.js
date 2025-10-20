// Service Worker for Zendapag Dashboard
// Provides offline functionality and caching

const CACHE_NAME = 'zendapag-dashboard-v1';
const API_CACHE_NAME = 'zendapag-api-v1';

// Resources to cache immediately
const STATIC_RESOURCES = [
  '/',
  '/static/js/bundle.js',
  '/static/css/main.css',
  '/manifest.json',
];

// API endpoints to cache
const API_CACHE_PATTERNS = [
  /^\/api\/v1\/merchants\/me$/,
  /^\/api\/v1\/analytics\/dashboard/,
  /^\/api\/v1\/payments\?page=1/,
];

// Install event - cache static resources
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        return cache.addAll(STATIC_RESOURCES);
      })
      .then(() => {
        return self.skipWaiting();
      })
  );
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((cacheNames) => {
        return Promise.all(
          cacheNames.map((cacheName) => {
            if (cacheName !== CACHE_NAME && cacheName !== API_CACHE_NAME) {
              return caches.delete(cacheName);
            }
          })
        );
      })
      .then(() => {
        return self.clients.claim();
      })
  );
});

// Fetch event - handle network requests
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // Handle API requests
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(handleApiRequest(request));
  }
  // Handle static resources
  else {
    event.respondWith(handleStaticRequest(request));
  }
});

// Handle API requests with cache-first or network-first strategy
async function handleApiRequest(request) {
  const url = new URL(request.url);
  const method = request.method.toLowerCase();

  // Only cache GET requests
  if (method !== 'get') {
    return handleMutationRequest(request);
  }

  // Check if this API endpoint should be cached
  const shouldCache = API_CACHE_PATTERNS.some(pattern => pattern.test(url.pathname));

  if (shouldCache) {
    return handleCacheableApiRequest(request);
  }

  // For non-cacheable API requests, try network first
  try {
    const response = await fetch(request);
    return response;
  } catch (error) {
    // Return a generic offline response for failed API requests
    return new Response(
      JSON.stringify({
        error: 'Offline',
        message: 'Esta funcionalidade não está disponível offline.',
      }),
      {
        status: 503,
        statusText: 'Service Unavailable',
        headers: { 'Content-Type': 'application/json' },
      }
    );
  }
}

// Handle cacheable API requests with stale-while-revalidate strategy
async function handleCacheableApiRequest(request) {
  const cache = await caches.open(API_CACHE_NAME);
  const cachedResponse = await cache.match(request);

  // If we have a cached response, return it and update in background
  if (cachedResponse) {
    // Revalidate in background
    fetch(request)
      .then(response => {
        if (response.status === 200) {
          cache.put(request, response.clone());
        }
      })
      .catch(() => {
        // Ignore network errors during revalidation
      });

    return cachedResponse;
  }

  // No cached response, try network
  try {
    const response = await fetch(request);
    if (response.status === 200) {
      cache.put(request, response.clone());
    }
    return response;
  } catch (error) {
    // Network failed and no cache - return offline response
    return new Response(
      JSON.stringify({
        error: 'Offline',
        message: 'Dados não disponíveis offline.',
      }),
      {
        status: 503,
        statusText: 'Service Unavailable',
        headers: { 'Content-Type': 'application/json' },
      }
    );
  }
}

// Handle mutation requests (POST, PUT, DELETE)
async function handleMutationRequest(request) {
  try {
    const response = await fetch(request);
    return response;
  } catch (error) {
    // For mutations, we need to queue them for later
    const requestData = {
      url: request.url,
      method: request.method,
      headers: Object.fromEntries(request.headers.entries()),
      body: request.method !== 'GET' ? await request.clone().text() : null,
      timestamp: Date.now(),
    };

    // Store in IndexedDB for later retry
    await storeOfflineMutation(requestData);

    // Return a response indicating the request was queued
    return new Response(
      JSON.stringify({
        success: true,
        message: 'Ação salva. Será sincronizada quando a conexão for restabelecida.',
        queued: true,
      }),
      {
        status: 202,
        statusText: 'Accepted',
        headers: { 'Content-Type': 'application/json' },
      }
    );
  }
}

// Handle static resource requests with cache-first strategy
async function handleStaticRequest(request) {
  const cache = await caches.open(CACHE_NAME);
  const cachedResponse = await cache.match(request);

  if (cachedResponse) {
    return cachedResponse;
  }

  try {
    const response = await fetch(request);
    if (response.status === 200) {
      cache.put(request, response.clone());
    }
    return response;
  } catch (error) {
    // For navigation requests, return the cached index.html
    if (request.mode === 'navigate') {
      const indexResponse = await cache.match('/');
      if (indexResponse) {
        return indexResponse;
      }
    }

    // Return a generic offline page
    return new Response(
      `
      <!DOCTYPE html>
      <html>
        <head>
          <title>Zendapag - Offline</title>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <style>
            body {
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
              display: flex;
              align-items: center;
              justify-content: center;
              min-height: 100vh;
              margin: 0;
              background: #f5f5f5;
              color: #333;
            }
            .container {
              text-align: center;
              padding: 2rem;
              background: white;
              border-radius: 8px;
              box-shadow: 0 2px 10px rgba(0,0,0,0.1);
              max-width: 400px;
            }
            h1 { color: #1890ff; margin-bottom: 1rem; }
            p { margin-bottom: 1.5rem; line-height: 1.5; }
            button {
              background: #1890ff;
              color: white;
              border: none;
              padding: 0.75rem 1.5rem;
              border-radius: 4px;
              cursor: pointer;
              font-size: 1rem;
            }
            button:hover { background: #40a9ff; }
          </style>
        </head>
        <body>
          <div class="container">
            <h1>Zendapag</h1>
            <p>Você está offline. Verifique sua conexão de internet e tente novamente.</p>
            <button onclick="window.location.reload()">Tentar Novamente</button>
          </div>
        </body>
      </html>
      `,
      {
        status: 200,
        headers: { 'Content-Type': 'text/html' },
      }
    );
  }
}

// Store offline mutations in IndexedDB
async function storeOfflineMutation(mutationData) {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('zendapag-offline', 1);

    request.onerror = () => reject(request.error);
    request.onsuccess = () => {
      const db = request.result;
      const transaction = db.transaction(['mutations'], 'readwrite');
      const store = transaction.objectStore('mutations');

      const mutationRequest = store.add({
        ...mutationData,
        id: `mutation_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      });

      mutationRequest.onerror = () => reject(mutationRequest.error);
      mutationRequest.onsuccess = () => resolve(mutationRequest.result);
    };

    request.onupgradeneeded = (event) => {
      const db = event.target.result;
      if (!db.objectStoreNames.contains('mutations')) {
        const store = db.createObjectStore('mutations', { keyPath: 'id' });
        store.createIndex('timestamp', 'timestamp');
      }
    };
  });
}

// Background sync for retrying failed requests
self.addEventListener('sync', (event) => {
  if (event.tag === 'retry-mutations') {
    event.waitUntil(retryOfflineMutations());
  }
});

// Retry offline mutations when connection is restored
async function retryOfflineMutations() {
  const mutations = await getOfflineMutations();

  for (const mutation of mutations) {
    try {
      const request = new Request(mutation.url, {
        method: mutation.method,
        headers: mutation.headers,
        body: mutation.body,
      });

      const response = await fetch(request);

      if (response.ok) {
        await removeOfflineMutation(mutation.id);
      }
    } catch (error) {
      console.error('Failed to retry mutation:', error);
    }
  }
}

// Get offline mutations from IndexedDB
function getOfflineMutations() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('zendapag-offline', 1);

    request.onerror = () => reject(request.error);
    request.onsuccess = () => {
      const db = request.result;
      const transaction = db.transaction(['mutations'], 'readonly');
      const store = transaction.objectStore('mutations');
      const getAllRequest = store.getAll();

      getAllRequest.onerror = () => reject(getAllRequest.error);
      getAllRequest.onsuccess = () => resolve(getAllRequest.result);
    };
  });
}

// Remove offline mutation from IndexedDB
function removeOfflineMutation(id) {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('zendapag-offline', 1);

    request.onerror = () => reject(request.error);
    request.onsuccess = () => {
      const db = request.result;
      const transaction = db.transaction(['mutations'], 'readwrite');
      const store = transaction.objectStore('mutations');
      const deleteRequest = store.delete(id);

      deleteRequest.onerror = () => reject(deleteRequest.error);
      deleteRequest.onsuccess = () => resolve();
    };
  });
}

// Message handling for communication with main thread
self.addEventListener('message', (event) => {
  const { type, data } = event.data;

  switch (type) {
    case 'SKIP_WAITING':
      self.skipWaiting();
      break;

    case 'GET_CACHE_SIZE':
      getCacheSize().then(size => {
        event.ports[0].postMessage({ size });
      });
      break;

    case 'CLEAR_CACHE':
      clearAllCaches().then(() => {
        event.ports[0].postMessage({ success: true });
      });
      break;
  }
});

// Get total cache size
async function getCacheSize() {
  let totalSize = 0;
  const cacheNames = await caches.keys();

  for (const cacheName of cacheNames) {
    const cache = await caches.open(cacheName);
    const keys = await cache.keys();

    for (const key of keys) {
      const response = await cache.match(key);
      if (response) {
        const blob = await response.blob();
        totalSize += blob.size;
      }
    }
  }

  return totalSize;
}

// Clear all caches
async function clearAllCaches() {
  const cacheNames = await caches.keys();
  await Promise.all(cacheNames.map(cacheName => caches.delete(cacheName)));
}