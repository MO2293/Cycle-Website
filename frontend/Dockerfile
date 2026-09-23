# ---- Stage 1: build the static bundle ----
FROM node:22-alpine AS build
WORKDIR /build

# package-lock.json (not just package.json) so the build installs the exact
# versions that were tested, rather than whatever is newest today.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Stage 2: serve it ----
# The output is plain static files. Nginx serves them; Node is not needed at
# runtime, which is why the final image is ~50MB rather than ~400MB.
FROM nginx:alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
