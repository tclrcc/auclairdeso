# Audit n° 2 — Sécurité et protection des données

> Audit en **lecture seule** du 10 novembre 2026, sur la branche `audit/novembre`
> (dernier commit `dd75c3b`). Aucun code modifié, aucune migration, aucun commit, aucun test
> exécuté. Seul fichier écrit : celui-ci.
>
> **`deploy/.env` n'a pas été ouvert.** La présence d'une variable a été vérifiée par
> `grep -c "^NOM=" deploy/.env`, qui ne renvoie qu'un nombre. Aucune valeur de configuration
> n'apparaît dans ce document. Aucun fichier `.swp`, `.bak` ou `.orig` n'a été ouvert : il n'en
> existe plus aucun dans l'arbre de travail.

**Comment lire les preuves.** Les préfixes de l'audit n° 1 sont repris :

| Préfixe | Chemin complet |
| --- | --- |
| `be/` | `backend/src/main/java/fr/auclairdeso/` |
| `fe/` | `frontend/src/app/` |

Les migrations sont citées par leur numéro seul (`V3:5` = `backend/src/main/resources/db/migration/V3__create_one_time_tokens.sql`, ligne 5), les tests par leur chemin sous `backend/src/test/java/fr/auclairdeso/`. `application.yaml` et `application-dev.yaml` sont sous `backend/src/main/resources/`.

**CONFIRMÉ** = j'ai lu le code cité. **SUPPOSÉ** = déduit d'un nom, d'un chemin ou du comportement
documenté d'une bibliothèque extérieure, sans avoir lu son implémentation.

**Gravité** : Bloquant avant ouverture / Important / Confort. **Effort** : S / M / L.

**Recoupements.** Quand un point touche l'audit n° 1 (`docs/audit/01-conformite.md`), il renvoie à
son numéro (S1…S10, A*, B-E*, C*, D*) au lieu d'être refait. Un constat de cet audit **corrige la
prémisse de son S2** : voir S3 ci-dessous.

**Deux environnements à ne pas confondre.** La *recette* envoie ses emails à Mailpit, en réseau
interne (`deploy/compose.yaml:46-47`, `:28-34`) : aucun email ne quitte le VPS. La *production*
enverra par le SMTP de Gmail. Plusieurs constats ne mordent qu'en production, et c'est dit à chaque
fois.

---

## 1. Synthèse

### Bloquant avant ouverture

**S1 — Aucun en-tête de sécurité sur les pages HTML.** Ni CSP, ni HSTS, ni `X-Frame-Options`, ni
`X-Content-Type-Options`, ni `Referrer-Policy`, ni `Permissions-Policy` : zéro occurrence dans tout
le dépôt. Les sept seuls `add_header` vivent dans la porte de recette et ne posent qu'un
`X-Robots-Tag` et un `Cache-Control` (`deploy/host/nginx-auclairdeso.conf:26`, `43-46`, `53-54`).
`SecurityConfiguration` n'appelle jamais `.headers(...)`, donc les défauts de Spring Security
s'appliquent — mais **seulement aux réponses `/api/**`**. Les pages HTML viennent du serveur de
rendu Node (`frontend/src/server.ts:30-48`), qui n'en pose aucun, et aucune des deux couches nginx
n'en ajoute. Conséquences concrètes : **aucun HSTS sur le seul point de terminaison TLS**
(`deploy/host/nginx-auclairdeso.conf:18-23`), l'administration est encadrable dans une iframe, et
le `Referer` d'une page portant le jeton de connexion part vers tout lien externe. CONFIRMÉ.
Effort S.

**S2 — `/api/auth/magic-link` est un relais d'email, et son abus coupe toutes les notifications.**
N'importe qui, sans compte, peut faire envoyer un email à n'importe quelle adresse : 3 par adresse
toutes les 15 minutes, 20 par adresse IP par heure
(`be/identity/MagicLinkRateLimitFilter.java:31-32`, `be/identity/MagicLinkSender.java:42-57`). En
recette c'est inoffensif. **En production, avec le SMTP de Gmail**, une campagne depuis plusieurs
adresses IP épuise le quota d'envoi du compte, et **tous** les emails de l'application s'arrêtent
alors : confirmations, refus, expirations, rappels, notes de séance. La reprise réessaie douze fois
puis abandonne (`be/notification/FailedNotificationRetries.java:17`). S'y ajoute l'atteinte à la
réputation d'expéditeur, qui se répare lentement. CONFIRMÉ. Effort M. **Bloquant pour la mise en
production, pas pour la recette.**

**S3 — `deploy/.env` n'a pas les trois `BILLING_*` que `compose.yaml` déclare obligatoires, et le
prochain déploiement ne démarrera pas.** `deploy/compose.yaml:51-53` emploie la forme `:?`
(« erreur si absent ») pour `BILLING_PRACTITIONER_NAME`, `BILLING_ADDRESS` et `BILLING_SIRET` ;
`grep -c` en trouve **zéro** dans un fichier de 15 lignes. `deploy/deploy.sh:3` est en
`set -euo pipefail`, et la ligne 9 lance `docker compose up` : le déploiement s'arrête avant de
démarrer quoi que ce soit. Ce n'est pas un trou de sécurité, c'est un blocage de livraison — et
cela **corrige la prémisse du S2 de l'audit n° 1** : les valeurs de remplacement de
`application.yaml:71-73` ne peuvent pas être servies en recette, puisque le conteneur ne démarre
pas. CONFIRMÉ. Effort S, mais la décision de domiciliation est en amont.

### Important

**S4 — Le jeton de lien magique est en clair partout.** Il voyage en paramètre de requête
(`be/identity/MagicLinkSender.java:33-40`), donc il s'écrit dans les journaux d'accès des deux
nginx, dans l'historique du navigateur et — faute de `Referrer-Policy` (S1) — dans le `Referer`. La
porte de recette le recopie même dans un cookie (`auclairdeso_next=$request_uri`,
`deploy/host/nginx-auclairdeso.conf:53`). Il est de plus stocké **non haché** en base (`V3:5`).
Atténuations réelles : usage unique, 10 minutes de validité, et sa consommation exige un clic — un
antivirus de messagerie ne le brûle pas (`fe/public/login/verify-page.ts:24-43`). CONFIRMÉ.
Effort M : le passer en fragment (`#token=`), que le navigateur n'envoie jamais au serveur.

**S5 — Aucune limitation de débit ni verrouillage sur `/api/auth/password`.** Le filtre de
limitation ne couvre que le lien magique (`be/identity/MagicLinkRateLimitFilter.java:36`, et
`identity/MagicLinkRateLimitFilterTests.java` fixe exprès ce périmètre), il n'y a aucun `limit_req`
dans les deux configurations nginx, et fail2ban ne surveille que la porte Basic de la recette
(`deploy/host/fail2ban-nginx-http-auth.local:2-9`). Le mot de passe du personnel se teste donc sans
entrave et sans trace. Atténuations réelles : 12 caractères minimum
(`be/identity/StaffPasswords.java:19`, `73-80`) et le mot de passe **seul** n'ouvre pas
`/api/admin/**`, où le double facteur est exigé (`be/shared/security/SecurityConfiguration.java:46`).
CONFIRMÉ. Effort S.

**S6 — `/contenus/` est le seul vecteur de contenu stocké du projet, et il est sur l'origine du
site.** nginx y sert les fichiers déposés à la main dans `MEDIA_DIR`, type deviné de l'extension,
sans `X-Content-Type-Options` et sans CSP (`deploy/nginx.conf:30-36`, `deploy/compose.yaml:84`). Un
`.html` ou un `.svg` déposé là exécute du script sur l'origine du site, donc dans le contexte de la
session de la praticienne. Le chemin d'écriture est un `scp` par le développeur seul : la
probabilité est faible, mais c'est là que vont les photos et les vidéos de So. CONFIRMÉ. Effort S.

**S7 — L'adresse du domicile de la cliente est gravée dix ans sur la note.** Pour une séance
`CLIENT_HOME`, le lieu recopié sur la note est « Au domicile de la cliente : » suivi de l'adresse
complète (`be/billing/Bill.java:146-152`), en `updatable = false` (`:38-39`). L'effacement de
compte ne retire que l'adresse électronique (`be/billing/ErasedClientBills.java:22-25`,
`be/billing/BillRepository.java:29-31`) : l'adresse postale reste, à côté du nom, pour dix ans.
L'arrêté 83-50/A exige bien le lieu de la séance — le problème n'est pas de la conserver, c'est que
ni la politique de confidentialité ni le message d'effacement ne le disent. Recoupe l'audit n° 1
D5 et B-E2, qui portaient sur le nom ; l'adresse est un second élément, non signalé.
CONFIRMÉ. Effort S (texte) à M (lieu moins précis sur la note).

**S8 — L'EXIF des photos n'est pas retiré.** `Photo.of` vérifie la taille et les premiers octets,
puis stocke les octets tels quels (`be/booking/Photo.java:14-21`) : les coordonnées GPS,
l'horodatage et le modèle d'appareil d'une photo prise au téléphone sont conservés en base et
resservis au personnel. Pour un site dont les clientes envoient leur visage, et où l'adresse n'est
demandée que pour un seul mode de consultation, c'est un manquement à la minimisation : la photo
peut livrer le domicile que le formulaire ne demandait pas. CONFIRMÉ. Effort S (ré-encoder à la
réception, ce qui supprime l'EXIF et neutralise au passage tout fichier polyglotte).

**S9 — Les droits RGPD sont partiellement implémentés, et la politique promet plus que le code.**
La politique affirme « Vous pouvez retirer cet accord à tout moment » à propos du consentement aux
données sensibles (`fe/public/legal/confidentialite-page.html:34`) : **aucun mécanisme de retrait
n'existe**, hors suppression totale du compte (`be/booking/AccountErasureController.java:26`). État
réel des six droits : effacement **fait** et bien fait ; accès **partiel** ; rectification,
portabilité, limitation et opposition **absents du logiciel**. Par ailleurs OVH est nommé comme
sous-traitant, mais **Google / Gmail ne l'est pas** (`:98` dit « le service de messagerie de la
praticienne »), alors que les emails lui confient nom, date, prestation, montant, adresse de séance
et pseudo Messenger. CONFIRMÉ. Effort S (textes et procédure écrite) à M (export).

**S10 — Aucun scan de dépendances, et une CVE Tomcat probable.** Pas de `dependabot.yml`, pas
d'OWASP dependency-check, pas de `npm audit` en CI, pas de CodeQL : `.github/` ne contient que
`workflows/ci.yml`. Or Spring Boot 4.1.1 résout **tomcat-embed-core 11.0.24** (vérifié dans le
dépôt Maven local), version qui tombe dans la plage attribuée à CVE-2026-86350, dont le correctif
est annoncé en 11.0.26. La condition d'exploitation serait que du HTTP/2 atteigne Tomcat ; ici nginx
lui parle en HTTP/1.1 (`deploy/nginx.conf:18`), donc elle n'est probablement pas réunie. Version
**CONFIRMÉE**, avis de sécurité **SUPPOSÉ** : à vérifier sur spring.io/security et la NVD.
Effort S.

---

## 2. Ce qui est solide, et qu'il ne faut pas casser

Avant les écarts : plusieurs points sont mieux traités que la moyenne d'un projet de cette taille.
Ils sont listés ici pour qu'une correction ultérieure ne les défasse pas sans le voir.

| # | Ce qui est tenu | Preuve | État |
| --- | --- | --- | --- |
| T1 | **Aucune IDOR sur les parcours cliente** : l'email vient toujours de la session, jamais du corps de la requête, et l'annulation cherche la paire (identifiant, email) | `be/booking/AppointmentRepository.java:34-35`, appelé en `be/booking/BookingRequests.java:116` ; `be/booking/BookingController.java:31`, `36`, `42`, `49` ; `be/booking/AccountErasureController.java:28` | CONFIRMÉ |
| T2 | **Aucune injection SQL possible** : JPQL à paramètres nommés ou `?` partout, pas une concaténation | `be/booking/ClientRepository.java:31-42`, `be/booking/AppointmentPhotoRepository.java:13`, `18-22`, `be/identity/ClientAccountDeletion.java:37`, `be/billing/BillNumbers.java:25-26`, `be/billing/BillRepository.java:29-31` | CONFIRMÉ |
| T3 | **Aucun puits XSS dans le frontend** : zéro `innerHTML`, `bypassSecurityTrust*`, `DomSanitizer`, `eval`, `document.write`, `localStorage` sous `frontend/src` | recherche exhaustive, aucun résultat | CONFIRMÉ |
| T4 | **Injection CSV traitée**, apostrophe en tête et guillemets doublés | `be/billing/ReceiptBookCsv.java:44-50` | CONFIRMÉ |
| T5 | **Type de photo déduit des premiers octets**, pas du `Content-Type` déclaré ; taille bornée et bornes alignées entre Spring et nginx | `be/booking/Photo.java:23-34`, `:12`, `:15` ; `application.yaml:10-13` ; `deploy/nginx.conf:15` | CONFIRMÉ |
| T6 | **Pas d'énumération de comptes** : un email inconnu reçoit un hachage inutilisable tiré au hasard, donc la même réponse et le même temps qu'un compte réel | `be/identity/AccountUserDetailsService.java:22-23`, `:27`, `38-39` | CONFIRMÉ |
| T7 | **CSRF actif sur tout**, connexion et déconnexion comprises, et le cookie est posé dès la première réponse ; le défaut d'Angular correspond au préréglage `spa()` | `be/shared/security/SecurityConfiguration.java:50-51`, `be/shared/security/CsrfCookieFilter.java:20-23`, `fe/app.config.ts:13` | CONFIRMÉ |
| T8 | **Pas de redirection ouverte** : double filtre, serveur et client | `be/identity/ReturnPaths.java:13`, `fe/core/auth/safe-redirect.ts:3` | CONFIRMÉ |
| T9 | **Le lien magique n'est pas empoisonnable par l'en-tête `Host`** : l'URL est bâtie depuis une valeur de configuration | `be/identity/MagicLinkSender.java:33`, `be/shared/AppProperties.java:15` | CONFIRMÉ |
| T10 | **La chaîne `X-Forwarded-*` est correcte** : la couche hôte écrase `X-Forwarded-Proto` avec `$scheme`, la passerelle le relaie par un `map`, et `forward-headers-strategy: native` le fait honorer. `$proxy_add_x_forwarded_for` **ajoute** au lieu d'écraser : la limitation par IP n'est pas contournable en envoyant son propre `X-Forwarded-For` | `deploy/host/nginx-auclairdeso.conf:60-61`, `deploy/nginx.conf:5-8`, `:20-21`, `application.yaml:2` | CONFIRMÉ (configuration) / SUPPOSÉ (comportement de `RemoteIpValve`) |
| T11 | **`/actuator/**` et Swagger ne sont pas atteignables de l'extérieur** : la passerelle ne relaie au backend que `/api/`, tout le reste part au frontend, et springdoc est en plus coupé par variable d'environnement | `deploy/nginx.conf:17`, `:38` ; `deploy/compose.yaml:57-58` | CONFIRMÉ |
| T12 | **`anyRequest().denyAll()`** ferme tout ce qui n'est pas explicitement ouvert | `be/shared/security/SecurityConfiguration.java:49` | CONFIRMÉ |
| T13 | **Validation serveur sur les dix `@RequestBody` / `@RequestPart`** du dépôt, sans exception | voir le volet D, § D.1 | CONFIRMÉ |
| T14 | **La porte de recette ne fuit pas vers l'application** : les identifiants Basic sont effacés avant le relais, et fail2ban surveille la porte | `deploy/host/nginx-auclairdeso.conf:62`, `deploy/host/fail2ban-nginx-http-auth.local:2-9` | CONFIRMÉ |
| T15 | **Aucun secret n'a jamais été commité.** Seuls `deploy/.env.example` et `nginx-auclairdeso-secret.conf.example` (littéral `CHANGE_ME`) sont suivis ; aucune révision d'aucun yaml, properties ou env suivi n'a jamais porté autre chose qu'un `${...}`. `deploy/.env` est ignoré et en mode `0600` | `git log --diff-filter=A` sur `*.env*`, `*secret*`, `*.pem`, `*.key`, `*.swp` ; `git grep` sur les 36 commits ; `git ls-files \| grep -i env` ; `.gitignore:34` | CONFIRMÉ |
| T16 | **Le `deploy/.env.swp` signalé par l'audit n° 1 a disparu** de l'arbre de travail. Il était bien ignoré (`.gitignore:20`, via `.env.*`) et n'a jamais été suivi | `git check-ignore -v deploy/.env.swp` ; recherche de `*.swp`, `*.bak`, `*.orig`, `*~` : aucun résultat | CONFIRMÉ |
| T17 | **Aucune donnée de cliente dans les journaux applicatifs** : cinq `log.info` en tout, tous des compteurs | `be/booking/BookingSchedule.java:28`, `37`, `44` ; seule exception, les emails du personnel : `be/identity/StaffBootstrap.java:41`, `44` | CONFIRMÉ |
| T18 | **Aucune donnée personnelle ne passe par le rendu serveur** : tous les écrans authentifiés sont en rendu client, et l'intercepteur du rendu serveur ne transmet aucun cookie | `fe/app.routes.server.ts:9-25`, `fe/core/api-origin.ts:10-15` | CONFIRMÉ |
| T19 | **Conteneurs non privilégiés**, tout à l'écoute sur `127.0.0.1`, journaux Docker bornés | `backend/Dockerfile:15-17`, `frontend/Dockerfile:12-17`, `deploy/compose.yaml:32-33`, `:81`, `:5-9` | CONFIRMÉ |
| T20 | **Le motif de consultation ne quitte jamais le site** : aucun email ne le contient, et le commentaire d'en-tête du module en fait une règle | `be/notification/Messages.java:14-16` ; aucun appel à `reason()` dans ce fichier | CONFIRMÉ |

---

## 3. Volet A — Authentification et sessions

| # | Constat | Preuve | État | Gravité | Effort |
| --- | --- | --- | --- | --- | --- |
| A1 | Lien magique : validité 10 minutes, fixée dans le code | `be/identity/MagicLinkConfiguration.java:15`, `:26` | CONFIRMÉ | — | — |
| A2 | Usage unique et rejeu : délégués à `JdbcOneTimeTokenService` de Spring Security, hors du dépôt | `be/identity/MagicLinkConfiguration.java:18-20` | SUPPOSÉ | — | — |
| A3 | **Le jeton voyage en paramètre de requête** : journaux nginx des deux couches, historique du navigateur, `Referer`. Voir S4 | `be/identity/MagicLinkSender.java:33-40`, `fe/app.routes.server.ts:10` | CONFIRMÉ | Important | M |
| A4 | **Le jeton est stocké non haché** : une lecture de la base ou d'une sauvegarde donne un jeton utilisable pendant 10 minutes | `V3:4-8` | CONFIRMÉ | Important | M |
| A5 | La porte de recette recopie l'URL demandée — jeton compris — dans un cookie de 10 minutes | `deploy/host/nginx-auclairdeso.conf:53` | CONFIRMÉ | Confort | S |
| A6 | La consommation du jeton exige un POST déclenché par un clic : un antivirus de messagerie qui préouvre le lien ne le brûle pas | `fe/public/login/verify-page.ts:24-43`, `fe/core/auth/auth-session.ts:48-51` | CONFIRMÉ | — | — |
| A7 | Pas d'énumération par le lien magique : un format d'adresse valide donne 204, et le compte est créé à la première connexion réussie | `be/identity/MagicLinkConfiguration.java:24-27`, `be/identity/LoginRecorder.java:23-27` | CONFIRMÉ | — | — |
| A8 | **Aucune limitation ni verrouillage sur `/api/auth/password`**. Voir S5 | `be/shared/security/SecurityConfiguration.java:60-64` (aucun filtre), `be/identity/MagicLinkRateLimitFilter.java:36` | CONFIRMÉ | Important | S |
| A9 | Aucune limitation non plus sur `/api/auth/magic-link/verify` : seule l'entropie du jeton (UUID) protège du devinement | `be/identity/MagicLinkRateLimitFilter.java:36`, `V3:5` | CONFIRMÉ | Confort | S |
| A10 | Double facteur réellement exigé sur `/api/admin/**` : jeton **et** mot de passe | `be/shared/security/SecurityConfiguration.java:22`, `28-30`, `46` | CONFIRMÉ | — | — |
| A11 | Mot de passe : bcrypt par le `DelegatingPasswordEncoder`, 12 caractères minimum, 72 octets maximum (limite de bcrypt, documentée) | `be/identity/PasswordConfiguration.java:13`, `be/identity/StaffPasswords.java:19`, `:22`, `73-80` | CONFIRMÉ | — | — |
| A12 | Aucune exigence de composition au-delà de la longueur, et aucune liste de mots de passe courants | `be/identity/StaffPasswords.java:73-80` | CONFIRMÉ | Confort | S |
| A13 | Définir son premier mot de passe exige le lien magique ; le changer exige les deux facteurs | `be/identity/StaffPasswords.java:42-46` | CONFIRMÉ | — | — |
| A14 | La réinitialisation par un `ADMIN` efface le hachage **et** ferme toutes les sessions du compte | `be/identity/StaffPasswords.java:68-69` | CONFIRMÉ | — | — |
| A15 | Mais **changer** son mot de passe ne ferme pas les autres sessions : une session volée survit au changement | `be/identity/StaffPasswords.java:48-49` | CONFIRMÉ | Confort | S |
| A16 | **`/api/account/password` est la seule route du personnel hors `bothFactors`** : le second facteur y est vérifié à la main, donc à la merci d'une régression qu'aucun matcher ne rattraperait | `be/shared/security/SecurityConfiguration.java:47` contre `be/identity/StaffPasswords.java:42-46` | CONFIRMÉ | Important | S |
| A17 | **`Secure` et `SameSite` des cookies `SESSION` et `XSRF-TOKEN` ne sont jamais configurés** : aucun `server.servlet.session.cookie.*` dans le dépôt. L'attribut `Secure` suit `request.isSecure()`, donc la chaîne de proxys. Celle-ci est correcte aujourd'hui (T10) : ce n'est pas un défaut actuel, c'est une fragilité qu'aucun test ne couvre, et qu'une erreur dans l'une des deux configurations nginx retirerait en silence | `application.yaml` (absence), `:2` ; `deploy/host/nginx-auclairdeso.conf:61` ; `deploy/nginx.conf:5-8`, `:21` | CONFIRMÉ | Important | S |
| A18 | `HttpOnly` sur `SESSION` et `SameSite=Lax` viennent des défauts de Spring Session ; `XSRF-TOKEN` est volontairement lisible par le script, comme l'exige une application à page unique | défauts de `DefaultCookieSerializer` et de `CookieCsrfTokenRepository.withHttpOnlyFalse()` | SUPPOSÉ | — | — |
| A19 | Session : 1 h d'inactivité, stockée en base, survit aux redéploiements. **Aucune durée de vie absolue** et **aucune limite de sessions simultanées** : `.sessionManagement(...)` n'est jamais appelé | `application.yaml:54-57`, `V5:4-26` ; absence dans `be/shared/security/SecurityConfiguration.java` | CONFIRMÉ | Confort | S |
| A20 | Protection contre la fixation de session : défaut de Spring Security (`changeSessionId`) | absence de configuration contraire | SUPPOSÉ | — | — |
| A21 | Déconnexion : POST protégé par CSRF, session invalidée côté serveur, 204. Mais **aucun `deleteCookies`** : le cookie `SESSION` n'est pas expiré côté navigateur | `be/shared/security/SecurityConfiguration.java:65-67` | CONFIRMÉ | Confort | S |
| A22 | L'effacement de compte ferme la session avant de répondre, et l'écouteur supprime la connexion, les jetons encore valides et les sessions ouvertes ailleurs | `be/booking/AccountErasureController.java:27-30`, `be/identity/ClientAccountDeletion.java:32-39` | CONFIRMÉ | — | — |
| A23 | **Une cliente bloquée s'authentifie quand même** : le `UserDetails` est construit sans `disabled` ni `locked`, le blocage n'agissant qu'à la réservation. Volontaire (elle doit pouvoir voir et annuler ses rendez-vous), mais nulle part écrit | `be/identity/AccountUserDetailsService.java:40-43` contre `be/booking/BookingRequests.java:71-73` | CONFIRMÉ | Confort | S (un commentaire) |
| A24 | `StaffBootstrap` réapplique les rôles de la configuration **à chaque démarrage**, et rétrograde aussi bien qu'il promeut : un rôle corrigé en base revient silencieusement à ce que portent les variables d'environnement | `be/identity/StaffBootstrap.java:28-31`, `42-44` | CONFIRMÉ | Confort | — (comportement voulu, à connaître) |
| A25 | Le filtre de limitation vit **hors de la chaîne Spring Security** (composant auto-enregistré, priorité maximale) et compare l'URI par égalité stricte : une variante de chemin qui atteindrait quand même le filtre de génération échapperait à la limite | `be/identity/MagicLinkRateLimitFilter.java:21-22`, `:36` | CONFIRMÉ (le code) / SUPPOSÉ (qu'une variante passe) | Confort | S |
| A26 | Les compteurs de limitation sont en mémoire : remis à zéro à chaque redéploiement, et non partagés si une seconde instance apparaît | `be/identity/AttemptLimiter.java:23-27` | CONFIRMÉ | Confort | — |

### A.1 — Le chemin d'une requête, pour situer les constats

Reconstitué à partir de `be/shared/security/SecurityConfiguration.java` et de
`be/identity/MagicLinkRateLimitFilter.java:22` :

```
RemoteIpValve de Tomcat            (application.yaml:2, forward-headers-strategy: native)
  └─ MagicLinkRateLimitFilter      hors chaîne de sécurité, priorité maximale
      └─ chaîne Spring Security
          ├─ CsrfFilter
          ├─ CsrfCookieFilter                      (:51)
          ├─ LogoutFilter                          (:66)
          ├─ filtre de génération du jeton         (:53)
          ├─ filtre de connexion par mot de passe  (:61)
          ├─ filtre de connexion par jeton         (:55)
          └─ AuthorizationFilter                   (:37-49)
              └─ DispatcherServlet → préfixe /api → contrôleurs
```

Ce détail explique un point qui surprend à la lecture : les quatre routes `/api/auth/**` ne portent
aucune règle `permitAll`, et sont pourtant atteignables sans compte. C'est parce que les filtres
d'authentification **précèdent** `AuthorizationFilter` : la réponse est déjà écrite quand la règle
`/api/**` → `authenticated()` serait évaluée. C'est le fonctionnement normal de Spring Security, et
non un contournement — mais la règle `:48` ne protège donc pas ce qu'on croirait en la lisant.

---

## 4. Volet B — Autorisation

**Toute l'autorisation du projet tient dans un seul fichier.** Il n'y a aucun `@PreAuthorize`,
`@PostAuthorize`, `@Secured` ni `@RolesAllowed` dans le dépôt, aucun `@EnableMethodSecurity`, et un
seul `SecurityFilterChain`. Les onze règles de `be/shared/security/SecurityConfiguration.java:37-49`
sont la seule porte. Le préfixe `/api` est posé au moment de l'exécution
(`be/shared/web/ApiPathConfiguration.java:14-16`) : aucune annotation de contrôleur ne le montre.

### B.1 — Les onze règles, dans leur ordre d'évaluation

| # | Ligne | Chemin | Règle |
| --- | --- | --- | --- |
| R1 | `:38` | `/error` | `permitAll` |
| R2 | `:39` | `/actuator/health`, `/actuator/info` | `permitAll` |
| R3 | `:40` | `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | `permitAll` |
| R4 | `:41` | `GET /api/offerings/**` | `permitAll` |
| R5 | `:42` | `GET /api/legal` | `permitAll` |
| R6 | `:43` | `DELETE /api/account` | rôle `CLIENT` |
| R7 | `:44-45` | `DELETE /api/admin/staff/*/password` | deux facteurs + rôle `ADMIN` |
| R8 | `:46` | `/api/admin/**` | deux facteurs + rôle `PRACTITIONER` ou `ADMIN` |
| R9 | `:47` | `/api/account/password` | rôle `PRACTITIONER` ou `ADMIN`, **sans exigence de facteur** |
| R10 | `:48` | `/api/**` | authentifié, quel que soit le rôle |
| R11 | `:49` | tout le reste | `denyAll` |

### B.2 — Tableau complet des routes

47 routes de contrôleur, 4 routes générées par Spring Security, 6 chemins hors `/api`. « Qui peut
appeler » est la conséquence de la règle, pas une seconde vérification : il n'y en a aucune.

#### Publiques, sans aucun compte

| Méthode | Chemin | Déclaration | Règle | Qui peut appeler |
| --- | --- | --- | --- | --- |
| GET | `/api/offerings` | `be/catalog/OfferingController.java:13`, `:22` | R4 | tout le monde |
| GET | `/api/offerings/{slug}` | `be/catalog/OfferingController.java:13`, `:27` | R4 | tout le monde |
| GET | `/api/offerings/{slug}/slots` | `be/booking/SlotController.java:25` | R4 | tout le monde |
| GET | `/api/legal` | `be/legal/LegalController.java:21` | R5 | tout le monde (obligatoire : LCEN, article 6 III) |
| POST | `/api/auth/magic-link` | `be/shared/security/SecurityConfiguration.java:53` | avant R10 | tout le monde, limité (3/adresse/15 min, 20/IP/h) |
| POST | `/api/auth/magic-link/verify` | `:55` | avant R10 | tout le monde, non limité |
| POST | `/api/auth/password` | `:61` | avant R10 | tout le monde, **non limité** (S5) |
| POST | `/api/auth/logout` | `:66` | avant R10 | tout le monde, protégé par CSRF |
| toutes | `/error` | — | R1 | tout le monde ; ne rend qu'un ProblemDetail |
| GET | `/actuator/health`, `/actuator/info` | `application.yaml:59-63` | R2 | tout le monde **côté conteneur**, non relayé par la passerelle (T11) |
| GET | `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | `:40` | R3 | idem, et coupé par variable d'environnement (`deploy/compose.yaml:57-58`) |

#### Authentifiées, quel que soit le rôle

| Méthode | Chemin | Déclaration | Règle | Portée des données |
| --- | --- | --- | --- | --- |
| GET | `/api/me` | `be/identity/CurrentUserController.java:18` | R10 | la personne connectée |
| POST | `/api/bookings` | `be/booking/BookingController.java:25` | R10 | crée pour l'email de la session (T1) |
| GET | `/api/bookings/mine` | `:34` | R10 | ses rendez-vous seulement (T1) |
| GET | `/api/bookings/profile` | `:40` | R10 | sa fiche seulement (T1) |
| POST | `/api/bookings/{id}/cancel` | `:47` | R10 | **paire (identifiant, email) vérifiée** (T1) |

#### Réservée au rôle `CLIENT`

| Méthode | Chemin | Déclaration | Règle | Portée |
| --- | --- | --- | --- | --- |
| DELETE | `/api/account` | `be/booking/AccountErasureController.java:26` | R6 | son propre compte, email pris de la session |

#### Réservée au personnel, **un seul facteur suffit au niveau du chemin**

| Méthode | Chemin | Déclaration | Règle | Remarque |
| --- | --- | --- | --- | --- |
| PUT | `/api/account/password` | `be/identity/AccountPasswordController.java:21` | R9 | le second facteur est vérifié dans le service (A16) |

#### Réservées au personnel avec les deux facteurs (R8)

| Méthode | Chemin | Déclaration |
| --- | --- | --- |
| GET | `/api/admin/staff` | `be/identity/StaffController.java:21` |
| GET | `/api/admin/offerings` | `be/catalog/OfferingAdminController.java:27` |
| GET | `/api/admin/offerings/{slug}` | `:32` |
| POST | `/api/admin/offerings` | `:37` |
| PUT | `/api/admin/offerings/{slug}` | `:47` |
| GET | `/api/admin/appointments/pending` | `be/booking/AgendaController.java:28` |
| GET | `/api/admin/appointments?from=&to=` | `:33` |
| GET | `/api/admin/appointments/to-close` | `:75` |
| GET | `/api/admin/appointments/{id}` | `:43` |
| GET | `/api/admin/appointments/{id}/photo` | `:49` |
| POST | `/api/admin/appointments` | `:59` |
| POST | `/api/admin/appointments/{id}/confirm` | `:65` |
| POST | `/api/admin/appointments/{id}/decline` | `:70` |
| POST | `/api/admin/appointments/{id}/cancel` | `:80` |
| POST | `/api/admin/appointments/{id}/complete` | `:85` |
| POST | `/api/admin/appointments/{id}/no-show` | `:90` |
| GET | `/api/admin/clients?query=&filter=` | `be/booking/ClientAdminController.java:29` |
| GET | `/api/admin/clients/{id}` | `:35` |
| PUT | `/api/admin/clients/{id}` | `:40` |
| POST | `/api/admin/clients/{id}/trust` | `:45` |
| POST | `/api/admin/clients/{id}/distrust` | `:50` |
| POST | `/api/admin/clients/{id}/block` | `:55` |
| POST | `/api/admin/clients/{id}/unblock` | `:60` |
| POST | `/api/admin/clients/{id}/notes` | `:65` |
| DELETE | `/api/admin/clients/{id}/notes/{noteId}` | `:71` |
| GET | `/api/admin/closures` | `be/booking/ClosureAdminController.java:25` |
| POST | `/api/admin/closures` | `:30` |
| DELETE | `/api/admin/closures/{id}` | `:40` |
| GET | `/api/admin/bills?appointmentId=` | `be/billing/BillAdminController.java:25` |
| GET | `/api/admin/bills/unpaid` | `:30` |
| GET | `/api/admin/bills/{id}` | `:35` |
| POST | `/api/admin/bills/{id}/payment` | `:40` |
| DELETE | `/api/admin/bills/{id}/payment` | `:45` |
| GET | `/api/admin/receipts?year=` | `be/billing/ReceiptBookController.java:28` |
| GET | `/api/admin/receipts/export?year=` | `:33` |

#### Réservée au rôle `ADMIN` avec les deux facteurs (R7)

| Méthode | Chemin | Déclaration | Remarque |
| --- | --- | --- | --- |
| DELETE | `/api/admin/staff/{email}/password` | `be/identity/StaffController.java:28` | la cible est filtrée sur les comptes du personnel ; un `ADMIN` peut réinitialiser n'importe quel membre, y compris lui-même (`be/identity/StaffPasswords.java:64-65`) |

#### Aucun autre chemin

Tout ce qui n'est pas dans ce tableau tombe sur R11, `denyAll` : le backend ne sert rien d'autre.
Le module `notification` n'a aucun contrôleur ; il ne réagit qu'à des événements
(`be/notification/BookingNotifications.java:37`, `:58`, `:66`).

### B.3 — Constats du volet

| # | Constat | Preuve | État | Gravité | Effort |
| --- | --- | --- | --- | --- | --- |
| B1 | **Les fiches clientes, les notes privées, les photos, les notes de séance et le livre des recettes sont tous sous `/api/admin/**`** : personnel **et** double facteur. Rien de tout cela n'est accessible à une cliente, ni anonymement | `be/shared/security/SecurityConfiguration.java:46` ; tableau B.2 | CONFIRMÉ | — | — |
| B2 | **Les photos ne sont servies qu'au personnel**, sans cache, type restreint à ce que la détection a reconnu | `be/booking/AgendaController.java:48-57`, `be/booking/Photo.java:23-34` | CONFIRMÉ | — | — |
| B3 | **Une cliente ne peut jamais lire les données d'une autre** : voir T1. Les trois identifiants exposés aux clientes (rendez-vous) sont filtrés par email ; les notes de séance et les photos n'ont aucune route côté cliente | `be/booking/AppointmentRepository.java:34-35`, `be/booking/BookingRequests.java:104`, `:116` | CONFIRMÉ | — | — |
| B4 | **Sous `/api/admin/**`, aucun `{id}` n'est contrôlé** : tout est chargé par clé primaire brute. Cohérent avec une praticienne unique, mais il n'y a **aucune seconde couche** derrière le matcher de chemin : si R8 tombait, tout tomberait | `be/booking/Agenda.java:69`, `:74`, `146-147` ; `be/booking/ClientFiles.java:127-129` ; `be/billing/Bills.java:79-87` ; `be/booking/ClosureAdministration.java:35-41` | CONFIRMÉ | Confort (aujourd'hui) | M (sécurité de méthode) |
| B5 | Seule exception, bien faite : la suppression d'une note privée vérifie la paire (note, cliente), donc une note d'une autre fiche n'est pas trouvée | `be/booking/ClientNoteRepository.java:15-16`, `be/booking/ClientFiles.java:67-71` | CONFIRMÉ | — | — |
| B6 | La photo a pour clé primaire l'identifiant du rendez-vous : `{id}` sert donc de deux clés, et les portraits sont énumérables par tout compte du personnel | `V9:53-54`, `be/booking/Agenda.java:72-75` | CONFIRMÉ | Confort | — |
| B7 | **Aucune route publique n'expose de donnée privée** : les trois vues publiques ne portent que le catalogue, des créneaux et les mentions légales | `be/catalog/OfferingView.java:5-15`, `be/booking/TimeSlot.java:6`, `be/legal/LegalIdentity.java:11-12` | CONFIRMÉ | — | — |
| B8 | Les créneaux étant publics, l'occupation et les fermetures de l'agenda se lisent sans compte. Inhérent à une réservation en ligne, à connaître | `be/booking/SlotController.java:25`, `be/shared/security/SecurityConfiguration.java:41` | CONFIRMÉ | Confort | — |
| B9 | `/api/offerings/**` n'est ouvert **que** pour GET : les autres méthodes retombent sur R10 puis sur les règles d'administration. Bien vu | `be/shared/security/SecurityConfiguration.java:41` | CONFIRMÉ | — | — |
| B10 | **Le `permitAll` sur springdoc est inconditionnel dans le code** : il n'est fermé que par la passerelle, qui ne relaie pas ces chemins, et par deux variables d'environnement. Un futur `location /v3/`, un accès direct au conteneur ou un démarrage sans ces variables publierait toute la carte de l'API d'administration | `be/shared/security/SecurityConfiguration.java:40` contre `deploy/nginx.conf:17`, `:38` et `deploy/compose.yaml:57-58` | CONFIRMÉ | Important (défense en profondeur ; **pas** une exposition actuelle) | S |
| B11 | Même remarque pour `/actuator/health` et `/actuator/info` : `permitAll` dans le code, fermés seulement par la passerelle. L'exposition est réduite (`show-details` reste à `never` par défaut) | `:39`, `application.yaml:59-63` | CONFIRMÉ | Confort | S |
| B12 | `ServletUriComponentsBuilder.fromCurrentRequest()` reflète l'en-tête `Host` dans l'en-tête `Location`, et la passerelle transmet le `Host` brut au backend. Sans conséquence ici : la page unique ignore ce `Location`, et les liens magiques viennent de la configuration (T9) | `be/catalog/OfferingAdminController.java:40`, `be/booking/ClosureAdminController.java:33`, `deploy/nginx.conf:19` | CONFIRMÉ | Confort | S |
| B13 | Côté frontend, tous les écrans `admin/*` portent `staffGuard`, sauf la page du second facteur elle-même, qui porte `authGuard` — c'est volontaire et correct | `fe/app.routes.ts:68`, `:75`, `:82`, `:89`, `:96`, `:103`, `:109`, `:115`, `:121`, `:127`, `:133`, `:139`, `:145` ; `fe/core/auth/auth-guard.ts:14-26` | CONFIRMÉ | — | — |
| B14 | Les gardes du frontend ne sont que du confort d'affichage : la vraie décision est côté serveur, et `hasBothFactors` du frontend reflète exactement la règle du backend | `fe/core/auth/current-user.ts`, `fe/core/auth/auth-guard.ts:25` contre `be/shared/security/SecurityConfiguration.java:28-30` | CONFIRMÉ | — | — |
| B15 | Un membre du personnel peut aussi agir comme cliente sur `/api/bookings/**` (R10 n'exige qu'un compte). Sans conséquence : il n'accède qu'à ses propres données | `be/shared/security/SecurityConfiguration.java:48` | CONFIRMÉ | — | — |
| B16 | `/api/bookings/profile` renvoie à la cliente le marquage « de confiance » que la praticienne lui a posé. Voulu — le parcours en a besoin pour proposer le présentiel (spec 3.4) — et sans gravité, le droit d'accès couvrant cette information | `be/booking/ClientProfile.java:7-8`, `be/booking/Client.java:184-186` | CONFIRMÉ | — | — |

---

## 5. Volet C — Données personnelles et RGPD

### C.1 — Inventaire des données collectées

| Donnée | Où elle vit | Finalité | Base légale | Durée réelle dans le code |
| --- | --- | --- | --- | --- |
| Adresse électronique | `user_account.email` (`V2`), `client.email` (`V9:4`), `bill.client_email` (`V14`), `one_time_tokens.username` (`V3:6`), `spring_session.principal_name` (`V5:11`) | identifiant de connexion, envoi des messages | exécution du contrat | tant que le compte existe ; retirée des notes à l'effacement (`be/billing/BillRepository.java:29-31`) |
| Prénom, nom | `client.first_name`, `client.last_name` (`V9:5-6`), `bill.client_name` (`V14`) | reconnaître et joindre la cliente ; mention obligatoire sur la note | contrat ; obligation légale pour la note | remplacés par « Compte / supprimé » à l'effacement (`be/booking/Client.java:136-137`) ; **conservés sur la note 10 ans** |
| Date de naissance | `client.birth_date` (`V9:7`) | vérifier la majorité | obligation légale (interdiction aux mineurs) | effacée à l'effacement (`be/booking/Client.java:138`, `V15:7-8`) |
| Téléphone | `client.phone` (`V9:8`) | joindre la cliente ; tenir la liste noire | contrat ; intérêt légitime pour le blocage | remplacé par un numéro neutre à l'effacement, **sauf si la cliente est bloquée** (`be/booking/Client.java:21`, `141-143`) |
| Ville | `client.city` (`V9:9`) | facultative | contrat | effacée (`be/booking/Client.java:139`) |
| Motif de la réservation | `appointment.reason` (`V9:31`) | préparer la séance. **Peut révéler la santé ou des convictions** | consentement explicite (`V11:11-12`) | remplacé par un marqueur à l'effacement (`be/booking/Appointment.java:242`) |
| Photo portrait | `appointment_photo.data` (`V9:53-58`) | préparer une séance de voyance. **Donnée biométrique de fait, et porteuse d'EXIF (S8)** | consentement explicite | **supprimée 7 jours après la séance**, purge nocturne (`be/booking/BookingSchedule.java:33-39`, `be/booking/BookingRules.java:22`) |
| Adresse de la séance | `appointment.address` (`V9:32`), **`bill.service_place` (`V14`)** | venir chez la cliente | contrat ; obligation légale pour le lieu sur la note | remplacée à l'effacement dans `appointment` ; **conservée 10 ans sur la note (S7)** |
| Pseudo Messenger | `appointment.messenger_name` (`V9:33`) | séance en visio | contrat | remplacé à l'effacement (`be/booking/Appointment.java:244`) |
| Notes privées de la praticienne | `client_note.text` (`V13:10-17`) | suivi d'une séance à l'autre | intérêt légitime | supprimées à l'effacement (`be/booking/ClientErasure.java:57`) |
| Marquage « de confiance », blocage, manquements | `client.trusted`, `client.blocked`, `client.late_cancellations` (`V9:10-11`, `V13:4`) | éligibilité au présentiel ; protéger l'agenda | intérêt légitime | blocage et téléphone conservés au-delà de l'effacement, jusqu'au déblocage |
| Consentement | `appointment.consented_at` (`V9:34`) | preuve | — | avec le rendez-vous. **Écart déjà traité par l'audit n° 1, volet C** |
| Notes de séance et encaissements | `bill` (`V14`) | comptabilité | obligation légale | **10 ans** |
| Dates de connexion | `user_account.last_login_at` (`V4`) | — | intérêt légitime | supprimées avec le compte |
| Sessions HTTP | `spring_session`, `spring_session_attributes` (`V5`) | garder la connexion | contrat | 1 h d'inactivité (`application.yaml:55`) |
| **Adresses IP** | journaux d'accès des deux nginx, journaux Docker, compteurs de limitation en mémoire | diagnostic ; limitation d'abus | intérêt légitime | **non décidée** (voir C3) |

### C.2 — Constats

| # | Constat | Preuve | État | Gravité | Effort |
| --- | --- | --- | --- | --- | --- |
| C1 | **Les données sensibles sont bien protégées là où ça compte** : le motif ne quitte jamais le site, la photo n'est servie qu'au personnel à double facteur, sans cache, et elle est purgée 7 jours après la séance. C'est le point le plus solide du dispositif | `be/notification/Messages.java:14-16` ; `be/booking/AgendaController.java:48-57` ; `be/booking/BookingSchedule.java:33-39` ; test `booking/AccountErasureFlowTests.java` | CONFIRMÉ | — | — |
| C2 | **L'adresse du domicile survit dix ans sur la note**, et aucun texte ne le dit. Voir S7 | `be/billing/Bill.java:146-152`, `:38-39`, `be/billing/ErasedClientBills.java:22-25` | CONFIRMÉ | Important | S-M |
| C3 | **Les journaux nginx enregistrent les adresses IP** des deux couches, sans durée de conservation décidée et sans un mot dans la politique — qui affirme même « elle n'est pas enregistrée ». L'audit n° 1 (D6, B-E3) demande déjà de retirer ce bloc, qui décrit une mesure d'audience inexistante ; le constat ici est **différent** : même après ce retrait, rien ne dira ce que deviennent les journaux. `access_log off` n'est posé que sur `/contenus/` | `fe/public/legal/confidentialite-page.html:52-54` ; `deploy/nginx.conf:35` (seul `access_log off`) ; `deploy/compose.yaml:5-9` (journaux Docker bornés à 3 × 10 Mo) | CONFIRMÉ | Important | S |
| C4 | **L'EXIF n'est pas retiré des photos.** Voir S8 | `be/booking/Photo.java:14-21` | CONFIRMÉ | Important | S |
| C5 | **Aucun mécanisme de retrait du consentement**, alors que la politique le promet. Voir S9 | `fe/public/legal/confidentialite-page.html:34` contre `be/booking/AccountErasureController.java:26` | CONFIRMÉ | Important | S (texte) / M (code) |
| C6 | **L'effacement est complet et bien construit** : motif, adresse et pseudo remplacés, photos et notes supprimées, connexion, jetons et sessions détruits. Et il est garanti : l'événement attend en base jusqu'à ce que l'écouteur réussisse, donc une connexion n'est jamais laissée derrière | `be/booking/ClientErasure.java:47-61`, `be/booking/Appointment.java:241-245`, `be/booking/Client.java:133-144`, `be/identity/ClientAccountDeletion.java:14-16`, `32-39` | CONFIRMÉ | — | — |
| C7 | **Rectification : absente du logiciel.** Seule la praticienne corrige une fiche ; la cliente peut seulement réécrire son nom et son téléphone en réservant de nouveau | `be/booking/ClientAdminController.java:40-43`, `be/booking/BookingRequests.java:86` | CONFIRMÉ | Important | S (procédure écrite) / M (écran) |
| C8 | **Portabilité : absente.** Aucun export côté cliente. Le droit d'accès est servi partiellement par deux routes, qui ne montrent ni le motif, ni les notes de séance, ni les notes privées | `be/booking/BookingController.java:34-45`, `be/booking/AppointmentView.java:11-13` (pas de `reason`) | CONFIRMÉ | Important | S (procédure) / M (export) |
| C9 | **Limitation et opposition : absentes.** Aucun état intermédiaire entre « compte actif » et « compte effacé » | absence dans `be/booking/Client.java` et `V15` | CONFIRMÉ | Confort | M |
| C10 | **Sous-traitants : Google / Gmail n'est pas nommé.** Les emails lui confient nom, prénom, date et heure de séance, nom de la prestation, montant, adresse de séance pour une visite à domicile et pseudo Messenger. Le motif, lui, ne part jamais (C1) | `fe/public/legal/confidentialite-page.html:98` ; `be/notification/BookingNotifications.java:83-89` ; `be/notification/Messages.java:98-121`, `:219-252`, `257-268` | CONFIRMÉ | Important | S |
| C11 | OVH est nommé, et la valeur vient de la configuration, donc modifiable sans toucher au code | `be/legal/LegalProperties.java:28`, `application.yaml:80` | CONFIRMÉ | — | — |
| C12 | Le fournisseur de SMS, non choisi, recevra le numéro de téléphone et le contenu du message : à ajouter à la politique **avant** de le brancher | spec 7, `CLAUDE.md` | CONFIRMÉ (le manque) | Confort | S |
| C13 | **`one_time_tokens.username` garde l'adresse de toute personne ayant demandé un lien**, y compris une tierce personne visée par l'abus de S2 : c'est une collecte que la politique ne décrit pas. Elle est bien effacée à la suppression de compte | `V3:4-8`, `be/identity/ClientAccountDeletion.java:37` | CONFIRMÉ | Confort | S |
| C14 | La purge des jetons expirés est celle de Spring Security, non lue : si elle n'avait pas lieu, ces adresses s'accumuleraient | `be/identity/MagicLinkConfiguration.java:18-20` | SUPPOSÉ | Confort | S (vérifier) |
| C15 | **Aucune donnée de cliente dans les journaux applicatifs** (T17). Seules les adresses du personnel y sont écrites au démarrage | `be/identity/StaffBootstrap.java:41`, `44` | CONFIRMÉ | Confort | S |
| C16 | « Le site n'est accessible qu'en HTTPS » est faux pour l'accès par tunnel SSH, en clair. Sans portée réelle tant que la production n'existe pas, mais la phrase devra être exacte | `fe/public/legal/confidentialite-page.html:144` contre `deploy/.env.example:8` | CONFIRMÉ | Confort | S |
| C17 | « Deux cookies techniques seulement » est juste : `SESSION` et `XSRF-TOKEN`. La porte de recette en ajoute deux, non mentionnés — normal, ils disparaissent à l'ouverture | `be/shared/security/SecurityConfiguration.java:50-51` ; `deploy/host/nginx-auclairdeso.conf:43-44` | CONFIRMÉ | — | — |
| C18 | Les sauvegardes de PostgreSQL porteraient les photos et les motifs. Rien dans le dépôt ne décrit de sauvegarde, ni son chiffrement : la tranche 7 l'a au programme | `deploy/compose.yaml:19-20` (volume `pgdata`, sans plus), spec 12 tranche 7 | CONFIRMÉ (le manque) | Important | M |

### C.3 — Comment les données sensibles sont protégées, en une phrase par barrière

Le motif de consultation et la photo sont les deux données qui peuvent révéler la santé ou des
convictions. Cinq barrières les protègent, et toutes tiennent :

1. **Consentement explicite et séparé** à la réservation — c'est le point le plus solide du
   dispositif de consentement (audit n° 1, C.3).
2. **Elles ne sortent jamais par email** : le module de notification n'y touche pas, et son
   commentaire d'en-tête en fait une règle (`be/notification/Messages.java:14-16`).
3. **Elles ne sont jamais servies à une route publique ni à une cliente** : aucune vue côté cliente
   ne porte `reason`, et la photo n'a qu'une route, sous `/api/admin/**` à double facteur.
4. **La photo n'est jamais sur le disque** ni dans une zone publique : elle est en base, avec
   `noStore` à la lecture (`V9:53-58`, `be/booking/AgendaController.java:54`).
5. **La photo s'efface d'elle-même** 7 jours après la séance, par une purge nocturne, sans
   intervention.

Les deux manques sont ailleurs : l'EXIF que personne ne retire (S8), et le fait qu'une base ou une
sauvegarde volée les rendrait toutes deux en clair (C18).

---

## 6. Volet D — Entrées et injections

| # | Constat | Preuve | État | Gravité | Effort |
| --- | --- | --- | --- | --- | --- |
| D1 | **Validation serveur sur les dix corps de requête du dépôt, sans exception** : chaque `@RequestBody` et le `@RequestPart` JSON portent `@Valid`, et chaque champ porte ses bornes | `be/identity/AccountPasswordController.java:23`, `be/billing/BillAdminController.java:41`, `be/booking/BookingController.java:27`, `be/booking/ClientAdminController.java:41`, `:67`, `be/booking/AgendaController.java:61`, `be/booking/ClosureAdminController.java:31`, `be/catalog/OfferingAdminController.java:38`, `:48` | CONFIRMÉ | — | — |
| D2 | **Aucune injection SQL possible** (T2). Tout passe par du JPQL à paramètres nommés ou par `?` ; les deux seules requêtes natives sont paramétrées | `be/identity/ClientAccountDeletion.java:37`, `be/billing/BillNumbers.java:25-26` | CONFIRMÉ | — | — |
| D3 | **Aucun puits XSS** (T3). Angular échappe par interpolation, et les seules liaisons d'URL (`mailto:`, `tel:`, `[src]`) viennent de la configuration ou d'un chemin construit, jamais d'une saisie de cliente | `fe/public/legal/confidentialite-page.html:14`, `fe/admin/agenda/appointment-detail-page.ts:25`, `fe/admin/billing/receipts-page.ts:34` | CONFIRMÉ | — | — |
| D4 | **Aucun rejet des caractères de contrôle dans les noms et les textes libres** : `\r\n` passe `@NotBlank` et `@Size`, et se retrouve dans l'objet d'un email, sur la note et dans l'administration. L'injection d'en-tête d'email est bloquée par l'encodage de JavaMail, **pas par le code** : la défense est hors du dépôt et personne ne l'a choisie | `be/booking/ClientDetails.java:13-18`, `be/booking/ManualClient.java:11-16`, `be/booking/BookingRequest.java:26-34`, `be/booking/NoteRequest.java:8-10` ; usage dans l'objet : `be/notification/Messages.java:39`, `297-299` → `be/notification/BookingNotifications.java:87` | CONFIRMÉ (absence de filtre) / SUPPOSÉ (défense de JavaMail) | Important | S |
| D5 | L'adresse de destination, elle, **ne peut pas porter de CRLF** : la normalisation interdit tout caractère blanc avant toute utilisation | `be/identity/EmailAddresses.java:10`, `:23` ; usage en `be/identity/MagicLinkSender.java:44` | CONFIRMÉ | — | — |
| D6 | **Injection CSV traitée** : une cellule qui commence par `=`, `+`, `-`, `@`, une tabulation ou un retour chariot reçoit une apostrophe, et les guillemets sont doublés. Les noms venant des clientes passent bien par ce filtre | `be/billing/ReceiptBookCsv.java:44-50`, `:24-29` ; tests `billing/ReceiptBookCsvTests.java` | CONFIRMÉ | — | — |
| D7 | **Téléversement de photo : type réel vérifié** sur les premiers octets (JPEG, PNG, WebP), taille entre 1 octet et 10 Mo, et le type resservi est celui qu'a reconnu la détection — jamais celui déclaré par le navigateur | `be/booking/Photo.java:14-21`, `23-34`, `be/booking/AgendaController.java:53` | CONFIRMÉ | — | — |
| D8 | Mais **aucun ré-encodage** : l'EXIF reste (S8), et un fichier polyglotte (en-tête JPEG valide suivi de HTML) est stocké tel quel. Il serait servi en `image/jpeg`, et le `nosniff` par défaut de Spring Security devrait suffire — ce qui fait reposer la défense sur un en-tête que personne n'a configuré | `be/booking/Photo.java:14-21` ; absence de `.headers()` dans `be/shared/security/SecurityConfiguration.java` | CONFIRMÉ (le manque) / SUPPOSÉ (le `nosniff` par défaut) | Confort | S |
| D9 | `photo.getBytes()` charge tout le fichier en mémoire **avant** la validation de taille | `be/booking/BookingController.java:30` | CONFIRMÉ | Confort | S |
| D10 | Limites de taille : 10 Mo par fichier, 12 Mo par requête multipart, et `client_max_body_size` aligné dans les deux nginx. **Aucune borne globale sur un corps JSON** : la surface anonyme est bornée par les défauts de Tomcat, la surface authentifiée ne l'est que par les `@Size` appliqués après désérialisation | `application.yaml:10-13`, `deploy/nginx.conf:15`, `deploy/host/nginx-auclairdeso.conf:29` | CONFIRMÉ | Confort | S |
| D11 | Messages d'erreur : `server.error.include-message`, `include-stacktrace`, `include-exception` et `include-binding-errors` sont laissés à leurs défauts, donc rien n'est divulgué. Les noms de champs invalides sont émis volontairement, et c'est le bon choix pour un formulaire | `application.yaml` (absence de `server.error.*`), `be/shared/web/ApiExceptionHandler.java:23-28` | CONFIRMÉ | — | — |
| D12 | Deux routes renvoient le slug fourni dans le `detail` du ProblemDetail. Réfléchi mais inoffensif : c'est du JSON, et le frontend l'affiche comme du texte | `be/booking/SlotController.java:30-31`, `be/catalog/OfferingController.java:30-31`, `fe/shared/problem-message.ts:13` | CONFIRMÉ | Confort | S |
| D13 | Le `LIKE` de la recherche cliente accepte les jokers `%` et `_` de la praticienne : la recherche s'élargit, ce n'est pas une injection | `be/booking/ClientRepository.java:34-37` | CONFIRMÉ | Confort | — |
| D14 | Le téléphone est validé par une expression régulière bornée, sans risque de retour sur trace catastrophique | `be/booking/ClientDetails.java:26` | CONFIRMÉ | — | — |

---

## 7. Volet E — Abus

| # | Constat | Preuve | État | Gravité | Effort |
| --- | --- | --- | --- | --- | --- |
| E1 | **`/api/auth/magic-link` est un relais d'email vers n'importe quelle adresse.** Voir S2. Détail du plafond : 3 par adresse toutes les 15 minutes (soit 12 par heure vers une victime choisie) et 20 par adresse IP par heure | `be/identity/MagicLinkRateLimitFilter.java:31-32`, `43-44`, `be/identity/MagicLinkSender.java:42-57` | CONFIRMÉ | **Bloquant avant ouverture** (production) | M |
| E2 | L'épuisement du quota Gmail **arrête toutes les notifications**, pas seulement les liens de connexion : confirmations, refus, expirations, rappels et notes de séance passent par le même expéditeur, et la reprise abandonne après douze essais | `be/notification/BookingNotifications.java:83-89`, `be/notification/FailedNotificationRetries.java:17`, `application.yaml:67` | CONFIRMÉ | **Bloquant avant ouverture** | M |
| E3 | La limitation par IP **fonctionne** : la chaîne `X-Forwarded-For` ajoute au lieu d'écraser, donc une adresse envoyée par le client ne remplace pas la vraie (T10) | `deploy/host/nginx-auclairdeso.conf:60`, `deploy/nginx.conf:20`, `application.yaml:2` | CONFIRMÉ (configuration) / SUPPOSÉ (`RemoteIpValve`) | — | — |
| E4 | Mais elle est contournable par le nombre d'adresses IP : seule la limite par adresse électronique protège une victime désignée, et rien ne plafonne le **total** d'emails envoyés par heure ou par jour | `be/identity/MagicLinkRateLimitFilter.java:31-32` | CONFIRMÉ | Important | S (un plafond global) |
| E5 | Si la limite par IP est atteinte, la limite par adresse n'est pas incrémentée ; une adresse illisible consomme un jeton d'IP et aucun d'adresse. Comportement correct, à connaître | `be/identity/MagicLinkRateLimitFilter.java:43-44` | CONFIRMÉ | — | — |
| E6 | **Aucune limitation sur `/api/auth/password`.** Voir S5 | `be/identity/MagicLinkRateLimitFilter.java:36` | CONFIRMÉ | Important | S |
| E7 | Aucune limitation sur `/api/auth/magic-link/verify` : seule l'entropie du jeton protège | idem | CONFIRMÉ | Confort | S |
| E8 | **Aucun `limit_req` ni `limit_conn`** dans les deux configurations nginx : rien ne freine au niveau du proxy, où ce serait le moins coûteux à mettre | `deploy/nginx.conf`, `deploy/host/nginx-auclairdeso.conf` (absence) | CONFIRMÉ | Important | S |
| E9 | fail2ban ne surveille que la porte Basic de la recette, pas l'application | `deploy/host/fail2ban-nginx-http-auth.local:2-9` | CONFIRMÉ | Confort | S |
| E10 | Aucune limitation sur `/api/bookings`, mais la surface est étroite : il faut un compte, et la règle « une seule réservation à venir » refuse la deuxième. Reste qu'un envoi répété de photos de 10 Mo n'est freiné par rien | `be/booking/BookingRequests.java:77-81`, `be/booking/BookingController.java:30` | CONFIRMÉ | Confort | S |
| E11 | Compteurs en mémoire : remis à zéro à chaque redéploiement, non partagés entre instances | `be/identity/AttemptLimiter.java:23-27` | CONFIRMÉ | Confort | — |
| E12 | Aucun CAPTCHA, aucune preuve de travail, nulle part. C'est une décision à prendre, pas un oubli à corriger mécaniquement : un CAPTCHA ajoute un sous-traitant et un obstacle d'accessibilité | absence dans tout le dépôt | CONFIRMÉ | décision | — |
| E13 | La mémoire du backend est plafonnée (768 Mo, `MaxRAMPercentage=75`), donc un abus sature le conteneur plutôt que le VPS, et Docker le redémarre | `deploy/compose.yaml:59-60`, `:38` | CONFIRMÉ | — | — |

---

## 8. Volet F — Dépendances et configuration

### F.1 — Versions, vérifiées et non supposées

Les versions résolues ont été lues dans le dépôt Maven local, pas seulement dans le `pom.xml` :

| Composant | Version | Preuve |
| --- | --- | --- |
| Spring Boot | 4.1.1 | `backend/pom.xml:9` |
| Spring Security | 7.1.1 | résolue (`spring-security-core`) |
| Hibernate ORM | 7.4.5.Final | résolue (`hibernate-core`) |
| **tomcat-embed-core** | **11.0.24** | résolue — voir F2 |
| Spring Modulith | 2.1.1 | `backend/pom.xml:36`, `157-167` |
| springdoc | 3.1.0 | `backend/pom.xml:37`, `70-74` (seule version épinglée à la main) |
| Java | 25 | `backend/pom.xml:35`, `.github/workflows/ci.yml:28` |
| Angular | 22.2 | `frontend/package.json:15-22` |
| express | 5.2.1 | résolue dans `frontend/package-lock.json` |
| Node | 24 | `.github/workflows/ci.yml:46`, `frontend/Dockerfile:4`, `:13` |
| PostgreSQL | 17-alpine | `deploy/compose.yaml:13` |

### F.2 — Constats

| # | Constat | Preuve | État | Gravité | Effort |
| --- | --- | --- | --- | --- | --- |
| F1 | **Aucun scan de dépendances** : pas de `dependabot.yml`, pas d'OWASP dependency-check, pas de `npm audit` ni d'`audit-ci`, pas de CodeQL, pas d'analyse de secrets. `.github/` ne contient que `workflows/ci.yml` | `.github/workflows/ci.yml` (seul fichier) | CONFIRMÉ | Important | S |
| F2 | **tomcat-embed-core 11.0.24** tombe dans la plage attribuée à CVE-2026-86350, corrigée en 11.0.26. La condition serait que du HTTP/2 atteigne Tomcat ; nginx lui parle en HTTP/1.1, donc elle n'est probablement pas réunie. Version CONFIRMÉE, avis **non vérifié** | dépôt Maven local ; `deploy/nginx.conf:18` | CONFIRMÉ (version) / SUPPOSÉ (l'avis) | Important | S (vérifier, puis monter de version) |
| F3 | **Aucun en-tête de sécurité, nulle part.** Voir S1. Les sept `add_header` du dépôt sont tous dans la porte de recette | recherche exhaustive ; `deploy/host/nginx-auclairdeso.conf:26`, `43-46`, `53-54` | CONFIRMÉ | **Bloquant avant ouverture** | S |
| F4 | Spring Security pose quand même ses défauts (`nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store`, HSTS si la requête est vue en HTTPS) — **mais seulement sur `/api/**`**, donc sur du JSON, là où ils servent le moins | absence de `.headers()` dans `be/shared/security/SecurityConfiguration.java` | SUPPOSÉ (jeu d'en-têtes par défaut) | — | — |
| F5 | **Piège à connaître avant de corriger S1** : en nginx, `add_header` ne s'hérite pas quand un bloc imbriqué en déclare un. Un en-tête posé au niveau `server` de la couche hôte serait absent des 302 de la porte et de `@recette_open`, qui déclarent déjà les leurs — la configuration actuelle le montre, puisqu'elle doit répéter `X-Robots-Tag` | `deploy/host/nginx-auclairdeso.conf:26` contre `:46` et `:53-54` | CONFIRMÉ | — | — |
| F6 | **`/contenus/` sert des fichiers déposés à la main, même origine, type deviné de l'extension.** Voir S6 | `deploy/nginx.conf:30-36`, `deploy/compose.yaml:84` | CONFIRMÉ | Important | S |
| F7 | **Actuator réduit à `health` et `info`**, et `show-details` reste à `never` par défaut | `application.yaml:59-63` | CONFIRMÉ | — | — |
| F8 | **Ni actuator ni Swagger ne sont atteignables de l'extérieur** (T11) : la passerelle ne relaie au backend que `/api/`. Mais c'est la passerelle qui ferme, pas la configuration de sécurité (B10, B11) | `deploy/nginx.conf:17`, `:38`, `deploy/compose.yaml:57-58` | CONFIRMÉ | — | — |
| F9 | **Les défauts SMTP sont sans authentification ni TLS.** Sans conséquence en recette, où le serveur est Mailpit en réseau interne. Mais au passage en production, une variable oubliée dans `deploy/.env` dégraderait l'envoi **en silence** : le mot de passe d'application Gmail partirait en clair, ou l'envoi échouerait sans que rien ne le dise | `application.yaml:42-44`, `deploy/compose.yaml:46-47` ; `grep -c` : `MAIL_SMTP_AUTH` et `MAIL_STARTTLS` absents de `deploy/.env`, où ils sont inutiles aujourd'hui | CONFIRMÉ | Important (pour la bascule) | S |
| F10 | **`deploy/.env` n'a pas les trois `BILLING_*` obligatoires.** Voir S3 | `deploy/compose.yaml:51-53`, `deploy/deploy.sh:3`, `:9` ; `grep -c` = 0 pour les trois | CONFIRMÉ | **Bloquant** (déploiement) | S |
| F11 | Le profil de production est le profil par défaut : le jar construit par le Dockerfile n'active aucun profil, et `application-dev.yaml` (SQL en debug) ne peut donc pas s'appliquer. Le `<profile>dev</profile>` du `pom.xml` ne vaut que pour `spring-boot:run` — c'est un piège, pas un défaut | `backend/Dockerfile:10`, `:19` ; `backend/pom.xml:174-178` ; `application-dev.yaml:5-7` | CONFIRMÉ | Confort | S (un commentaire) |
| F12 | `ddl-auto: validate`, `open-in-view: false`, Flyway sur `classpath:db/migration` seul en production : trois bons réglages | `application.yaml:21`, `:23`, `:29-30` | CONFIRMÉ | — | — |
| F13 | Les propriétés de configuration sont validées au démarrage (`@Validated`, `@NotBlank`) : une identité légale vide arrête l'application au lieu de publier une page fausse | `be/shared/AppProperties.java:12-16`, `be/legal/LegalProperties.java:21-31`, `be/billing/BillingProperties.java` | CONFIRMÉ | — | — |
| F14 | **`NG_ALLOWED_HOSTS` est de la configuration morte** : `compose.yaml` la passe, `angular.json` a `"allowedHosts": []` en dur, et rien dans `frontend/src` ne la lit. Le serveur de rendu de production ne valide donc aucun en-tête `Host` — sans conséquence ici (B12), mais la variable fait croire à une protection qui n'existe pas | `deploy/compose.yaml:70`, `frontend/angular.json:34`, `deploy/.env.example:10` ; aucune occurrence dans `frontend/src` | CONFIRMÉ | Confort | S |
| F15 | **Images Docker en étiquettes flottantes**, et `axllent/mailpit` sans étiquette du tout, donc `latest` : un redéploiement peut changer de version sans que rien ne le dise | `deploy/compose.yaml:13`, `:29`, `:75` ; `backend/Dockerfile:4`, `:13` ; `frontend/Dockerfile:4`, `:13` | CONFIRMÉ | Confort | S |
| F16 | Aucun `HEALTHCHECK` dans les deux Dockerfile ; seul PostgreSQL en a un dans `compose.yaml` | `backend/Dockerfile`, `frontend/Dockerfile` (absence), `deploy/compose.yaml:21-25` | CONFIRMÉ | Confort | S |
| F17 | **`.gitignore` ne couvre ni `*.swp`, ni `*.bak`, ni `*.orig`, ni `<nom>.env`** : `prod.env` et `deploy/secrets.env` ne seraient **pas** ignorés. En revanche `deploy/.env` et `deploy/.env.swp` le sont bien | `git check-ignore -v` sur chacun ; `.gitignore:19-23`, `:34` | CONFIRMÉ | Confort | S |
| F18 | `@types/node` en version 22 alors que tout tourne sur Node 24 | `frontend/package.json:35` contre `.github/workflows/ci.yml:46` | CONFIRMÉ | Confort | S |
| F19 | `packageManager` est déclaré mais non appliqué, la CI n'utilisant pas Corepack | `frontend/package.json:13` contre `.github/workflows/ci.yml:44-51` | CONFIRMÉ | Confort | S |
| F20 | La CI a les bons droits (`contents: read`) et épingle ses actions par version majeure | `.github/workflows/ci.yml:8-9`, `:23`, `:25`, `:42`, `:44` | CONFIRMÉ | — | — |
| F21 | Aucun secret n'a jamais été commité, et `deploy/.env` est en mode `0600` (T15, T16) | voir T15 | CONFIRMÉ | — | — |
| F22 | **Rien dans le dépôt ne décrit de sauvegarde de PostgreSQL**, alors que la base porte les photos, les motifs et les notes privées. C'est au programme de la tranche 7 | `deploy/compose.yaml:19-20`, `:87-88` ; spec 12 | CONFIRMÉ | Important | M |

---

## 9. Questions à poser et décisions à prendre

1. **Jeton de lien magique** : le passer en fragment d'URL (`#token=`) plutôt qu'en paramètre de
   requête ? C'est le correctif propre de S4, et il touche le backend et le frontend.
2. **Lieu sur la note d'une séance à domicile** : adresse complète, ville seule, ou « Au domicile de
   la cliente » sans plus ? L'arrêté exige le lieu, pas nécessairement l'adresse exacte. Décision
   métier, à arbitrer avec la question 8 de l'audit n° 1. (S7)
3. **Protection contre l'abus du lien magique** : plafond global d'envoi par heure et par jour,
   preuve de travail, CAPTCHA, ou limitation aux adresses déjà connues ? Le plafond global est le
   moins intrusif et protège le quota Gmail, qui est le vrai enjeu. (S2, E4)
4. **Contenu de la CSP** : une politique stricte demande d'inventorier les polices Google
   (`@fontsource`), le dossier `/contenus/` et les éventuelles images externes. À faire une fois,
   proprement. (S1)
5. **Durée de conservation des journaux nginx**, et ce qu'on en écrit dans la politique de
   confidentialité. (C3)
6. **Retrait du consentement aux données sensibles** : on l'implémente, ou on corrige la phrase de
   la politique ? Corriger la phrase est honnête si la procédure par email existe et est écrite. (S9, C5)
7. **Nomme-t-on Google / Gmail** comme sous-traitant dans la politique, et quelle mention prépare
   l'arrivée du fournisseur de SMS ? (C10, C12)
8. **Qui remplit les trois `BILLING_*` de `deploy/.env`, et quand ?** La recette ne redémarrera pas
   avant. Lié à la domiciliation (S3, et S2 de l'audit n° 1).
9. **Dependabot et un scan de dépendances en CI** : on les ajoute maintenant, ou à la tranche 7 ?
   Maintenant coûte une demi-heure et évite de découvrir dix versions de retard plus tard. (S10, F1)
10. **Portabilité et rectification** : livrées avant l'ouverture, ou traitées à la main par email
    avec une procédure écrite ? La seconde option est acceptable, mais elle doit être écrite
    quelque part. (C7, C8)
11. **Sauvegardes de PostgreSQL** : où, à quelle fréquence, chiffrées comment, et testées par quelle
    restauration ? La base porte les photos et les motifs. (F22, C18)
12. **Verrouillage de compte après N échecs** sur le mot de passe du personnel : on le met, ou on se
    contente d'une limitation de débit ? Avec deux comptes seulement, un verrouillage est risqué
    (déni de service sur la praticienne) ; une limitation par IP est probablement suffisante. (S5)

---

## 10. Ce que je n'ai pas pu vérifier, et pourquoi

- **`deploy/.env` n'a pas été ouvert**, par consigne. Seule la présence des variables est connue,
  par un `grep -c` qui ne renvoie qu'un nombre. Ne sont donc **pas** auditées : la force de
  `DB_PASSWORD`, la valeur de `PUBLIC_URL` (dont dépendent tous les liens magiques), les adresses de
  `ADMINS` et `PRACTITIONERS` (donc qui a réellement les rôles), `MEDIA_DIR` et
  `LEGAL_CONTACT_EMAIL`.
- **Le secret de la porte de recette** (`/etc/nginx/auclairdeso-recette.conf`) et
  `/etc/nginx/auclairdeso.htpasswd` vivent sur le VPS, hors du dépôt : ni leur valeur, ni leur
  force, ni leurs droits d'accès ne sont vérifiables ici. Le modèle est en revanche correct et
  avertit du risque (`deploy/host/nginx-auclairdeso-secret.conf.example:3`).
- **Aucune requête n'a été envoyée à aucun serveur.** Ne sont donc pas observés, seulement déduits
  des fichiers : les en-têtes réellement émis, l'attribut `Secure` réel des cookies, le contenu et
  la rotation des journaux nginx, la version et les suites TLS, la configuration effective de
  PostgreSQL, et le comportement réel du scénario d'abus du lien magique.
- **Aucun test n'a été exécuté** (`./mvnw verify`, `npx ng test`) : l'audit est en lecture seule. Ce
  que je dis des tests vient de leur lecture. Un test peut donc exister et échouer, ou être
  désactivé, sans que je le sache.
- **Les CVE ne sont pas vérifiées de façon fiable.** Une seule recherche a été faite, sur Tomcat, et
  son résultat est marqué SUPPOSÉ (F2) : il reste à confirmer sur spring.io/security et la NVD. Pour
  tout le reste des dépendances — Spring, Hibernate, Angular, express, les images Docker — je n'ai
  rien vérifié, et le projet n'a aucun outil pour le faire. C'est précisément le constat S10.
- **La configuration du VPS hors dépôt n'est pas auditée** : pare-feu, mises à jour du système,
  accès SSH et clés autorisées, rotation des journaux, droits des fichiers hors `deploy/`, et l'état
  réel de fail2ban (le fichier du dépôt est un modèle à installer à la main).
- **Rien n'a été vérifié dans un navigateur** : pas d'essai réel d'encadrement en iframe, pas de
  lecture des cookies émis, pas de contrôle du `Referer` effectivement envoyé depuis la page de
  vérification.
- **Le comportement interne de trois bibliothèques n'a pas été lu**, d'où les mentions SUPPOSÉ :
  l'usage unique et la purge des jetons par `JdbcOneTimeTokenService` (A2, C14), l'encodage des
  en-têtes par JavaMail qui bloque l'injection (D4), et le jeu d'en-têtes par défaut de Spring
  Security (F4). Ce sont trois défenses réelles mais non choisies : une montée de version pourrait
  les changer sans que rien dans le dépôt ne s'en aperçoive.
- **Le contenu réel de la base en recette est inconnu** : combien de comptes, quels rôles, quelles
  photos encore présentes, et si la purge nocturne a bien tourné. L'audit n° 1 faisait la même
  réserve pour le catalogue.
