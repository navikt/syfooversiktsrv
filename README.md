# syfooversiktsrv
Denne appen er en backend-tjenste for Syfooversikt

## Bygge og kjøre appen lokalt
1. Kjør `./gradlew clean shadowJar`
2. Lag en kopi av `localEnvForTests.json` som du gir navnet `localEnv.json` og legg inn riktige verdier. Denne filen ignoreres av git. 
   Husk å oppdatere med riktig filsti i Environment.kt: `const val localEnvironmentPropertiesPath = "./src/main/resources/localEnv.json"`
2. Bygg dockerimage og start appen med `docker build -t app_name .` og
`docker run -p 8080:8080 syfooversiktsrv`
