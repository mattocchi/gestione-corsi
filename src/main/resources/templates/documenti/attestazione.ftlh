<#import "comune.ftlh" as c>
<!DOCTYPE html>
<html lang="it">
<head>
<meta charset="UTF-8"/>
<title>Attestazione delle spese ${anno}</title>
<@c.stile/>
<style>
  table.elenco { table-layout: fixed; margin-top: 2mm; }
  table.elenco th.data, table.elenco td.data { width: 24mm; }
  table.elenco th.metodo, table.elenco td.metodo { width: 24mm; }
  table.elenco th.importo, table.elenco td.importo { width: 28mm; }
  p.dichiarazione { margin: 4mm 0 2mm; line-height: 1.4; }
</style>
</head>
<body>
<@c.intestazione titolo="Attestazione delle spese " + anno/>
<table class="colonne">
  <tr>
    <td class="colonna">
      <h2>Allievo</h2>
      <table class="dati">
        <tr><td class="etichetta">Nome</td><td>${allievo.nome}</td></tr>
        <#if allievo.nascita?has_content><tr><td class="etichetta">Nato il</td><td>${allievo.nascita}</td></tr></#if>
        <#if allievo.codiceFiscale?has_content><tr><td class="etichetta">Codice fiscale</td><td>${allievo.codiceFiscale}</td></tr></#if>
      </table>
    </td>
    <#if genitore??>
    <td class="colonna">
      <h2>Spese sostenute da</h2>
      <table class="dati">
        <tr><td class="etichetta">Genitore</td><td>${genitore.nome}</td></tr>
        <#if genitore.codiceFiscale?has_content><tr><td class="etichetta">Codice fiscale</td><td>${genitore.codiceFiscale}</td></tr></#if>
      </table>
    </td>
    </#if>
  </tr>
</table>
<p class="dichiarazione">${associazione.nome} attesta che nell'anno ${anno} sono stati pagati per l'allievo
  i seguenti importi per la frequenza dei corsi<#if genitore??>, sostenuti da ${genitore.nome}</#if>.</p>
<#if pagamenti?has_content>
<table class="elenco">
  <tr><th class="data">Data</th><th>Corso</th><th class="metodo">Metodo</th><th class="importo">Importo</th></tr>
  <#list pagamenti as pagamento>
  <tr><td class="data">${pagamento.data}</td><td>${pagamento.corso}</td><td class="metodo">${pagamento.metodo}</td><td class="importo">${pagamento.importo}</td></tr>
  </#list>
</table>
<#else>
<p>Nessun pagamento registrato nell'anno ${anno}.</p>
</#if>
<div class="riquadro">
<table class="totali">
  <tr><td>Totale pagato nell'anno ${anno}</td><td class="valore">${totale}</td></tr>
</table>
</div>
</body>
</html>
