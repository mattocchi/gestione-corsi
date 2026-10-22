<#-- Regole su recuperi, assenze e rinuncia, uguali nell'email e nel PDF di conferma di iscrizione.
     XHTML ben formato (lo usa anche OpenHTMLtoPDF); con email=true gli stili sono in linea per i client di posta. -->
<#macro regole email=false>
<#if email>
<p style="margin: 16px 0 4px;"><strong>Lezioni, assenze e recuperi</strong></p>
<ul style="margin: 0; padding-left: 20px;">
<#else>
<h2>Lezioni, assenze e recuperi</h2>
<ul class="regole">
</#if>
  <li><strong>Lezione annullata dall'insegnante</strong> – Viene sempre recuperata, in un altro giorno o in coda al
    corso: il numero di lezioni previsto è garantito.</li>
  <li><strong>Assenza dell'allievo</strong> – Se avvisate la segreteria dei corsi almeno una settimana prima,
    l'assenza è giustificata e, se possibile, si concorda con l'insegnante un recupero. Con un preavviso più breve
    (o senza avviso) l'assenza non è giustificata: la lezione non viene recuperata ed è comunque dovuta.</li>
  <li><strong>Rinuncia al corso</strong> – Non è previsto il rimborso delle lezioni non frequentate. Eventuali
    eccezioni sono valutate caso per caso, a giudizio insindacabile della segreteria dei corsi.</li>
</ul>
</#macro>
