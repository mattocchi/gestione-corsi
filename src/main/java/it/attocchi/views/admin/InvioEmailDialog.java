package it.attocchi.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.VaadinService;
import it.attocchi.entity.Allievo;
import it.attocchi.entity.Corso;
import it.attocchi.entity.Genitore;
import it.attocchi.entity.InvioEmail;
import it.attocchi.entity.TipoDocumento;
import it.attocchi.service.DocumentiService;
import it.attocchi.service.EmailService;
import it.attocchi.service.DocumentiService.Pdf;
import it.attocchi.service.EmailService.Anteprima;
import it.attocchi.service.EmailService.Destinatario;
import it.attocchi.service.EmailService.DestinatarioConferma;
import it.attocchi.service.EmailService.Riepilogo;
import it.attocchi.views.MenuPdf;
import it.attocchi.views.Stili;
import it.attocchi.views.ViewSupport;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

/**
 * Invio per email di un documento (admin e segreteria): conferma di iscrizione o ricevuta di un partecipante,
 * riepilogo annuale di una famiglia o di un allievo adulto; con il destinatario in evidenza e l'anteprima dell'email.
 * E conferma dell'invio multiplo delle conferme di un corso.
 */
public final class InvioEmailDialog {

    private InvioEmailDialog() {
    }

    /** Invio della conferma o della ricevuta di un'iscrizione; {@code dopoInvio} è eseguito dopo l'invio. */
    public static void apri(TipoDocumento tipo, Corso corso, Allievo allievo, Runnable dopoInvio) {
        EmailService emailService = emailService();
        String username = username();
        String impedimento = DocumentiService.avvisoDatiMancanti(allievo);
        if (impedimento == null && tipo == TipoDocumento.RICEVUTA && !emailService.haPagamenti(corso.getId(), allievo.getId())) {
            impedimento = "Nessun pagamento registrato: non c'è una ricevuta da inviare";
        }
        apriInvio(emailService, tipo, tipo.getTitolo() + " – " + allievo.getNomeCompleto(),
            corso.getNome() + (corso.getAnnoScolastico() != null ? " " + corso.getAnnoScolastico() : ""),
            emailService.destinatario(corso.getId(), allievo.getId()), impedimento,
            () -> emailService.anteprima(tipo, corso.getId(), allievo.getId(), username),
            () -> emailService.invia(tipo, corso.getId(), allievo.getId(), username), dopoInvio);
    }

    /**
     * Invio del riepilogo annuale della famiglia ({@code genitore}) o dell'allievo adulto ({@code allievo});
     * {@code anno} {@code null} = corsi senza anno scolastico.
     */
    public static void apriRiepilogo(Genitore genitore, Allievo allievo, String anno, Runnable dopoInvio) {
        EmailService emailService = emailService();
        String username = username();
        Long genitoreId = genitore != null ? genitore.getId() : null;
        Long allievoId = genitore == null ? allievo.getId() : null;
        String intestatario = genitore != null ? genitore.getNomeCompleto() : allievo.getNomeCompleto();
        apriInvio(emailService, TipoDocumento.RIEPILOGO, TipoDocumento.RIEPILOGO.getTitolo() + " – " + intestatario,
            anno != null ? "Anno scolastico " + anno : "Corsi senza anno scolastico",
            emailService.destinatarioRiepilogo(genitoreId, allievoId, anno), null,
            () -> emailService.anteprimaRiepilogo(genitoreId, allievoId, anno, username),
            () -> emailService.inviaRiepilogo(genitoreId, allievoId, anno, username), dopoInvio);
    }

    /** Dialog comune: documento, destinatario o motivo per cui non si può inviare, anteprima e invio. */
    private static void apriInvio(EmailService emailService, TipoDocumento tipo, String titolo, String descrizione,
                                  Optional<Destinatario> destinatario, String impedimento,
                                  SerializableSupplier<Anteprima> anteprima, SerializableSupplier<InvioEmail> invio,
                                  Runnable dopoInvio) {
        Dialog dialog = ViewSupport.dialog("Invia: " + titolo, "520px");
        VerticalLayout content = new VerticalLayout();
        content.addClassNames("p-0");
        content.add(new Span(descrizione));
        Span a = new Span(destinatario.map(d -> "A: " + d.nome() + " <" + d.email() + ">")
            .orElse("Nessun indirizzo email: aggiungilo nella scheda del genitore o dell'allievo per poter inviare."));
        a.addClassNames(Stili.TESTO_SECONDARIO);
        content.add(a);
        if (impedimento != null) {
            Span avviso = new Span(impedimento + ".");
            avviso.addClassNames(Stili.TESTO_SECONDARIO);
            content.add(avviso);
        }
        if (!emailService.invioReale()) {
            Span simulato = new Span("Server email non configurato: l'invio sarà solo simulato e registrato.");
            simulato.addClassNames(Stili.TESTO_SECONDARIO);
            content.add(simulato);
        }
        boolean inviabile = destinatario.isPresent() && impedimento == null;
        Button vedi = new Button("Anteprima", VaadinIcon.EYE.create(), e -> mostraAnteprima(tipo, anteprima));
        vedi.setAriaLabel("Anteprima: " + tipo.getTitolo());
        vedi.setEnabled(inviabile);
        Button invia = new Button("Invia " + tipo.getBreve().toLowerCase(), VaadinIcon.ENVELOPE.create(),
            e -> esegui(tipo, invio, dialog, dopoInvio));
        invia.addThemeVariants(ButtonVariant.PRIMARY);
        invia.setEnabled(inviabile);
        dialog.add(content);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), vedi, invia);
        dialog.open();
    }

    private static void esegui(TipoDocumento tipo, SerializableSupplier<InvioEmail> invio, Dialog dialog, Runnable dopoInvio) {
        try {
            InvioEmail esito = invio.get();
            switch (esito.getEsito()) {
                case INVIATA -> Notification.show(tipo.getTitolo() + " inviata a " + esito.getDestinatario());
                case SIMULATA -> Notification.show(tipo.getTitolo() + ": invio simulato a " + esito.getDestinatario());
                case ERRORE -> Notification.show("Invio non riuscito: " + esito.getErrore());
            }
            dialog.close();
        } catch (RuntimeException ex) {
            Notification.show("Invio non possibile: " + ex.getMessage());
        }
        dopoInvio.run();
    }

    /** Apre il dialog di invio per l'iscrizione; {@code dopoInvio} è eseguito dopo ogni invio. */
    public static void apri(Corso corso, Allievo allievo, Runnable dopoInvio) {
        EmailService emailService = emailService();
        String username = username();
        Dialog dialog = ViewSupport.dialog("Email: conferma o ricevuta – " + allievo.getNomeCompleto(), "520px");
        VerticalLayout content = new VerticalLayout();
        content.addClassNames("p-0");
        content.add(new Span(corso.getNome() + (corso.getAnnoScolastico() != null ? " " + corso.getAnnoScolastico() : "")));

        Optional<Destinatario> destinatario = emailService.destinatario(corso.getId(), allievo.getId());
        Span a = new Span(destinatario.map(d -> "A: " + d.nome() + " <" + d.email() + ">")
            .orElse("Né il genitore né l'allievo hanno un indirizzo email: aggiungilo nella loro scheda per poter inviare."));
        a.addClassNames(Stili.TESTO_SECONDARIO);
        content.add(a);
        String datiMancanti = DocumentiService.avvisoDatiMancanti(allievo);
        if (datiMancanti != null) {
            Span avviso = new Span(datiMancanti + ".");
            avviso.addClassNames(Stili.TESTO_SECONDARIO);
            content.add(avviso);
        }
        if (!emailService.invioReale()) {
            Span simulato = new Span("Server email non configurato: l'invio sarà solo simulato e registrato.");
            simulato.addClassNames(Stili.TESTO_SECONDARIO);
            content.add(simulato);
        }

        Button conferma = new Button("Invia conferma di iscrizione", VaadinIcon.ENVELOPE.create(),
            e -> invia(emailService, TipoDocumento.CONFERMA_ISCRIZIONE, corso, allievo, username, dialog, dopoInvio));
        conferma.addThemeVariants(ButtonVariant.PRIMARY);
        Button ricevuta = new Button("Invia ricevuta", VaadinIcon.ENVELOPE.create(),
            e -> invia(emailService, TipoDocumento.RICEVUTA, corso, allievo, username, dialog, dopoInvio));
        boolean conPagamenti = emailService.haPagamenti(corso.getId(), allievo.getId());
        conferma.setEnabled(destinatario.isPresent() && datiMancanti == null);
        ricevuta.setEnabled(destinatario.isPresent() && conPagamenti && datiMancanti == null);
        if (!conPagamenti) {
            ricevuta.setTooltipText("Nessun pagamento registrato");
        }
        // Accanto a ogni invio la sua anteprima, con gli stessi controlli
        Button anteprimaConferma = pulsanteAnteprima(emailService, TipoDocumento.CONFERMA_ISCRIZIONE, corso, allievo.getId(), username);
        anteprimaConferma.setEnabled(conferma.isEnabled());
        Button anteprimaRicevuta = pulsanteAnteprima(emailService, TipoDocumento.RICEVUTA, corso, allievo.getId(), username);
        anteprimaRicevuta.setEnabled(ricevuta.isEnabled());
        content.add(riga(conferma, anteprimaConferma), riga(ricevuta, anteprimaRicevuta));
        dialog.add(content);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Chiudi"));
        dialog.open();
    }

    /**
     * Mostra a chi andrà la conferma di iscrizione (e chi è saltato), con l'anteprima sul primo che la riceve, e la
     * invia a tutti dopo la conferma.
     */
    public static void inviaConferme(Corso corso, Runnable dopoInvio) {
        EmailService emailService = emailService();
        String username = username();
        List<DestinatarioConferma> destinatari = emailService.destinatariConferme(corso.getId(), username);
        List<DestinatarioConferma> riceventi = destinatari.stream().filter(DestinatarioConferma::riceve).toList();
        Dialog dialog = ViewSupport.dialog("Conferme di iscrizione – " + corso.getNome(), "560px");
        Span intro = new Span("La conferma di iscrizione sarà inviata per email a " + riceventi.size() + " iscritti su "
            + destinatari.size() + " (al genitore o, se manca, all'allievo)."
            + (emailService.invioReale() ? "" : " Server email non configurato: l'invio sarà solo simulato."));
        Div elenco = new Div();
        elenco.addClassNames("flex flex-col gap-1");
        for (DestinatarioConferma destinatario : destinatari) {
            Span nome = new Span(destinatario.allievo());
            nome.addClassNames("font-(--aura-font-weight-medium)");
            Span dettaglio = new Span(destinatario.riceve()
                ? "→ " + destinatario.destinatario().nome() + " <" + destinatario.destinatario().email() + ">"
                : "saltato: " + destinatario.motivoEsclusione());
            dettaglio.addClassNames(Stili.TESTO_SECONDARIO);
            Div voce = new Div(nome, dettaglio);
            voce.addClassNames("flex flex-wrap gap-x-2");
            elenco.add(voce);
        }
        VerticalLayout content = new VerticalLayout(intro, elenco);
        content.addClassNames("p-0");
        dialog.add(content);

        Button anteprima = riceventi.isEmpty() ? new Button("Anteprima", VaadinIcon.EYE.create())
            : pulsanteAnteprima(emailService, TipoDocumento.CONFERMA_ISCRIZIONE, corso, riceventi.getFirst().allievoId(), username);
        anteprima.setEnabled(!riceventi.isEmpty());
        if (!riceventi.isEmpty()) {
            anteprima.setTooltipText("Anteprima della conferma per " + riceventi.getFirst().allievo());
        }
        Button invia = new Button("Invia " + riceventi.size() + " conferme di iscrizione", VaadinIcon.ENVELOPE.create(), e -> {
            try {
                Riepilogo r = emailService.inviaConferme(corso.getId(), username);
                dialog.close();
                Notification.show("Conferme: " + r.inviate() + " inviate, " + r.simulate() + " simulate, " + r.errori()
                    + " con errore, " + r.senzaEmail() + " senza email"
                    + (r.datiMancanti() > 0 ? ", " + r.datiMancanti() + " senza codice fiscale o data di nascita" : ""));
                dopoInvio.run();
            } catch (RuntimeException ex) {
                Notification.show("Invio non riuscito: " + ex.getMessage());
            }
        });
        invia.addThemeVariants(ButtonVariant.PRIMARY);
        invia.setEnabled(!riceventi.isEmpty());
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Annulla"), anteprima, invia);
        dialog.open();
    }

    /** Pulsante di invio e la sua anteprima sulla stessa riga (vanno a capo se lo spazio non basta). */
    private static Div riga(Button invia, Button anteprima) {
        invia.addClassNames("grow");
        Div riga = new Div(invia, anteprima);
        riga.addClassNames("flex flex-wrap gap-2 w-full");
        return riga;
    }

    private static Button pulsanteAnteprima(EmailService emailService, TipoDocumento tipo, Corso corso, Long allievoId,
                                            String username) {
        Button anteprima = new Button("Anteprima", VaadinIcon.EYE.create(),
            e -> mostraAnteprima(emailService, tipo, corso, allievoId, username));
        anteprima.setAriaLabel("Anteprima: " + tipo.getTitolo());
        return anteprima;
    }

    /** Anteprima dell'email com'è inviata (destinatario, oggetto, testo e allegato), senza inviarla. */
    static void mostraAnteprima(EmailService emailService, TipoDocumento tipo, Corso corso, Long allievoId, String username) {
        mostraAnteprima(tipo, () -> emailService.anteprima(tipo, corso.getId(), allievoId, username));
    }

    private static void mostraAnteprima(TipoDocumento tipo, SerializableSupplier<Anteprima> costruisci) {
        Anteprima anteprima;
        try {
            anteprima = costruisci.get();
        } catch (RuntimeException ex) {
            Notification.show("Anteprima non disponibile: " + ex.getMessage());
            return;
        }
        Dialog dialog = ViewSupport.dialog("Anteprima – " + tipo.getTitolo(), "760px");
        Span nota = new Span("Anteprima: l'email non viene inviata.");
        nota.addClassNames(Stili.TESTO_SECONDARIO);
        Span a = new Span("A: " + anteprima.destinatario().nome() + " <" + anteprima.destinatario().email() + ">");
        Span oggetto = new Span("Oggetto: " + anteprima.oggetto());
        byte[] pdf = anteprima.pdf();
        MenuBar allegato = MenuPdf.conTesto("Allegato: " + anteprima.allegato(), VaadinIcon.FILE_TEXT,
            () -> new Pdf(anteprima.allegato(), pdf));
        // Il testo dell'email in un riquadro isolato dalla pagina (nessuno script)
        IFrame corpo = new IFrame();
        corpo.setSrcdoc(anteprima.corpoHtml());
        corpo.setSandbox();
        corpo.setTitle("Testo dell'email");
        corpo.addClassNames("w-full h-[60vh] border border-(--vaadin-border-color-secondary) rounded-(--vaadin-radius-m)");
        VerticalLayout content = new VerticalLayout(nota, a, oggetto, allegato, corpo);
        content.addClassNames("p-0 gap-2");
        dialog.add(content);
        dialog.getFooter().add(ViewSupport.closeButton(dialog, "Chiudi"));
        dialog.open();
    }

    private static void invia(EmailService emailService, TipoDocumento tipo, Corso corso, Allievo allievo, String username,
                              Dialog dialog, Runnable dopoInvio) {
        try {
            InvioEmail invio = emailService.invia(tipo, corso.getId(), allievo.getId(), username);
            switch (invio.getEsito()) {
                case INVIATA -> Notification.show(tipo.getTitolo() + " inviata a " + invio.getDestinatario());
                case SIMULATA -> Notification.show(tipo.getTitolo() + ": invio simulato a " + invio.getDestinatario());
                case ERRORE -> Notification.show("Invio non riuscito: " + invio.getErrore());
            }
            dialog.close();
        } catch (RuntimeException ex) {
            Notification.show("Invio non possibile: " + ex.getMessage());
        }
        dopoInvio.run();
    }

    private static EmailService emailService() {
        // Servizio Spring tramite l'Instantiator di Vaadin: non va passato a ogni vista e dialog
        return VaadinService.getCurrent().getInstantiator().getOrCreate(EmailService.class);
    }

    private static String username() {
        Authentication autenticazione = SecurityContextHolder.getContext().getAuthentication();
        return autenticazione != null ? autenticazione.getName() : "";
    }
}
