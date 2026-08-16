# ---- Build stage ----
FROM node:20-alpine AS build
WORKDIR /build

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Runtime stage ----
FROM nginx:1.27-alpine

# openssl: used once by deploy/init-letsencrypt.sh to mint a throwaway cert so nginx can boot
# before the real Let's Encrypt certificate exists. curl: container healthcheck.
RUN apk add --no-cache curl openssl \
    && rm -f /etc/nginx/conf.d/default.conf

COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx/templates /etc/nginx/templates

EXPOSE 80 443

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=5 \
    CMD curl -fk https://localhost/ || curl -f http://localhost/.well-known/acme-challenge/ || exit 1
