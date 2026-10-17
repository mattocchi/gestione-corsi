#!/bin/bash
# chmod +x start-test.sh
# 
# pm2 start /home/cloud/gestionecorsi/start-test.sh --name test-gestionecorsi --watch
#
export PORT=8081
JAVA_HOME="/home/cloud/.sdkman/candidates/java/25.0.4-tem/"
$JAVA_HOME/bin/java -jar gestione-corsi-test.jar
