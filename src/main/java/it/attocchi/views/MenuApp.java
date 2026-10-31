package it.attocchi.views;

import com.vaadin.flow.server.menu.MenuEntry;
import it.attocchi.views.admin.AllieviView;
import it.attocchi.views.admin.AnniScolasticiView;
import it.attocchi.views.admin.CorsiView;
import it.attocchi.views.admin.EmailInviateView;
import it.attocchi.views.admin.FestivitaView;
import it.attocchi.views.admin.GenitoriView;
import it.attocchi.views.admin.GestionePagamentiView;
import it.attocchi.views.admin.InsegnantiView;
import it.attocchi.views.admin.LezioniView;
import it.attocchi.views.admin.LuoghiView;
import it.attocchi.views.admin.RiepilogoIscrittiView;
import it.attocchi.views.admin.RimborsiView;
import it.attocchi.views.admin.UtentiView;
import it.attocchi.views.allievo.MieiCorsiView;
import it.attocchi.views.allievo.MieiPagamentiView;
import it.attocchi.views.genitore.FigliView;
import it.attocchi.views.genitore.PagamentiFigliView;
import it.attocchi.views.insegnante.LezioniInsegnanteView;
import it.attocchi.views.insegnante.MieiAllieviView;
import it.attocchi.views.insegnante.MieiRimborsiView;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Organizzazione del menu laterale per ruolo: ogni voce ({@code @Menu}, già filtrata per accesso) va in un gruppo.
 * Lo staff ha i gruppi con titolo (Corsi e lezioni, Persone, Economia, Documenti, Impostazioni richiudibile); chi ha
 * un solo ruolo tra insegnante, genitore e allievo ha una lista semplice; con più ruoli ogni ruolo ha il suo gruppo
 * con titolo. In alto Home e Calendario, in fondo profilo e guida.
 */
public final class MenuApp {

    /** Gruppo del menu, nell'ordine di visualizzazione; {@code titolo} null = senza titolo. */
    public enum Gruppo {
        GENERALE(null, false),
        CORSI("Corsi e lezioni", false),
        PERSONE("Persone", false),
        ECONOMIA("Economia", false),
        DOCUMENTI("Documenti", false),
        IMPOSTAZIONI("Impostazioni", true),
        INSEGNANTE("Insegnante", false),
        FAMIGLIA("Famiglia", false),
        ALLIEVO("Allievo", false),
        ACCOUNT(null, false);

        private final String titolo;
        private final boolean richiudibile;

        Gruppo(String titolo, boolean richiudibile) {
            this.titolo = titolo;
            this.richiudibile = richiudibile;
        }

        public String getTitolo() {
            return titolo;
        }

        /** Richiudibile e chiuso all'inizio (pagine usate di rado). */
        public boolean isRichiudibile() {
            return richiudibile;
        }
    }

    /** Un gruppo con le sue voci; {@code titolo} null = lista senza titolo. */
    public record Sezione(Gruppo gruppo, String titolo, List<MenuEntry> voci) {
    }

    /** Area di lavoro dell'utente: lo staff (admin o segreteria) o uno dei ruoli personali. */
    private enum Area {
        STAFF, INSEGNANTE, FAMIGLIA, ALLIEVO
    }

    private static final Map<Class<?>, Gruppo> GRUPPI = Map.ofEntries(
        Map.entry(HomeView.class, Gruppo.GENERALE),
        Map.entry(CalendarioView.class, Gruppo.GENERALE),
        Map.entry(CorsiView.class, Gruppo.CORSI),
        Map.entry(LezioniView.class, Gruppo.CORSI),
        Map.entry(RiepilogoIscrittiView.class, Gruppo.CORSI),
        Map.entry(AllieviView.class, Gruppo.PERSONE),
        Map.entry(GenitoriView.class, Gruppo.PERSONE),
        Map.entry(InsegnantiView.class, Gruppo.PERSONE),
        Map.entry(UtentiView.class, Gruppo.PERSONE),
        Map.entry(GestionePagamentiView.class, Gruppo.ECONOMIA),
        Map.entry(RimborsiView.class, Gruppo.ECONOMIA),
        Map.entry(EmailInviateView.class, Gruppo.DOCUMENTI),
        Map.entry(AnniScolasticiView.class, Gruppo.IMPOSTAZIONI),
        Map.entry(LuoghiView.class, Gruppo.IMPOSTAZIONI),
        Map.entry(FestivitaView.class, Gruppo.IMPOSTAZIONI),
        Map.entry(LezioniInsegnanteView.class, Gruppo.INSEGNANTE),
        Map.entry(MieiAllieviView.class, Gruppo.INSEGNANTE),
        Map.entry(MieiRimborsiView.class, Gruppo.INSEGNANTE),
        Map.entry(FigliView.class, Gruppo.FAMIGLIA),
        Map.entry(PagamentiFigliView.class, Gruppo.FAMIGLIA),
        Map.entry(MieiCorsiView.class, Gruppo.ALLIEVO),
        Map.entry(MieiPagamentiView.class, Gruppo.ALLIEVO),
        Map.entry(ProfiloView.class, Gruppo.ACCOUNT));

    private MenuApp() {
    }

    /**
     * Voci accessibili ({@code voci}, nell'ordine di {@code @Menu}) divise nei gruppi per l'utente con {@code ruoli};
     * i gruppi vuoti non compaiono.
     */
    public static List<Sezione> organizza(List<MenuEntry> voci, Set<String> ruoli) {
        Set<Area> aree = EnumSet.noneOf(Area.class);
        if (ruoli.contains("ADMIN") || ruoli.contains("SEGRETERIA")) {
            aree.add(Area.STAFF);
        }
        if (ruoli.contains("INSEGNANTE")) {
            aree.add(Area.INSEGNANTE);
        }
        if (ruoli.contains("GENITORE")) {
            aree.add(Area.FAMIGLIA);
        }
        if (ruoli.contains("ALLIEVO")) {
            aree.add(Area.ALLIEVO);
        }
        boolean staff = aree.contains(Area.STAFF);
        // Un solo ruolo personale: lista semplice, Documenti dopo le voci del ruolo
        boolean listaSemplice = !staff && aree.size() <= 1;

        Map<Gruppo, List<MenuEntry>> perGruppo = new EnumMap<>(Gruppo.class);
        for (MenuEntry voce : voci) {
            Gruppo gruppo = gruppo(voce, staff, listaSemplice);
            perGruppo.computeIfAbsent(gruppo, g -> new ArrayList<>()).add(voce);
        }
        List<Sezione> sezioni = new ArrayList<>();
        perGruppo.forEach((gruppo, elenco) -> sezioni.add(new Sezione(gruppo, listaSemplice ? null : gruppo.getTitolo(), elenco)));
        return sezioni;
    }

    private static Gruppo gruppo(MenuEntry voce, boolean staff, boolean listaSemplice) {
        Class<?> vista = voce.menuClass();
        if (vista == DocumentiView.class) {
            // Lo staff ha Documenti con la Cronologia; gli altri in alto (o in coda alla lista semplice)
            return staff ? Gruppo.DOCUMENTI : listaSemplice ? Gruppo.ACCOUNT : Gruppo.GENERALE;
        }
        return GRUPPI.getOrDefault(vista, Gruppo.GENERALE);
    }
}
