FROM openjdk:17-jdk

EXPOSE 30303

COPY  bela /opt/bela/

ENTRYPOINT ["/opt/bela/bin/bela"]

# Build-time metadata as defined at http://label-schema.org
ARG BUILD_DATE
ARG VCS_REF
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="Bela" \
      org.label-schema.description="Besu Lanterna" \
      org.label-schema.url="https://github.com/ConsenSys/bela/" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/ConsenSys/bela" \
      org.label-schema.vendor="Besu Contributors" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"
