# transitlog-cancellation-sink [![Test and create Docker image](https://github.com/HSLdevcom/transitlog-cancellation-sink/actions/workflows/test-and-build.yml/badge.svg)](https://github.com/HSLdevcom/transitlog-cancellation-sink/actions/workflows/test-and-build.yml)

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
  - ```cd transitdata-common && mvn install```  

### Locally

- ```mvn compile```  
- ```mvn package```  

### Docker image

- Run [this script](build-image.sh) to build the Docker image


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
