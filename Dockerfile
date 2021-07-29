FROM luke19/spring-base-image:1627557346

ENV DB_AUTH_HOST=tmp
ENV DB_AUTH_DB=tmp
ENV DB_AUTH_USER=tmp
ENV DB_AUTH_PASS=tmp
ENV AUTH_LOG_LEVEL=DEBUG


COPY ./target/*.jar /APP/app.jar


ENTRYPOINT ["java" ,"-jar", "app.jar"]

EXPOSE 8080
