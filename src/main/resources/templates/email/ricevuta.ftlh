<#import "piede.ftlh" as p>
<#import "/importi.ftlh" as i>
<!DOCTYPE html>
<html lang="it">
<head><meta charset="UTF-8"/></head>
<body style="font-family: Helvetica, Arial, sans-serif; font-size: 14px; color: #222;">
<p>Gentile ${saluto},</p>
<p>in allegato la ricevuta dei pagamenti per il corso <strong>${corso.nome}</strong> (anno scolastico ${corso.anno})
  di <strong>${allievo.nome}</strong>.</p>
<@i.blocco email=true/>
<p>Cordiali saluti,<br/>${associazione.nome}</p>
<@p.areaCorsi/>
</body>
</html>
