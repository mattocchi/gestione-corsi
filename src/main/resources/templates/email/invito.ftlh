<!DOCTYPE html>
<html lang="it">
<head><meta charset="UTF-8"/></head>
<body style="font-family: Helvetica, Arial, sans-serif; font-size: 14px; color: #222;">
<p>Gentile ${saluto},</p>
<p style="padding: 8px 12px; background: #f2f5fa; border-radius: 6px;">Per entrare puoi usare
<#if email?has_content>indistintamente il nome utente <strong>${username}</strong> oppure la tua email
  <strong>${email}</strong>.<#else>il nome utente <strong>${username}</strong> (la tua email è usata da più account,
  quindi serve il nome utente).</#if></p>
<#if reimpostazione>
<p>il tuo accesso all'Area Corsi di ${associazione.nome} è stato reimpostato: la password precedente non vale più.</p>
<p>Per entrare di nuovo scegli una nuova password da questo link, entro ${giorni} giorni:</p>
<#else>
<p>ti invitiamo ad accedere all'Area Corsi di ${associazione.nome}: potrai consultare documenti, corsi e stato dei
  pagamenti.</p>
<p>Per entrare scegli la tua password da questo link, entro ${giorni} giorni:</p>
</#if>
<p><a href="${link}" style="color: #1a5fd0;">Imposta la password ed entra</a></p>
<p style="font-size: 12px; color: #555;">Se il pulsante non funziona copia questo indirizzo nel browser:<br/>${link}</p>
<p>Il link si può usare una sola volta. <#if reimpostazione>Scaduto, chiedi un nuovo link alla segreteria.<#else>Scaduto,
  puoi usare «Password dimenticata?» nella pagina di accesso.</#if></p>
<p>Cordiali saluti,<br/>${associazione.nome}</p>
</body>
</html>
