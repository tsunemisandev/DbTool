#!/bin/bash
set -e

echo "Starting SQL Server..."
/opt/mssql/bin/sqlservr & pid=$!

SQLCMD_BIN="/opt/mssql-tools/bin/sqlcmd"
if [ ! -x "$SQLCMD_BIN" ] && [ -x "/opt/mssql-tools18/bin/sqlcmd" ]; then
  SQLCMD_BIN="/opt/mssql-tools18/bin/sqlcmd"
fi
SQLCMD_OPTS="-C" # trust server certificate (ODBC Driver 18+ requires this)

echo "Waiting for SQL Server to accept connections..."
for i in {1..30}; do
  if "$SQLCMD_BIN" $SQLCMD_OPTS -S localhost -d master -U sa -P "${SA_PASSWORD}" -Q "SELECT 1" >/dev/null 2>&1; then
    echo "SQL Server is up."
    break
  fi
  sleep 2
done

echo "Running init script..."
"$SQLCMD_BIN" $SQLCMD_OPTS -S localhost -d master -U sa -P "${SA_PASSWORD}" -i /docker-entrypoint-initdb.d/init.sql

wait $pid
