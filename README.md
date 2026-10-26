# transitlog-cancellation-sink [![CI/CD](https://github.com/HSLdevcom/transitlog-cancellation-sink/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/HSLdevcom/transitlog-cancellation-sink/actions/workflows/ci-cd.yml)

## Description

Application for inserting trip cancellations to PostgreSQL.


## Setup:

Setup the PostgreSQL database using the [db-init-script](schema/init.sql):

```   
psql -h localhost -d template1 -U postgres -f ./schema/init.sql
```   

## Building

### Dependencies

This project depends on [transitdata-common](https://github.com/HSLdevcom/transitdata-common) project.

Either use released versions from public maven repository or build your own and install to local maven repository:
  - ```cd transitdata-common && ./mvnw install```

### Locally

- Requires Java 25.
- Set `GITHUB_ACTOR` and `GITHUB_TOKEN` for GitHub Packages access.
- ```./mvnw -s .mvn/settings.xml compile```
- ```./mvnw -s .mvn/settings.xml package```

### Docker image

- ```DOCKER_BUILDKIT=1 docker build --secret id=github_token,env=GITHUB_TOKEN -t transitlog-cancellation-sink .```


## Running

Requirements:
- Pulsar Cluster
  - By default uses localhost, override host in PULSAR_HOST if needed.
    - Tip: f.ex if running inside Docker in OSX set `PULSAR_HOST=host.docker.internal` to connect to the parent machine
  - You can use [this script](https://github.com/HSLdevcom/transitdata/blob/master/bin/pulsar/pulsar-up.sh) to launch it as Docker container
- Connection string to Transitlog database is read from file.
  - Set filepath via env variable FILEPATH_CONNECTION_STRING, default is `/run/secrets/db_conn_string`
  - Format of connection string is `jdbc:postgresql://localhost:5432/vehicles?user=hfp_writer&password=hfp_writer`

All other configuration options are configured in the [config file](src/main/resources/environment.conf)
which can also be configured externally via env variable CONFIG_PATH

Launch Docker container with

```docker-compose -f compose-config-file.yml up <service-name>```   
