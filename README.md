# syfooversiktsrv
This application is a service dedicated to supply Syfooversikt(https://github.com/navikt/syfooversikt) with a list of
relevant persons and information on them.

## Technologies used
* Kotlin
* Ktor
* Gradle
* Spek
* Postgres

## Buil and run local

* First start the database:
```console
$ docker-compose up
```

* Run the `main()` function in `SyfooversiktApplication.kt`

## Download packages from Github Package Registry

Certain packages (isdialogmote-schema) must be downloaded from Github Package Registry, which requires
authentication. The packages can be downloaded via build.gradle:

```
val githubUser: String by project
val githubPassword: String by project
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/navikt/isdialogmote-schema")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}
```

`githubUser` and `githubPassword` are properties that are set in `~/.gradle/gradle.properties`:

```
githubUser=x-access-token
githubPassword=<token>
```

Where `<token>` is a personal access token with scope `read:packages`(and SSO enabled).

The variables can alternatively be configured as environment variables or used in the command lines:

* `ORG_GRADLE_PROJECT_githubUser`
* `ORG_GRADLE_PROJECT_githubPassword`

```
./gradlew -PgithubUser=x-access-token -PgithubPassword=[token]
```

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
