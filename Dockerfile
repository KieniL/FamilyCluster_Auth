FROM luke19/spring-base-image:1630235280

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
ENV SECLIST_LOCATION=/app/seclist

COPY ./target/*.jar /app/app.jar

RUN git clone --depth 1 https://github.com/danielmiessler/SecLists.git $SECLIST_LOCATION

ENTRYPOINT ["java", "-Djava.io.tmpdir=/app/tmp" ,"-jar", "app.jar"]

EXPOSE 8080
