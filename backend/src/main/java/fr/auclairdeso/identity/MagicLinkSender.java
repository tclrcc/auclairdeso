package fr.auclairdeso.identity;

import fr.auclairdeso.shared.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;

/**
 * Emails the magic link once Spring Security has generated a one-time token.
 */
@Component
class MagicLinkSender implements OneTimeTokenGenerationSuccessHandler {

    private final JavaMailSender mailSender;
    private final AppProperties properties;

    MagicLinkSender(JavaMailSender mailSender, AppProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, OneTimeToken token) {
        var variables = new HashMap<String, String>();
        variables.put("token", token.getTokenValue());
        var builder = UriComponentsBuilder.fromUri(properties.frontendUrl())
            .path("/connexion/verifier")
            .queryParam("token", "{token}");
        ReturnPaths.from(request.getParameter("redirect")).ifPresent(path -> {
            builder.queryParam("redirect", "{redirect}");
            variables.put("redirect", path);
        });
        var link = builder.encode().buildAndExpand(variables).toUri();

        var message = new SimpleMailMessage();
        message.setFrom(properties.mailFrom());
        message.setTo(token.getUsername());
        message.setSubject("Votre lien de connexion — Au clair de So");
        message.setText("""
                Bonjour,

                Voici votre lien de connexion à Au clair de So.
                Il est valable %d minutes et ne fonctionne qu'une seule fois :

                %s

                Si vous n'êtes pas à l'origine de cette demande, ignorez simplement cet email.
                """.formatted(MagicLinkConfiguration.TOKEN_VALIDITY.toMinutes(), link));

        mailSender.send(message);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
