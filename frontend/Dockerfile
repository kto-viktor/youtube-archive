FROM node:lts-alpine as build-stage
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:stable-alpine as production-stage
COPY nginx/conf/nginx.conf /etc/nginx/nginx.conf
COPY nginx/conf/nginx.conf.template /etc/nginx/conf.d/nginx.conf.template
COPY nginx/cert/ /etc/nginx/ssl/
RUN rm -rf /etc/nginx/conf.d/default.conf
COPY --from=build-stage /app/dist /usr/share/nginx/html
CMD ["nginx", "-g", "daemon off;"]