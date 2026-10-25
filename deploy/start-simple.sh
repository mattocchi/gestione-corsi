#!/bin/bash
# chmod +x start.sh
# 
# pm2 start start-test.sh --name test-gestionecorsi --watch gestione-corsi-test.jar
#
JAVA_HOME="/home/cloud/.sdkman/candidates/java/25.0.4-tem/"
$JAVA_HOME/bin/java -Xms256m -Xmx512m -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Duser.timezone=Europe/Rome -jar gestione-corsi.jar --spring.profiles.active=prod
