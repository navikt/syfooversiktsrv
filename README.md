# syfooversiktsrv
This application is a service dedicated to supply Syfooversikt(https://github.com/navikt/syfooversikt) with a list of
relevant persons and information on them.

## Technologies used
* Kotlin
* Ktor
* Gradle
* JUnit
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

### Lint (Ktlint)
##### Command line
Run checking: `./gradlew --continue ktlintCheck`

Run formatting: `./gradlew ktlintFormat`
##### Git Hooks
Apply checking: `./gradlew addKtlintCheckGitPreCommitHook`

Apply formatting: `./gradlew addKtlintFormatGitPreCommitHook`

#### Creating a docker image
Creating a docker image should be as simple as `docker build -t syfooversiktsrv .`

#### Running a docker image
`docker run --rm -it -p 8080:8080 syfooversiktsrv`

#### Starting a local PostgreSQL server

Run `docker-compose up`.
