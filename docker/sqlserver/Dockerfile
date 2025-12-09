FROM mcr.microsoft.com/mssql/server:2022-latest

USER root

ENV ACCEPT_EULA=Y
ENV SA_PASSWORD=YourStrong!Passw0rd
ENV MSSQL_PID=Developer

COPY docker-entrypoint.sh /docker-entrypoint.sh
COPY init.sql /docker-entrypoint-initdb.d/init.sql

# Ensure the entrypoint script is executable inside the image
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/bin/bash", "/docker-entrypoint.sh"]
