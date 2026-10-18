<!DOCTYPE html>
<html lang="it">
<head><meta charset="UTF-8"/></head>
<body style="font-family: Helvetica, Arial, sans-serif; font-size: 14px; color: #222;">
<p>Gentile ${saluto},</p>
<p>abbiamo ricevuto una richiesta di reimpostazione della password per l'account <strong>${username}</strong>
  di ${associazione.nome}.</p>
<p>Per scegliere una nuova password apri questo link entro ${ore} ore:</p>
<p><a href="${link}" style="color: #1a5fd0;">Imposta una nuova password</a></p>
<p style="font-size: 12px; color: #555;">Se il pulsante non funziona copia questo indirizzo nel browser:<br/>${link}</p>
<p>Il link si può usare una sola volta. Se non hai chiesto tu il cambio della password ignora questa email:
  la password attuale resta valida.</p>
<p>Cordiali saluti,<br/>${associazione.nome}</p>
</body>
</html>
