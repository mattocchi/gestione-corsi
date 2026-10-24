<#import "comune.ftlh" as c>
<#import "/regole-corso.ftlh" as r>
<#import "/importi.ftlh" as i>
<!DOCTYPE html>
<html lang="it">
<head>
<meta charset="UTF-8"/>
<title>Conferma di iscrizione</title>
<@c.stile/>
</head>
<body>
<@c.intestazione titolo="Conferma di iscrizione"/>
<p>Confermiamo l'iscrizione di <strong>${allievo.nome}</strong> al corso <strong>${corso.nome}</strong>
  per l'anno scolastico ${corso.anno}<#if corso.giorni?has_content>, con lezione il ${corso.giorni}</#if>.</p>
<@c.datiIscrizione insegnanti=false/>
<h2>Importo</h2>
<@i.blocco/>
<div class="riquadro">
  ${modalitaPagamento}
  <#if associazione.iban?has_content>
  <br/>Si può pagare con bonifico intestato a <strong>${associazione.nome}</strong>,
  IBAN <strong>${associazione.iban}</strong>, indicando nella causale
  &quot;${corso.nome} ${corso.anno} – ${allievo.nome}&quot;.
  </#if>
</div>
<@r.regole/>
</body>
</html>
