package it.attocchi.security;

import it.attocchi.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ogni notte sospende gli account che non entrano da più di {@link UserService#MESI_INATTIVITA} mesi (non gli admin, non
 * chi non è mai entrato): si riattivano da soli con «Password dimenticata?».
 */
@Component
public class SospensioneInattivi {

    private static final Logger log = LoggerFactory.getLogger(SospensioneInattivi.class);

    private final UserService userService;

    public SospensioneInattivi(UserService userService) {
        this.userService = userService;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Europe/Rome")
    public void sospendi() {
        List<String> sospesi = userService.sospendiInattivi(LocalDateTime.now());
        if (!sospesi.isEmpty()) {
            log.info("Account sospesi per inattività ({} mesi senza accessi): {}", UserService.MESI_INATTIVITA, sospesi);
        }
    }
}
