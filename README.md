# syfooversiktsrv
This app i a service for Syfooversikt(https://github.com/navikt/syfooversikt)

## Technologies used
* Kotlin
* Ktor
* Gradle
* Spek
* Vault
* Postgres

## Buil and run local

* First start the database:
```console
$ docker-compose up
```

* Run the `main()` function in `SyfooversiktApplication.kt`

### Connect to the db from terminal:

To connect and run queries directly against the db run:
```console
$ docker-compose exec -it db bash

//in the docker bash run the following
$ psql -U username syfooversiktsrv_dev
```

Some sample commands/queries:
```console
// list tables
$ \dt

// sample query
$ select * from person_oversikt_status;
```

#### Build
Run `./gradlew clean shadowJar`
   
#### Local Env variables
Make a copy of `localEnvForTests.json` with the name `localEnv.json` and set the variables. This file ignored by git.
Remember to set the correct path in Environment.kt: `const val localEnvironmentPropertiesPath = "./src/main/resources/localEnv.json"`

#### Creating a docker image
Creating a docker image should be as simple as `docker build -t syfooversiktsrv .`

#### Running a docker image
`docker run --rm -it -p 8080:8080 syfooversiktsrv`

#### Starting a local PostgreSQL server

Run `docker-compose up`.

### Access to the Postgres database

For utfyllende dokumentasjon se [Postgres i NAV](https://github.com/navikt/utvikling/blob/master/PostgreSQL.md)


#### Tldr

The application uses dynamically generated user / passwords for the database.
To connect to the database one must generate user / password (which lasts for one hour)
as follows:

Use The Vault Browser CLI that is build in https://vault.adeo.no


Preprod credentials:

```
vault read postgresql/preprod-fss/creds/syfooversiktsrv-admin

```

Prod credentials:

```
vault read postgresql/prod-fss/creds/syfooversiktsrv-admin

```

The user / password combination can be used to connect to the relevant databases (From developer image ...)
e.g.

```

psql -d $DATABASE_NAME -h $DATABASE_HOST -U $GENERERT_USER_NAME

```
