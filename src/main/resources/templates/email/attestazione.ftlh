<#import "piede.ftlh" as p>
<!DOCTYPE html>
<html lang="it">
<head><meta charset="UTF-8"/></head>
<body style="font-family: Helvetica, Arial, sans-serif; font-size: 14px; color: #222;">
<p>Gentile ${saluto},</p>
<p>in allegato la ricevuta delle spese per i corsi musicali dell'anno ${anno}<#if famiglia> di ${allievo}</#if>,
  da conservare per la dichiarazione dei redditi.</p>
<table style="border-collapse: collapse; margin: 12px 0;">
  <tr><td style="padding: 4px 16px 4px 0; font-weight: bold;">Versamenti tracciabili nel ${anno}</td><td style="padding: 4px 0; text-align: right; font-weight: bold;">${totale}</td></tr>
</table>
<p style="font-size: 12px; color: #555;">Comprende solo i pagamenti con bonifico: quelli in contanti non danno diritto
  alla detrazione. I requisiti per la detrazione (età del ragazzo, reddito, limite di spesa) li verifica chi la
  richiede.</p>
<p>Cordiali saluti,<br/>${associazione.nome}</p>
<@p.areaCorsi/>
</body>
</html>
