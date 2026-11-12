# CLAUDE.md — Au clair de So

Site vitrine + prise de rendez-vous en ligne pour une médium en micro-entreprise.
Monorepo : `backend/` (Spring Boot), `frontend/` (Angular SSR), `deploy/` (Docker Compose, nginx, script de déploiement).
Le document de cadrage fait référence pour le périmètre, l'architecture et la roadmap.

## Mode de travail
- Tony apprend en recopiant le code dans IntelliJ. Pour « explique » ou « génère » : fichiers COMPLETS (chemin + contenu intégral) et le pourquoi du choix. Ne modifier le dépôt directement que si la demande le dit.
- Une étape à la fois, vérifiable (test, curl, écran). Finir par un message Conventional Commits (feat, fix, refactor, test, chore, docs).
- Réponses en français ; code, identifiants et commits en anglais.
- Toute nouvelle dépendance est signalée et justifiée.

## Stack
- Java 25, Spring Boot 4.1.x, Spring Modulith 2.1, Spring Security 7.1, Spring Data JPA (Hibernate 7), PostgreSQL 17, Flyway, Maven wrapper.
- Angular 22 : standalone, zoneless, signals, Signal Forms, httpResource, @Service, OnPush par défaut, @angular/ssr, Angular CDK/Aria, Tailwind CSS, Vitest, Playwright.
- Intégrations : envoi d'e-mails par le SMTP de Gmail (mot de passe d'application, en attendant un nom de domaine). Pas de paiement en ligne ni de synchronisation d'agenda (spec, 13) ; fournisseur de SMS à choisir.
- Infra : Docker Compose sur VPS OVH, nginx existant comme reverse proxy, GitHub Actions.

## Commandes
- Backend (dans `backend/`) : `./mvnw verify` (Linux/CI) ou `.\mvnw.cmd verify` (Windows), `./mvnw spring-boot:run`
  - Un seul test : `./mvnw test -Dtest=ModularityTests` (ou `Classe#méthode`)
- Frontend (dans `frontend/`) : `npm ci`, `ng serve` (proxy API : `proxy.conf.json`), `ng build` (SSR + prérendu)
  - Tests : `npx ng test --watch=false` (comme la CI) ; un seul fichier : `npx ng test --watch=false --include=src/app/booking/booking-form-model.spec.ts`
- CI (`.github/workflows/ci.yml`) : `./mvnw -B verify` (Java 25) ; `ng test` puis `ng build` (Node 24).

## Architecture backend
- Package racine `fr.auclairdeso`, un sous-package par module : identity, catalog, booking, billing, legal, notification, shared. La spec (`docs/specification.md`, 12) ne prévoit aucun module de plus : le planning (plages, fermetures, créneaux) vit dans `booking`, le paiement en ligne et la synchronisation Google Agenda sont hors périmètre (13), et le contenu de la vitrine est dans le frontend, les médias dans `MEDIA_DIR` (décision 16). Les tranches à venir (SMS, question ciblée, statistiques) s'ajoutent aux modules existants.
  - `booking` : rendez-vous, fiches clients, notes privées, fermetures, et l'effacement de compte (`AccountErasureController`, `ClientErasure`, événement `ClientErased`). Il porte aussi les tâches planifiées (`BookingSchedule`), dont la purge nocturne des photos après la séance.
  - `billing` : une note par séance réalisée (`Bill`, numérotation `BillNumbers`), encaissements, et livre des recettes (`ReceiptBook`, export CSV). Identité de la praticienne via `auclairdeso.billing.*` (`BILLING_*` dans `deploy/.env`).
  - `catalog` : prestations ; chacune porte une vidéo facultative (`videoPath`, nom de fichier simple sous `/contenus/`, et `videoTranscript`), validée dans `OfferingDraft` et par une contrainte SQL (V16). Le fichier vit dans `MEDIA_DIR`, jamais dans Git.
  - `legal` : alimente les pages légales à partir de `auclairdeso.legal.*` (`LEGAL_*` dans `deploy/.env`) ; le médiateur reste vide tant qu'il n'est pas choisi.
  - Effacement de compte : `booking` publie `ClientErased` ; `identity` supprime la connexion et les sessions, `billing` retire l'e-mail des notes. Les notes et le livre des recettes sont conservés 10 ans, avec le nom (arrêté 83-50/A).
- Les migrations sont dans `backend/src/main/resources/db/migration` (`V<n>__description.sql`) ; prendre le numéro suivant.
- Un module n'utilise que l'API publique d'un autre (package racine du module) ou ses événements ; `ApplicationModules.of(...).verify()` en test.
- Records pour DTO et value objects ; erreurs HTTP en ProblemDetail.
- Dates en `Instant` / `timestamptz` ; conversion Europe/Paris uniquement à l'affichage.
- Une migration Flyway mergée ne se modifie jamais : on en ajoute une nouvelle.

## Règles non négociables
- Pas de double réservation : contrainte d'exclusion PostgreSQL `appointment_no_overlap` (plage `starts_at`–`occupied_until`) ; seuls les statuts REQUESTED et CONFIRMED gardent le créneau (`AppointmentStatus.HOLDING_SLOT`).
- Consentements horodatés : version des CGV, majorité, demande d'exécution anticipée sous 14 jours.
  - À vérifier (audit) : écart avec `docs/specification.md`, qui ne prévoit que les consentements à la majorité, aux conditions générales et au traitement des données et traite le droit de rétractation de 14 jours dans les conditions générales ; aucune « demande d'exécution anticipée » n'y figure. Règle conservée telle quelle en attendant la décision.
- Aucune donnée carte stockée, aucun secret dans le dépôt (`.env` hors Git).
- Textes du site : ton moderne et sobre, aucune promesse de résultat, aucun conseil médical, juridique ou financier.

## Glossaire métier → code
| Métier | Code |
|---|---|
| Praticienne | Practitioner |
| Prestation | Offering |
| Créneau | Slot |
| Plage récurrente / exception | RecurringWindow / AvailabilityException |
| Rendez-vous | Appointment |
| Chez la praticienne, chez la cliente, visio, téléphone | IN_PERSON, CLIENT_HOME, VIDEO, PHONE |

## Frontend
- Rendu défini dans `app.routes.server.ts` : `account/` (route `mon-espace`), `admin/`, `reserver/:slug` et `connexion` en rendu client (guards) ; `''`, `seances`, `a-propos` et les pages légales (`mentions-legales`, `conditions-generales`, `confidentialite`) en rendu serveur (`RenderMode.Server`) ; le reste est prérendu. Toute nouvelle route doit y être déclarée.
- Vidéo d'une prestation : `public/offerings/offering-video` (utilisé par « Séances et tarifs » et la page de réservation) réutilise le lecteur `public/about/presentation-video` ; transcription dans un bloc « Lire la transcription ».
- Pages publiques dans `public/` (dont `about/` et `legal/`, ce dernier lit l'identité légale depuis l'API) ; facturation côté praticienne dans `admin/billing/` (note de séance, encaissements, livre des recettes) ; suppression de compte dans `account/account-erasure.ts`.
- Jamais `window` ou `document` sans vérifier la plateforme (compatibilité SSR).
- En-têtes de sécurité posés par le serveur de rendu (`src/server.ts`, valeurs dans `src/server-security.ts`) : CSP stricte à nonce. Ne pas retirer `ngCspNonce` de `index.html` ; aucun script ou style en ligne, ni `style="…"`, ajouté à la main (la CSP les bloquerait).
- Le client API doit être généré depuis l'OpenAPI du backend (springdoc) : ne pas l'éditer à la main. Pas encore en place : les appels passent pour l'instant par des fichiers `*-api.ts` écrits à la main (ex. `booking/booking-api.ts`).
- Les règles Angular détaillées (signals, Signal Forms, pas de `standalone: true`, pas d'`OnPush` explicite, a11y) sont dans `frontend/CLAUDE.md`.
- Accessibilité visée : WCAG AA. Mobile d'abord, surtout pour l'admin.

## Déploiement (recette)
- `deploy/compose.yaml` : gateway nginx, backend, frontend SSR, PostgreSQL, Mailpit ; tout sur 127.0.0.1.
- Sur le VPS : `~/auclairdeso/deploy/deploy.sh` (git pull, docker compose up --build, redémarrage de la gateway).
- Accès public : https://vps-5ff241d3.vps.ovh.net:8443 ; mot de passe demandé une fois à la porte `/recette/acces`, puis cookie de 90 jours (nginx du VPS ; fichiers dans `deploy/host/`, installés à la main ; secret dans `/etc/nginx/auclairdeso-recette.conf`).
- Accès par le tunnel SSH : http://127.0.0.1:8090 ; Mailpit : http://localhost:8025 (tunnel uniquement).
- Secrets : `deploy/.env` et `/etc/nginx/auclairdeso.htpasswd`, jamais versionnés.
- Lien magique limité (nginx par IP ; backend par IP, par adresse 3/15 min et 10/24 h, 100/jour au total hors personnel, `MagicLinkQuota`). Un lien refusé n'est pas envoyé mais la réponse reste 204 ; un WARN sans adresse apparaît dans `docker compose logs backend`. Compteurs en mémoire : `docker compose restart backend` débloque aussitôt un membre du personnel.
- HSTS coupé tant que le domaine définitif n'est pas en HTTPS : interrupteur unique `HSTS_MAX_AGE` (`deploy/.env`), mode d'emploi dans `deploy/.env.example`. Spring Security n'en envoie pas.

## Spécification
- Le comportement métier attendu est décrit dans `docs/specification.md`.
  Le lire avant de développer une fonctionnalité ; le mettre à jour quand une règle change.
