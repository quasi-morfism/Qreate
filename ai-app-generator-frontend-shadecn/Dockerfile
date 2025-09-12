# Build stage
FROM node:20-alpine as build-stage

WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

COPY . .
RUN npm run build

# Production stage
FROM nginx:alpine as production-stage

# Copy custom nginx configuration template
COPY docker-nginx.conf /etc/nginx/conf.d/default.conf.template

# Copy built application
COPY --from=build-stage /app/dist /usr/share/nginx/html

# Create directory for deployed apps
RUN mkdir -p /usr/share/nginx/deployed-apps

# Create entrypoint script to substitute environment variables
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# Set default environment variables
ENV API_BASE_URL=http://localhost:8100/api

EXPOSE 80

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]