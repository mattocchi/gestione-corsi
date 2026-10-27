<!DOCTYPE html>
<html lang="it">
<head><meta charset="UTF-8"/></head>
<body style="font-family: Helvetica, Arial, sans-serif; font-size: 14px; color: #222;">
<p>Gentile ${saluto},</p>
<#if reimpostazione>
<p>il tuo accesso all'Area Corsi di ${associazione.nome} è stato reimpostato: la password precedente non vale più.</p>
<p>Il tuo nome utente è <strong>${username}</strong>. Per entrare di nuovo scegli una nuova password da questo link,
  entro ${giorni} giorni:</p>
<#else>
<p>ti invitiamo ad accedere all'Area Corsi di ${associazione.nome}: potrai consultare documenti, corsi e stato dei
  pagamenti.</p>
<p>Il tuo nome utente è <strong>${username}</strong>. Per entrare scegli la tua password da questo link, entro
  ${giorni} giorni:</p>
</#if>
<p><a href="${link}" style="color: #1a5fd0;">Imposta la password ed entra</a></p>
<p style="font-size: 12px; color: #555;">Se il pulsante non funziona copia questo indirizzo nel browser:<br/>${link}</p>
<p>Il link si può usare una sola volta. <#if reimpostazione>Scaduto, chiedi un nuovo link alla segreteria.<#else>Scaduto,
  puoi usare «Password dimenticata?» nella pagina di accesso.</#if></p>
<p>Cordiali saluti,<br/>${associazione.nome}</p>
</body>
</html>
