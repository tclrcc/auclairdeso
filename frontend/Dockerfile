# syntax=docker/dockerfile:1

# Build: installs the exact dependencies of package-lock.json and builds the application.
FROM node:24-alpine AS build
WORKDIR /workspace
ENV NG_CLI_ANALYTICS=false
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
RUN npx ng build

# Run: the server-side rendering server, alone, as the unprivileged "node" user.
FROM node:24-alpine
WORKDIR /app
ENV NODE_ENV=production PORT=4000
COPY --from=build --chown=node:node /workspace/dist/frontend ./dist/frontend
USER node
EXPOSE 4000
CMD ["node", "dist/frontend/server/server.mjs"]
