<#import "/regole-corso.ftlh" as r>
<#import "/importi.ftlh" as i>
<!DOCTYPE html>
<html lang="it">
<head><meta charset="UTF-8"/></head>
<body style="font-family: Helvetica, Arial, sans-serif; font-size: 14px; color: #222;">
<p>Gentile ${saluto},</p>
<p>confermiamo l'iscrizione di <strong>${allievo.nome}</strong> al corso <strong>${corso.nome}</strong>
  (anno scolastico ${corso.anno}<#if corso.insegnante?has_content>, ${corso.etichettaInsegnante?lower_case} ${corso.insegnante}</#if>)<#if corso.giorni?has_content>, con lezione il ${corso.giorni}</#if>.</p>
<@i.blocco email=true/>
<#if associazione.iban?has_content>
<p>Il pagamento può essere effettuato con bonifico intestato a ${associazione.nome}, IBAN <strong>${associazione.iban}</strong>,
  indicando nella causale &quot;${corso.nome} ${corso.anno} – ${allievo.nome}&quot;.</p>
</#if>
<@r.regole email=true/>
<p>In allegato la conferma di iscrizione in PDF.</p>
<p>Cordiali saluti,<br/>${associazione.nome}</p>
</body>
</html>
