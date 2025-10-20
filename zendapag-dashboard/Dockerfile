# Multi-stage build for Zendapag Dashboard
FROM node:18-alpine as builder

WORKDIR /app

# Copy package files
COPY zendapag-dashboard/package*.json ./

# Install dependencies
RUN npm ci --legacy-peer-deps

# Copy source code
COPY zendapag-dashboard/ ./

# Build the application
RUN npm run build

# Production stage with Nginx
FROM nginx:1.25-alpine

WORKDIR /usr/share/nginx/html

# Remove default nginx static files
RUN rm -rf ./*

# Copy built files from builder
COPY --from=builder /app/build .

# Copy nginx configuration
COPY zendapag-dashboard/nginx.conf /etc/nginx/conf.d/default.conf 2>/dev/null || \
  echo 'server { \
    listen 80; \
    server_name _; \
    root /usr/share/nginx/html; \
    index index.html; \
    location / { \
      try_files $uri $uri/ /index.html; \
    } \
    location /api { \
      proxy_pass http://zendapag-api:8080; \
      proxy_http_version 1.1; \
      proxy_set_header Upgrade $http_upgrade; \
      proxy_set_header Connection "upgrade"; \
      proxy_set_header Host $host; \
      proxy_set_header X-Real-IP $remote_addr; \
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; \
      proxy_set_header X-Forwarded-Proto $scheme; \
    } \
  }' > /etc/nginx/conf.d/default.conf

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:80/ || exit 1

# Expose port
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
