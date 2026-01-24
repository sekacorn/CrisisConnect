# Build stage
FROM node:18-alpine AS build

WORKDIR /app

# Copy package files (cached layer)
COPY package*.json ./

# Install dependencies (including dev dependencies for build)
RUN npm ci && npm cache clean --force

# Copy source code
COPY . .

# Build arguments for configuration
ARG REACT_APP_API_URL=http://localhost:8080/api
ARG NODE_ENV=production

ENV REACT_APP_API_URL=$REACT_APP_API_URL
ENV NODE_ENV=$NODE_ENV

# Build the application
RUN npm run build

# Production stage with nginx
FROM nginx:alpine

# Metadata labels
LABEL maintainer="CrisisConnect Team"
LABEL version="1.0.0"
LABEL description="CrisisConnect Frontend Service"

# Install curl for health checks
RUN apk add --no-cache curl

# Copy built files
COPY --from=build /app/build /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose HTTP port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:80/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
