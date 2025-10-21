# gestione-corsi

```bash
    bash build.sh
    bash deploy.sh
```

## SERVER

### PM2
https://medium.com/@sharathholmes143/efficiently-manage-your-applications-with-pm2-in-background-790a29fff5e3

    pm2 start start.sh --name “gestione-corsi”
    pm2 startup
    pm2 list

## DB

    CREATE DATABASE gestione_corsi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    