// Avvio in produzione con pm2 (vedi README.md in questa cartella).
//
//   pm2 start ecosystem.config.js                  primo avvio
//   pm2 reload ecosystem.config.js --update-env    dopo aver cambiato il JAR o gestione-corsi.env
//
// pm2 lancia direttamente la JVM (interpreter 'none'): segnali di arresto e riavvii arrivano a Java,
// non a una shell intermedia. Le variabili d'ambiente (segreti compresi) stanno in gestione-corsi.env,
// accanto a questo file, fuori da git e con permessi 600: vedi gestione-corsi.env.example.

const fs = require('fs');
const path = require('path');

const ENV_FILE = path.join(__dirname, 'gestione-corsi.env');

/** Legge un file KEY=VALUE: righe vuote e commenti (#) ignorati, virgolette esterne tolte. */
function leggiEnv(file) {
  if (!fs.existsSync(file)) {
    throw new Error(`Manca ${file}: copia gestione-corsi.env.example in gestione-corsi.env e compilalo`);
  }
  const env = {};
  for (const riga of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const m = riga.match(/^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$/);
    if (m) {
      env[m[1]] = m[2].replace(/^(['"])(.*)\1$/, '$2');
    }
  }
  return env;
}

const { APP_JAR, JAVA_BIN, JAVA_OPTS, ...appEnv } = leggiEnv(ENV_FILE);
if (!appEnv.DB_PASSWORD) {
  throw new Error(`DB_PASSWORD mancante in ${ENV_FILE}`);
}

const javaOpts = (JAVA_OPTS || '-Xms256m -Xmx512m -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Duser.timezone=Europe/Rome')
  .split(/\s+/)
  .filter(Boolean);

module.exports = {
  apps: [
    {
      name: 'gestione-corsi',
      cwd: __dirname,
      script: JAVA_BIN || 'java',
      interpreter: 'none',
      // Il profilo prod è fissato qui: senza, Spring userebbe il profilo dev (H2 in memoria e utenti di prova)
      args: [...javaOpts, '-jar', APP_JAR || 'gestione-corsi.jar', '--spring.profiles.active=prod'],
      exec_mode: 'fork',
      instances: 1,
      env: appEnv,

      // Riavvii: Spring Boot impiega qualche secondo ad avviarsi; dopo 10 avvii falliti di fila pm2 si ferma
      autorestart: true,
      min_uptime: '60s',
      max_restarts: 10,
      restart_delay: 10000,
      watch: false,

      // Arresto ordinato: Spring chiude le richieste in corso (fino a 30 s) prima che pm2 forzi l'uscita
      kill_timeout: 35000,

      // Log di Spring (stdout) in ./logs con data e ora; rotazione con il modulo pm2-logrotate
      out_file: path.join(__dirname, 'logs', 'out.log'),
      error_file: path.join(__dirname, 'logs', 'error.log'),
      merge_logs: true,
      time: true,
    },
  ],
};
