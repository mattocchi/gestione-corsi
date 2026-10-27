package it.attocchi.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import it.attocchi.entity.Corso;

/** Tipo del corso nelle view: badge "Servizio" (i corsi non ne hanno) e nome del corso con il badge accanto. */
public final class TipoCorsoUI {

    private TipoCorsoUI() {
    }

    /** Badge "Servizio", o {@code null} per un corso. */
    public static Badge badge(Corso corso) {
        return corso.isServizio() ? new Badge("Servizio", VaadinIcon.TOOLS.create()) : null;
    }

    /** Badge "Non pubblicato" (corso in preparazione, non visibile a genitori e allievi), o {@code null}. */
    public static Badge badgePubblicazione(Corso corso) {
        if (corso.isPubblicato()) {
            return null;
        }
        Badge badge = new Badge("Non pubblicato", VaadinIcon.EYE_SLASH.create());
        badge.addThemeVariants(BadgeVariant.CONTRAST);
        return badge;
    }

    /** Nome del corso seguito dai badge "Servizio" e "Non pubblicato" quando servono. */
    public static Component nome(Corso corso) {
        Span nome = new Span(corso.getNome());
        Badge servizio = badge(corso);
        Badge pubblicazione = badgePubblicazione(corso);
        if (servizio == null && pubblicazione == null) {
            return nome;
        }
        Span riga = new Span(nome);
        if (servizio != null) {
            riga.add(servizio);
        }
        if (pubblicazione != null) {
            riga.add(pubblicazione);
        }
        riga.addClassNames("inline-flex flex-wrap items-center gap-2");
        return riga;
    }
}
