#mvn verify -Pit,production
#mvn clean package -Pproduction -DskipTests

mvn clean package -Pproduction -DskipTests
cp target/gestione-corsi-1.0-SNAPSHOT.jar release/

mvn clean
