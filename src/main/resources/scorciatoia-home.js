// Scorciatoia nella schermata Home del telefono (vedi views/ScorciatoiaHome).
// Caricato nella <head> da Application.configurePage: beforeinstallprompt può arrivare prima che le viste siano pronte.
(function () {
    var CHIAVE = 'scorciatoiaHome.chiuso';
    var s = window.scorciatoiaHome = {evento: null, installata: false};

    // Android (Chrome, Edge, Samsung Internet): si tiene l'invito del browser per il pulsante "Aggiungi"
    window.addEventListener('beforeinstallprompt', function (e) {
        e.preventDefault();
        s.evento = e;
    });
    window.addEventListener('appinstalled', function () {
        s.evento = null;
        s.installata = true;
    });

    // "installata" (aperta dalla scorciatoia o appena aggiunta), "ios", "android" (ogni altro smartphone) o "computer"
    s.stato = function () {
        if (s.installata || navigator.standalone === true || window.matchMedia('(display-mode: standalone)').matches) {
            return 'installata';
        }
        var ua = navigator.userAgent;
        // L'iPad si presenta come un Mac, ma ha lo schermo touch
        if (/iPhone|iPad|iPod/.test(ua) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)) {
            return 'ios';
        }
        var mobile = navigator.userAgentData ? navigator.userAgentData.mobile : /Android|Mobi/.test(ua);
        return mobile ? 'android' : 'computer';
    };

    // Banner chiuso su questo dispositivo; la memoria del browser può mancare (navigazione privata)
    s.chiuso = function () {
        try {
            return localStorage.getItem(CHIAVE) === '1';
        } catch (e) {
            return false;
        }
    };
    s.chiudi = function () {
        try {
            localStorage.setItem(CHIAVE, '1');
        } catch (e) {
            // resta chiuso solo fino al prossimo caricamento
        }
    };

    // Clic su "Aggiungi": l'installazione del browser va aperta nel gesto dell'utente, senza passare dal server;
    // se il browser non la offre (iPhone, Firefox, già rifiutata) il server mostra le istruzioni
    s.aggiungi = function (pulsante) {
        var evento = s.evento;
        if (evento) {
            s.evento = null;
            evento.prompt();
            evento.userChoice.then(function (scelta) {
                if (scelta.outcome === 'accepted') {
                    pulsante.dispatchEvent(new CustomEvent('scorciatoia-installata'));
                }
            });
        } else {
            pulsante.dispatchEvent(new CustomEvent('scorciatoia-istruzioni'));
        }
    };
})();
