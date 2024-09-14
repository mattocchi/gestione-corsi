#scp release/tiot-manager-1.0-SNAPSHOT.jar aleven@app.apprici.it:~/tiotmanager

#rsync -r --no-whole-file --stats release/tiot-manager-1.0-SNAPSHOT.jar aleven@app.apprici.it:~/tiotmanager
#rsync --progress --no-whole-file --stats release/tiot-manager-1.0-SNAPSHOT.jar aleven@app.apprici.it:~/tiotmanager

#rsync -e 'ssh -p 2022' --progress --no-whole-file --stats release/start.sh cloud@cloud.attocchi.it:~/gestionecorsi
rsync -e 'ssh -p 2022' --progress --no-whole-file --stats release/gestione-corsi-1.0-SNAPSHOT.jar cloud@cloud.attocchi.it:~/gestionecorsi
