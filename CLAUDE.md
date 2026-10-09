# CLAUDE.md — Au clair de So

Site vitrine + prise de rendez-vous en ligne pour une médium en micro-entreprise.
Monorepo : `backend/` (Spring Boot), `frontend/` (Angular SSR), `infra/` (Docker Compose, nginx).
Le document de cadrage fait référence pour le périmètre, l'architecture et la roadmap.

## Mode de travail
- Tony apprend en recopiant le code dans IntelliJ. Pour « explique » ou « génère » : fichiers COMPLETS (chemin + contenu intégral) et le pourquoi du choix. Ne modifier le dépôt directement que si la demande le dit.
- Une étape à la fois, vérifiable (test, curl, écran). Finir par un message Conventional Commits (feat, fix, refactor, test, chore, docs).
- Réponses en français ; code, identifiants et commits en anglais.
- Toute nouvelle dépendance est signalée et justifiée.

## Stack
- Java 25, Spring Boot 4.1.x, Spring Modulith 2.1, Spring Security 7.1, Spring Data JPA (Hibernate 7), PostgreSQL 17, Flyway, Maven wrapper.
- Angular 22 : standalone, zoneless, signals, Signal Forms, httpResource, @Service, OnPush par défaut, @angular/ssr, Angular CDK/Aria, Tailwind CSS, Vitest, Playwright.
- Intégrations : Stripe Checkout, Google Calendar API v3, Brevo (SMTP), iCal4j.
- Infra : Docker Compose sur VPS OVH, nginx existant comme reverse proxy, GitHub Actions.

## Commandes (à ajuster une fois le dépôt créé)
- Backend : `.\mvnw.cmd verify` (Windows) ou `./mvnw verify` (Linux/CI), `.\mvnw.cmd spring-boot:run`
- Frontend : `npm ci`, `ng serve`, `ng test`, `ng build` (SSR + prérendu)

## Architecture backend
- Package racine `fr.auclairdeso`, un sous-package par module : identity, catalog, scheduling, booking, payment, calendar, notification, content, shared.
- Un module n'utilise que l'API publique d'un autre (package racine du module) ou ses événements ; `ApplicationModules.of(...).verify()` en test.
- Records pour DTO et value objects ; erreurs HTTP en ProblemDetail.
- Dates en `Instant` / `timestamptz` ; conversion Europe/Paris uniquement à l'affichage.
- Une migration Flyway mergée ne se modifie jamais : on en ajoute une nouvelle.

## Règles non négociables
- Pas de double réservation : contrainte d'exclusion PostgreSQL sur (practitioner_id, slot) pour les statuts PENDING_PAYMENT et CONFIRMED.
- Webhooks Stripe idempotents (identifiant d'événement stocké).
- Consentements horodatés : version des CGV, majorité, demande d'exécution anticipée sous 14 jours.
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
| Acompte | Deposit |
| Cabinet, visio, téléphone, écrit | IN_PERSON, VIDEO, PHONE, WRITTEN |

## Frontend
- Routes publiques prérendues ; `account/` et `admin/` en rendu client, protégées par des guards.
- Jamais `window` ou `document` sans vérifier la plateforme (compatibilité SSR).
- Le client API est généré depuis l'OpenAPI du backend : ne pas l'éditer à la main.
- Accessibilité visée : WCAG AA. Mobile d'abord, surtout pour l'admin.

## Déploiement (recette)
- `deploy/compose.yaml` : gateway nginx, backend, frontend SSR, PostgreSQL, Mailpit ; tout sur 127.0.0.1.
- Sur le VPS : `~/auclairdeso/deploy/deploy.sh` (git pull, docker compose up --build, redémarrage de la gateway).
- Accès public : https://vps-5ff241d3.vps.ovh.net:8443 ; mot de passe demandé une fois à la porte `/recette/acces`, puis cookie de 90 jours (nginx du VPS ; fichiers dans `deploy/host/`, installés à la main ; secret dans `/etc/nginx/auclairdeso-recette.conf`).
- Accès par le tunnel SSH : http://127.0.0.1:8090 ; Mailpit : http://localhost:8025 (tunnel uniquement).
- Secrets : `deploy/.env` et `/etc/nginx/auclairdeso.htpasswd`, jamais versionnés.

## Spécification
- Le comportement métier attendu est décrit dans `docs/specification.md`.
  Le lire avant de développer une fonctionnalité ; le mettre à jour quand une règle change.
