FROM navikt/java:11-appdynamics

ENV APPD_ENABLED=TRUE

COPY build/libs/syfooversiktsrv-*-all.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback-remote.xml"
ENV APPLICATION_PROFILE="remote"
