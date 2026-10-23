#mvn verify -Pit,production
#mvn clean package -Pproduction -DskipTests

./mvnw clean package -Pproduction -DskipTests
cp target/gestione-corsi-1.1.0.jar release/

./mvnw clean
