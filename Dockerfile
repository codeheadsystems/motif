FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY server/build/distributions/server.tar /tmp/server.tar
RUN tar xf /tmp/server.tar -C /app --strip-components=1 && rm /tmp/server.tar

COPY docker/config.yml /app/config.yml
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

EXPOSE 8080 8081

ENTRYPOINT ["/app/entrypoint.sh"]
