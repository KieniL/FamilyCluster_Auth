FROM luke19/spring-base-image:1630237917

LABEL maintainer="KieniL"
LABEL name="auth"
LABEL version="1.0.0"
LABEL author="KieniL"
LABEL contact="https://github.com/KieniL/FamilyCluster_Auth/issues"
LABEL documentation="https://github.com/KieniL/FamilyCluster_Auth"

ENV DB_AUTH_HOST=tmp
ENV DB_AUTH_DB=tmp
ENV DB_AUTH_USER=tmp
ENV DB_AUTH_PASS=tmp
ENV AUTH_LOG_LEVEL=DEBUG
ENV SECLIST_LOCATION=/seclist

VOLUME $SECLIST_LOCATION

COPY ./target/*.jar /app/app.jar

ENTRYPOINT ["java", "-Djava.io.tmpdir=/app/tmp" ,"-jar", "app.jar"]

EXPOSE 8080
