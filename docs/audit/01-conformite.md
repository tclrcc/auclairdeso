# Audit n° 1 — Conformité

> Audit en **lecture seule** du 10 novembre 2026, sur la branche `audit/novembre`
> (dernier commit `d1b7ec0`). Aucun code modifié, aucune migration, aucun commit.
> `deploy/.env` n'a pas été ouvert.

**Comment lire les preuves.** Chaque constat cite un fichier et une ligne. Pour alléger les
tableaux, deux préfixes sont sous-entendus :

| Préfixe | Chemin complet |
| --- | --- |
| `be/` | `backend/src/main/java/fr/auclairdeso/` |
| `fe/` | `frontend/src/app/` |

Les migrations sont citées par leur numéro seul (`V9:45` = `backend/src/main/resources/db/migration/V9__create_client_and_appointment.sql`, ligne 45), les tests par leur chemin sous `backend/src/test/java/fr/auclairdeso/`.

**CONFIRMÉ** = j'ai lu le code cité. **SUPPOSÉ** = déduit de noms, de chemins ou d'une
bibliothèque extérieure, sans avoir lu l'implémentation.

**Numérotation Flyway.** `V15` est la dernière migration sur `main`
(`git ls-tree main backend/src/main/resources/db/migration/`). Toute nouvelle migration part donc
de **V16** — y compris une branche de statistiques, qui doit passer de V15 à V16. Une migration
mergée ne se modifie jamais : V16 doit *assouplir* les contraintes de V9, pas les réécrire.

---

## 1. Synthèse

### Bloquant avant ouverture

**S1 — Le site peut annoncer un paiement en ligne qui n'existe pas.** L'énumération
`PaymentPolicy` garde `FULL_ONLINE` et `DEPOSIT_ONLINE`, l'éditeur d'administration laisse So les
choisir (« Paiement complet en ligne »), et la page publique « Séances et tarifs » imprime alors
« Réglé en ligne à la réservation » ou « Acompte de X à la réservation » — tandis que l'article 4
des conditions générales dit « **aucun paiement en ligne** » sur le même site. Une visiteuse peut
lire les deux à une minute d'intervalle. CONFIRMÉ. Effort S.

**S2 — Les notes partent avec une identité de remplacement tant que `deploy/.env` n'est pas
rempli.** Les valeurs par défaut sont « Nom de la praticienne EI », « Adresse professionnelle à
configurer », « SIRET à configurer », et elles sont recopiées **définitivement** sur chaque note
émise. L'adresse professionnelle est une mention obligatoire de l'arrêté 83-50/A. La domiciliation
doit être arrêtée avant la première séance facturée. CONFIRMÉ. Effort S, mais c'est une décision.

### Important

**S3 — La question ciblée (15 €) est irréalisable sans contournement**, et le contournement
serait nuisible : un faux créneau bloquerait une vraie place et compterait dans le maximum
journalier, ce que la spécification interdit explicitement pour cette prestation. CONFIRMÉ.
Effort M, migration V16. Déjà identifié comme tranche 3E, « À faire ». **Bloquant seulement si la
question ciblée est vendue dès l'ouverture, à décider avec So.**

**S4 — La remise de parrainage −10 % n'a aucune place dans le modèle**, et ce qui casse n'est pas
la numérotation des notes : c'est que la note est émise **automatiquement, dans la transaction qui
marque la séance effectuée**, au prix figé à la réservation, et qu'aucun écran ne permet d'en
changer le montant. Une remise doit donc être décidée à la réservation, ou le modèle doit changer.
CONFIRMÉ. Effort M.

**S5 — Le message d'effacement de compte dit le contraire de ce que fait le code.** La cliente
lit que ses séances passées sont conservées « sans votre nom », alors que la note garde
volontairement son nom (l'arrêté l'exige) et que la politique de confidentialité, elle, le dit
correctement. C'est une affirmation sur ses données faite à la personne concernée, au moment
précis où elle exerce son droit. CONFIRMÉ. Effort S.

**S6 — La politique de confidentialité décrit une mesure d'audience qui n'existe pas encore**
(« Deux nombres par jour : les pages vues et les visiteurs… »), alors que les statistiques sont la
tranche 5B, « À faire ». CONFIRMÉ. Effort S.

**S7 — L'écart de consentement signalé dans `CLAUDE.md` est réel, et plus large que décrit :** ni
version des conditions générales, ni demande d'exécution anticipée, ni trace par consentement ne
sont enregistrées. Et l'article 6 des CGV s'appuie pourtant sur une exécution anticipée, qu'il
rend implicite. Voir le volet C. CONFIRMÉ.

**S8 — Le maximum de 3 séances par jour n'est couvert par aucun test.** La règle est bien dans le
code, mais `maxSessionsPerDay` n'apparaît nulle part sous `backend/src/test`. CONFIRMÉ. Effort S.

**S9 — `CLAUDE.md` décrit des modes de consultation que le code n'a plus.** Le glossaire annonce
« écrit → WRITTEN » et omet « chez la cliente → CLIENT_HOME », alors que la spécification dit
« Il n'y a **pas** de séance par écrit » et que V6 a supprimé `WRITTEN`. Le glossaire se trompe
deux fois. CONFIRMÉ. Effort S.

**S10 — Les descriptions de prestations sont du texte libre affiché tel quel, sans aucun
garde-fou contre une allégation thérapeutique** — et c'est précisément là que les motifs de So
(zona, lombalgie, abcès dentaire, eczéma, douleurs liées aux traitements) entreront. Les textes
écrits à la main dans le frontend sont, eux, irréprochables. Mécanisme CONFIRMÉ ; qu'un texte
problématique y arrive est SUPPOSÉ. Voir le volet D.

---

## 2. Volet A — Adéquation du modèle au catalogue arrêté avec So

| # | Constat | Preuve (`chemin:ligne`) | État | Gravité | Effort |
| --- | --- | --- | --- | --- | --- |
| A1 | Une prestation porte **une** durée fixe et **une** pause fixe. Les durées variables (magnétisme 30-45 min, rééquilibrage 45 min-1 h) ne sont pas représentables. | `be/catalog/Offering.java:39-43`, `be/catalog/OfferingDraft.java:27-32`, `V1:18`, `V6:10-11` | CONFIRMÉ | Important | S (contournement) / M-L (vraie durée) |
| A2 | `SlotCalculator` ne reçoit qu'une durée et qu'une pause : rien ne porte une plage. | `be/booking/SlotCalculator.java:27-29`, `51-67` | CONFIRMÉ | — | — |
| A3 | `ends_at` et `occupied_until` sont calculés une fois pour toutes à la création du rendez-vous et jamais révisés. Clore une séance sur sa durée réelle n'est pas prévu. | `be/booking/Appointment.java:112-113` | CONFIRMÉ | Important | M-L |
| A4 | Une demande de réservation **exige** un créneau : `start` est obligatoire. | `be/booking/BookingRequest.java:20-21` | CONFIRMÉ | Important (bloquant si la question ciblée est vendue dès l'ouverture) | M |
| A5 | Les trois horodatages du rendez-vous sont `NOT NULL`, avec une contrainte d'ordre. Un rendez-vous sans créneau viole le schéma. | `V9:27-29`, `V9:41` (`appointment_times_ck`) | CONFIRMÉ | Important (bloquant si la question ciblée est vendue dès l'ouverture) | M, migration V16 |
| A6 | La demande est refusée si le début n'est pas l'un des créneaux calculés. | `be/booking/BookingRequests.java:82-84`, `be/booking/Availability.java:42-46` | CONFIRMÉ | Important (bloquant si la question ciblée est vendue dès l'ouverture) | M |
| A7 | Une demande `REQUESTED` bloque son créneau **et** compte dans le maximum journalier : un faux créneau pour la question ciblée mangerait une vraie place, ce que la spec 4.3 interdit. | `V9:45-46`, `be/booking/SlotCalculator.java:82-86`, spec 4.3 ligne 137 | CONFIRMÉ | Important (bloquant si la question ciblée est vendue dès l'ouverture) | M |
| A8 | La spec exige le pseudo Messenger pour la question ciblée ; le code ne l'exige que pour `VIDEO`. | spec 5.2 ligne 166 contre `be/booking/BookingRequests.java:62-64`, `V9:43` | CONFIRMÉ | Important | S |
| A9 | Le canal de réponse de la question ciblée (SMS ou Messenger, au choix de la cliente) n'a aucune colonne. | `V9:20-47` (aucune), `be/booking/Appointment.java:31-97` | CONFIRMÉ | Important | S, migration V16 |
| A10 | Aucune notion de remise, de parrainage ou de code promotionnel n'existe dans le dépôt. | `grep -riE "remise\|discount\|parrain\|referral\|promo\|coupon"` : seuls résultats, le mot courant « une note vous est remise » (`fe/public/about/about-content.ts:69`, `fe/public/legal/conditions-generales-page.html:52`) | CONFIRMÉ | Important | M |
| A11 | La note est émise automatiquement et immédiatement à la clôture de la séance, au prix recopié du rendez-vous, lui-même recopié de la prestation à la réservation. Aucun écran ne change le montant. | `be/billing/SessionBilling.java:24-27`, `be/billing/Bills.java:35-42`, `be/billing/Bill.java:96`, `be/booking/Appointment.java:109` | CONFIRMÉ | Important | M |
| A12 | Une note est immuable : tous ses champs sont `updatable = false`, seul le paiement bouge. Une remise oubliée ne se corrige pas. | `be/billing/Bill.java:35-69`, `104-122` | CONFIRMÉ | Important | M |
| A13 | La note porte un seul libellé et un seul montant, avec `amount_cents > 0`. L'arrêté exige un « décompte » : une remise doit être visible. | `V14:23-24`, `V14:31`, `be/billing/Bill.java:65-69` | CONFIRMÉ | Important | M, migration V16 |
| A14 | La **numérotation ne casse pas** : une ligne par année, incrément verrouillé, sans trou. Une note remisée prend le numéro suivant comme une autre. | `be/billing/BillNumbers.java:23-30`, `V14:6-9`, `V14:29` | CONFIRMÉ | — | — |
| A15 | Le **livre des recettes ne casse pas** si la remise est dans `amount_cents` : il lit le montant de la note. En revanche une remise appliquée à l'encaissement ferait diverger note et livre. | `be/billing/Bill.java:137-143`, `be/billing/Bills.java:72-77`, `be/billing/ReceiptBookCsv.java:20-33` | CONFIRMÉ | — | — |
| A16 | Le **seuil de 25 € ne casse pas** : après −10 %, 50→45 €, 80→72 €, 60→54 €, 70→63 €, la note reste obligatoire. Seule la question ciblée est en dessous, et le code n'omet la note que pour un prix nul — en émettre une sous 25 € n'est pas interdit. | `be/billing/Bills.java:36`, spec 10 ligne 305 | CONFIRMÉ | — | — |
| A17 | Aucun champ vidéo sur une prestation. La seule vidéo du site est la présentation unique de So, codée en dur. | `be/catalog/Offering.java` (aucune colonne), `be/catalog/OfferingView.java`, `fe/public/about/about-content.ts:21-26` | CONFIRMÉ | Important | M, migration V16 |
| A18 | Le mécanisme d'hébergement des médias existe déjà et se réemploie tel quel : fichier servi par nginx sous `/contenus/` depuis `MEDIA_DIR`, jamais dans le dépôt. Le composant masque la transcription quand elle est vide. | `fe/public/about/about-content.ts:9-13`, `fe/public/about/presentation-video.html:26-31`, spec 11 hypothèse 16 ligne 329 | CONFIRMÉ | — | — |
| A19 | Il **existe** une notion d'indisponibilité réutilisable : la table `closure`, jours entiers, bornes incluses, administrée depuis `/admin/fermetures`. | `V7:20-28`, `be/booking/Closure.java:40-51`, `be/booking/ClosedPeriod.java:8-10`, `be/booking/ClosureAdministration.java:23-41` | CONFIRMÉ | — | — |
| A20 | Mais `closure` n'est consommée que par le calcul des créneaux, et **aucun délai de réponse n'existe** : `BookingRules` n'a pas de champ pour les 48 h de la question ciblée. | `be/booking/SlotCalculator.java:36`, `78-80`, contre `be/booking/BookingRules.java:15-22` | CONFIRMÉ | Important | S-M |

### A.1 — Durées variables (30-45 min, 45 min-1 h)

Non supportées. `durationMinutes` et `bufferMinutes` sont deux entiers fixes par prestation, bornés
entre 15 et 240 minutes côté entité, côté validation et côté base. `SlotCalculator` reçoit une
`Duration duration` et une `Duration breakAfter`, pas une plage. Et `Appointment` fige
`ends_at = start + durée` puis `occupied_until = ends_at + pause` à la création : rien dans le code
ne révise ces valeurs quand la séance se termine réellement.

**Contournement immédiat, sans migration, effort S :** déclarer la durée **haute** — 45 min pour le
magnétisme, 60 min pour le rééquilibrage. Le créneau est alors réservé au maximum et la séance peut
finir plus tôt sans conséquence : la cliente ne voit qu'un créneau de début, et `occupied_until` ne
sert qu'à empêcher le chevauchement. Reste à décider ce qu'on **affiche** : la page des séances
imprime `{{ offering.durationMinutes }} min` (`fe/public/offerings/offerings-page.html:18`), donc
« 45 min » et non « 30 à 45 min ». Afficher une plage demande une colonne de plus.

**Vraie durée variable** (la praticienne clôt sur la durée réelle) : effort M-L. Il faut une colonne
de fin réelle en V16, revoir la plage de la contrainte d'exclusion, et décider si une séance plus
courte libère le créneau suivant. Aucun impact sur la facturation : le prix est forfaitaire.

### A.2 — Prestation sans créneau (question ciblée)

Impossible sans contournement, et le contournement serait **nuisible**. Quatre verrous, tous
confirmés :

1. `BookingRequest.start` est `@NotNull`.
2. `starts_at`, `ends_at` et `occupied_until` sont `NOT NULL`, avec
   `appointment_times_ck CHECK (starts_at < ends_at AND ends_at <= occupied_until)`.
3. La demande est refusée si le début n'est pas un créneau calculé.
4. Une demande `REQUESTED` bloque son créneau via `appointment_no_overlap` **et** est comptée par
   `sessionsOn` dans le maximum journalier.

Le point 4 est le plus gênant : la spec 4.3 (ligne 137) dit que la question ciblée « n'occupe pas
de créneau et ne compte pas dans le maximum journalier ». Un faux créneau ferait exactement
l'inverse des deux.

**Ce qu'il faut, effort M, migration V16 :** rendre les trois horodatages nullables et restreindre
`appointment_no_overlap` et `appointment_times_ck` aux rendez-vous qui ont un créneau (un `WHERE
starts_at IS NOT NULL` sur l'exclusion, et une contrainte conditionnelle sur l'ordre). V9 étant
mergée, V16 doit supprimer puis recréer ces contraintes, pas éditer V9. S'ajoutent le pseudo
Messenger obligatoire (A8) et le canal de réponse (A9).

### A.3 — Remise de parrainage −10 % face à la facturation

Ce qui casse, par ordre d'importance :

1. **Le moment de l'émission** (A11, A12). La note sort automatiquement, dans la transaction de la
   clôture, au prix figé à la réservation, et elle est immuable. Conséquence directe : **une remise
   doit être décidée à la réservation**. Si elle est décidée après, la note est déjà partie au plein
   tarif et ne peut plus être corrigée — il faudrait émettre une seconde note, ce que le modèle
   interdit (`bill_appointment_uk UNIQUE (appointment_id)`, `V14:30`).
2. **Le décompte** (A13). Un seul libellé, un seul montant, `amount_cents > 0`. Pour qu'une remise
   de 10 % soit visible comme l'arrêté le demande, il faut un montant brut, une remise et un net,
   ou de vraies lignes de note.
3. **La numérotation : rien ne casse** (A14).
4. **Le livre des recettes : rien ne casse** si la remise est dans `amount_cents` (A15). Le piège à
   éviter est d'appliquer la remise à l'encaissement : note et livre divergeraient.
5. **Le seuil de 25 € : rien ne casse** (A16).

En résumé : le risque n'est pas comptable, il est chronologique. La question à trancher avec So est
*quand* la remise est accordée, pas *comment* elle est numérotée.

### A.4 — Vidéo par prestation : où la rattacher

Trois colonnes sur `offering` en V16 — `video_path`, `video_transcript`, `video_duration_label` —
exposées dans `OfferingView`, et réemploi tel quel de l'interface `SiteVideo` et du composant
`PresentationVideo` sur `/seances` et `/reserver/:slug`. Le fichier va dans `MEDIA_DIR`, servi sous
`/contenus/`, jamais dans le dépôt, exactement comme la présentation actuelle. Effort M. La
transcription compte pour l'objectif WCAG AA, et le composant sait déjà masquer sa section quand
elle est vide.

### A.5 — « Sauf congés » sur le délai de 48 h

Oui, la notion existe et elle est réutilisable : `closure`. Mais elle n'est lue que par le calcul
des créneaux, et le délai de 48 h lui-même n'existe nulle part — ni paramètre, ni colonne, ni
affichage. Un « sous 48 h hors congés » se calcule donc depuis `closure` sans migration, en sautant
les jours fermés dans le décompte : effort S-M. Sans cela, le « sauf congés » reste une mention de
texte, non une règle.

---

## 3. Volet B — Conformité à la spécification

### B.1 — Règles conformes et couvertes par un test

| Règle (spec) | Preuve | Test | État |
| --- | --- | --- | --- |
| Réservation au plus tôt le lendemain, au plus tard 2 mois (4.3) | `be/booking/SlotCalculator.java:30-33`, défauts `be/booking/BookingRules.java:18-19` ; borne API 62 jours `be/booking/SlotController.java:17`, `29-32` | `booking/SlotCalculatorTests.java:55`, `:63` | CONFORME |
| Créneaux enchaînés, pause après la séance, fin avant fermeture (4.2) | `be/booking/SlotCalculator.java:51-67` | `booking/SlotCalculatorTests.java:40`, `:45`, `:78`, `:85`, `:92` ; l'exemple même de la spec rejoué dans `booking/BookingFlowTests.java:80-87` | CONFORME |
| Pas de double réservation (4.3, `CLAUDE.md`) | `V9:45-46` (`appointment_no_overlap`), `be/booking/AppointmentStatus.java:17`, course perdue en 409 `be/booking/BookingRequests.java:89-94`, `be/booking/Agenda.java:98-104` | `booking/BookingFlowTests.java:90`, `:107` ; `booking/AgendaFlowTests.java:141` | CONFORME |
| Une seule réservation à venir par cliente (5.3) | `be/booking/BookingRequests.java:77-81` | `booking/BookingFlowTests.java:96` | CONFORME |
| Blocage par email **et** par téléphone, quelle que soit l'écriture (6) | `be/booking/BookingRequests.java:71-73`, `be/booking/ClientRepository.java:21-25`, `be/booking/Phones.java:15-27` | `booking/ClientFilesFlowTests.java:97-128` (refus avec un autre email et une autre écriture, puis accepté après déblocage) ; `booking/PhonesTests.java` | CONFORME |
| Message neutre à une cliente bloquée (5.3) | `be/booking/BookingRequests.java:72`, repris dans les CGV `fe/public/legal/conditions-generales-page.html:70` | `booking/ClientFilesFlowTests.java:110-114` | CONFORME |
| Expiration d'une demande la veille à 21 h (5.5) | `be/booking/AppointmentDeadlines.java:55-67`, `be/booking/BookingDeadlines.java:16`, `be/booking/BookingSchedule.java:23-30` | `booking/NotificationFlowTests.java:124` ; `booking/AppointmentTests.java:99` | CONFORME |
| Annulation tardive comptée seulement sur un rendez-vous confirmé (6) | `be/booking/Appointment.java:162-172`, `be/booking/Client.java:116-122` | `booking/AppointmentTests.java:48`, `:56`, `:61` ; `booking/ClientTests.java:11` ; `booking/AgendaFlowTests.java:107` | CONFORME |
| 2 manquements → blocage ; absence → blocage immédiat ; déblocage → compteur à zéro (6) | `be/booking/Client.java:116-122`, `147-154` ; `be/booking/Appointment.java:199-203` | `booking/ClientTests.java:11`, `:20` ; `booking/AppointmentTests.java:77` ; `booking/AgendaFlowTests.java:107`, `:124` | CONFORME |
| Une note par séance effectuée, aucune pour une séance gratuite (3.5) | `be/billing/SessionBilling.java:24-27`, `be/billing/Bills.java:35-42`, `V14:30` | `billing/BillingFlowTests.java:80` | CONFORME |
| Numérotation `2026-0001`, une série par an, sans trou (3.5) | `be/billing/BillNumbers.java:23-30`, `V14:6-9`, `V14:29` | `billing/BillingFlowTests.java:80` | CONFORME |
| Mentions obligatoires de l'arrêté sur la note (10) | `be/billing/Bill.java:29`, `83-98`, `146-153` | `billing/BillTests.java:21` ; `notification/MessagesTests.java:71` | CONFORME |
| Encaissement : moyen et date, jamais dans le futur, corrigé par annulation (3.5) | `be/billing/Bill.java:104-122`, `V14:34`, verrou `be/billing/Bills.java:84-87` | `billing/BillTests.java:34`, `:47` ; `billing/BillingFlowTests.java:104` | CONFORME |
| Livre des recettes chronologique, export CSV (10) | `be/billing/Bills.java:72-77`, `be/billing/ReceiptBook.java:14-25`, `be/billing/ReceiptBookCsv.java:22-31`, protection contre l'injection CSV `:44-50` | `billing/ReceiptBookCsvTests.java` ; `billing/BillAdminControllerTests.java:34`, `:54` | CONFORME |
| Effacement de compte, refusé tant qu'un rendez-vous à venir existe (9) | `be/booking/ClientErasure.java:38-61`, `be/booking/Client.java:133-144`, `V15:7-8`, `be/identity/ClientAccountDeletion.java:31-39`, `be/billing/ErasedClientBills.java:22-25` | `booking/AccountErasureFlowTests.java:69`, `:83` ; `booking/ClientErasureTests.java:23`, `:39`, `:51` | CONFORME |
| Purge des photos 7 jours après la séance, chaque nuit (9) | `be/booking/AppointmentDeadlines.java:45-47`, `be/booking/AppointmentPhotoRepository.java:17-22`, `be/booking/BookingSchedule.java:33-39`, `be/booking/BookingRules.java:22` | `booking/AccountErasureFlowTests.java:121` | CONFORME |
| Photos en base, jamais sur disque, réservées au personnel (9) | `V9:53-58`, `be/booking/Agenda.java:72-75` | `booking/AgendaFlowTests.java:95` | CONFORME |
| Programme du soir à 19 h, rien les soirs vides (7) | `be/booking/BookingSchedule.java:41-46`, `be/booking/AppointmentDeadlines.java:95-115` | `notification/MessagesTests.java:50` | CONFORME |
| Plages lundi-jeudi 14 h-18 h (4.1) | `V7:13-17` | `booking/SlotCalculatorTests.java:50` (rien les jours sans plage) | CONFORME |
| Rendez-vous manuel hors plages, mais jamais sur une autre séance (4.3, 8) | `be/booking/Agenda.java:77-105` | `booking/AgendaFlowTests.java:141` | CONFORME |

### B.2 — Règles conformes mais **sans aucun test**

| Règle (spec) | Preuve que le code la tient | État |
| --- | --- | --- |
| Maximum 3 séances par jour (4.3) — **aucune occurrence de `maxSessionsPerDay` ni `max-sessions` sous `backend/src/test`** | `be/booking/SlotCalculator.java:36`, `82-86` ; défaut 3 `be/booking/BookingRules.java:17` | CONFORME, non testé |
| Refus d'une mineure par l'API (5.3) — seule la règle de date est testée en unitaire | `be/booking/BookingRequests.java:53-55`, `be/booking/ClientDetails.java:32-34` ; test unitaire `booking/ClientDetailsTests.java:13`, `:18` | CONFORME, refus non testé |
| Présentiel refusé à une cliente non « de confiance » (3.4) | `be/booking/BookingRequests.java:74-76` | CONFORME, non testé |
| Adresse obligatoire en `CLIENT_HOME` (5.2) | `be/booking/BookingRequests.java:59-61`, `V9:42` | CONFORME, non testé |
| Rappel la veille à 12 h (7) — seul « une seule fois » est testé | `be/booking/AppointmentDeadlines.java:74-87`, `be/booking/BookingDeadlines.java:17` ; test de l'unicité `booking/AppointmentTests.java:109` | CONFORME, horaire non testé |

### B.3 — Écarts

| # | Écart | Preuve | État | Gravité | Effort |
| --- | --- | --- | --- | --- | --- |
| B-E1 | Le paiement en ligne subsiste dans l'énumération, dans l'éditeur et sur la page publique, alors qu'il est hors périmètre (spec 3.5 ligne 93) et que la spec 3.5 ligne 98 ne garde `payment_policy` qu'à `ON_SITE`. | `be/catalog/PaymentPolicy.java:6-14`, `V1:20-25`, `fe/admin/offerings/admin-offering.ts:22-26`, `fe/admin/offerings/offering-editor-page.html:56-72`, `fe/public/offerings/offerings-page.html:28-40` ; contredit `fe/public/legal/conditions-generales-page.html:47-49` | CONFIRMÉ | Bloquant | S |
| B-E2 | Le message d'effacement affirme à la cliente que ses séances sont gardées « sans votre nom », alors que la note garde le nom. | `fe/account/account-erasure.ts:13-15` contre `be/billing/ErasedClientBills.java:11-12`, `22-25` et `fe/public/legal/confidentialite-page.html:134-136` | CONFIRMÉ | Important | S |
| B-E3 | La politique de confidentialité décrit une mesure d'audience qui n'existe pas (tranche 5B « À faire », spec 12 ligne 349). | `fe/public/legal/confidentialite-page.html:50-55`, `85-87` | CONFIRMÉ | Important | S |
| B-E4 | La spec 5.1 prévoit **trois** consentements ; le code en collecte **deux**, la première case fusionnant majorité et conditions générales. | spec 5.1 ligne 149 contre `be/booking/BookingRequest.java:36-40`, `fe/booking/booking-page.html:151-167` | CONFIRMÉ | Important | Voir volet C |
| B-E5 | La page `/seances` affiche « En présentiel » comme mode disponible à tout visiteur, alors que la spec 3.4 veut que « seules ces clientes voient ce mode ». Le parcours de réservation, lui, est correct : il filtre et explique. | `fe/public/offerings/offerings-page.html:21-25`, `fe/public/offerings/offering.ts:21`, servi anonymement par `be/catalog/OfferingController.java:22-25` ; parcours correct `fe/booking/booking-page.ts:45-48`, `fe/booking/booking-page.html:30-34` | CONFIRMÉ | Confort | S |
| B-E6 | L'identité de la praticienne sur les notes vaut par défaut des valeurs de remplacement, recopiées définitivement sur chaque note émise. | `backend/src/main/resources/application.yaml:71-73`, `be/billing/Bill.java:88-90` | CONFIRMÉ | Bloquant | S (configuration) |
| B-E7 | La spec demande le nom « suivi de EI » sur la note ; rien ne le vérifie, le code fait confiance à la valeur configurée. | `be/billing/BillingProperties.java:15-18`, `24` | CONFIRMÉ | Confort | S |

### B.4 — Non vérifiable

| Règle | Pourquoi | État |
| --- | --- | --- |
| Statistiques (8) | Tranche 5B, « À faire » (spec 12 ligne 349). Rien à vérifier — mais la politique de confidentialité l'annonce déjà (B-E3). | NON VÉRIFIABLE |
| SMS à la praticienne (7), envoi réel par Gmail (3D) | Tranche 3D, « En partie » (spec 12 ligne 345) ; fournisseur de SMS non choisi. Hors périmètre connu. | NON VÉRIFIABLE |
| Usage unique du lien magique (2) | Assuré par `JdbcOneTimeTokenService` de Spring Security, hors du dépôt. La durée de 10 minutes, elle, est dans le code : `be/identity/MagicLinkConfiguration.java:15`, `26` (CONFIRMÉ). | SUPPOSÉ |

### B.5 — Contradictions entre la spécification et `CLAUDE.md`

| # | Contradiction | Qui a raison | État |
| --- | --- | --- | --- |
| B-C1 | Le glossaire de `CLAUDE.md` annonce « Cabinet, visio, téléphone, **écrit** → IN_PERSON, VIDEO, PHONE, **WRITTEN** » et omet `CLIENT_HOME`. | La spec (3.4 ligne 89 : « Il n'y a **pas** de séance par écrit ») et le code : `V6:4-7` supprime `WRITTEN` et ajoute `CLIENT_HOME`, `be/catalog/ConsultationMode.java:6-17`. `CLAUDE.md` est faux deux fois. | CONFIRMÉ |
| B-C2 | `CLAUDE.md` impose des consentements horodatés « version des CGV, majorité, demande d'exécution anticipée sous 14 jours ». Aucun des trois n'est enregistré comme tel. | Le code et la spec. `CLAUDE.md` signale lui-même l'écart « à vérifier (audit) » : il est confirmé, et plus large. Voir volet C. | CONFIRMÉ |
| B-C3 | La spec 3.5 ligne 98 dit garder `payment_policy` « (valeur `ON_SITE`) », mais le code et l'interface offrent les trois valeurs. | La spec. Le code n'a pas suivi. Voir B-E1. | CONFIRMÉ |
| B-C4 | **La spec se contredit elle-même** sur l'effacement : 11 (hypothèse 15, ligne 330) dit garder les notes « sans rien qui la nomme », tandis que 9 (lignes 285 et 289) dit les garder « avec le nom qui figurait sur la note ». | Le code suit 9, et c'est l'arrêté qui l'impose. L'hypothèse 15 doit être corrigée ; c'est elle qui a produit l'écart B-E2. | CONFIRMÉ |
| B-C5 | `CLAUDE.md` dit que le médiateur « reste vide tant qu'il n'est pas choisi ». | Conforme : `be/legal/LegalProperties.java:29`, `backend/src/main/resources/application.yaml:81`, et les CGV affichent l'avertissement `fe/public/legal/conditions-generales-page.html:118-122`. | CONFORME |

---

## 4. Volet C — Écart connu : les consentements

### C.1 — Ce que le code enregistre réellement

| # | Constat | Preuve | État |
| --- | --- | --- | --- |
| C1 | **Deux** cases à cocher, pas trois, toutes deux obligatoires (`@AssertTrue`) : `acceptedTerms` et `consentsToDataProcessing`. La première fusionne majorité et conditions générales. | `be/booking/BookingRequest.java:36-40` | CONFIRMÉ |
| C2 | Libellés affichés : « J'ai 18 ans ou plus, et j'accepte les conditions générales (annulation au moins 48 h avant). » puis « J'accepte que mes informations, y compris la raison de ma demande et ma photo, servent uniquement à préparer et suivre ma séance. » | `fe/booking/booking-page.html:151-159`, `161-167` | CONFIRMÉ |
| C3 | **Un seul horodatage** : `appointment.consented_at`, posé à `now` à la création, `updatable = false`, et jamais relu par aucune vue ni aucun événement. | `be/booking/Appointment.java:85-86`, `125` ; `V9:34` ; obligatoire pour une demande en ligne via `appointment_online_consent_ck`, `V11:11-12` | CONFIRMÉ |
| C4 | **Aucune version des conditions générales** n'est enregistrée : ni colonne, ni propriété de configuration, ni constante. | Aucune occurrence dans `be/booking/Appointment.java`, dans les migrations V9 à V15, ni dans `be/legal/LegalProperties.java` | CONFIRMÉ |
| C5 | **Aucune « demande d'exécution anticipée »** n'est enregistrée, sous aucune forme. | Aucune occurrence dans le dépôt | CONFIRMÉ |
| C6 | Les deux booléens eux-mêmes ne sont pas stockés : seule leur conjonction, implicitement, par un `consented_at` non nul. On ne peut pas savoir après coup laquelle a été cochée. | `be/booking/Appointment.java:119-127` (les booléens ne sont pas recopiés) | CONFIRMÉ |
| C7 | Les rendez-vous saisis par la praticienne n'ont **pas** de consentement, et c'est volontaire : la contrainte ne l'exige que pour la source `ONLINE`. | `V11:10-12`, `be/booking/Appointment.java:130-138` | CONFIRMÉ |
| C8 | **L'article 6 des CGV s'appuie pourtant bien sur une exécution anticipée, et la rend implicite :** « Si vous demandez que la séance ait lieu avant la fin de ces quatorze jours — ce que vous faites en choisissant un créneau plus proche — ». | `fe/public/legal/conditions-generales-page.html:87-101` | CONFIRMÉ |
| C9 | La majorité, elle, est vérifiée **deux fois** : par la case et côté serveur contre la date de naissance, dans le fuseau de la praticienne. C'est la preuve la plus solide, conservée jusqu'à l'effacement du compte. | `be/booking/BookingRequests.java:53-55`, `be/booking/ClientDetails.java:32-34`, `be/booking/Client.java:139` | CONFIRMÉ |

Autrement dit : le contrat écrit repose sur un consentement que l'application n'enregistre pas. Sa
seule trace serait la date du créneau comparée à `consented_at` — dérivable, mais jamais pensée
comme preuve, et perdue si la cliente efface son compte.

### C.2 — Options, sans trancher

| Option | Ce qu'elle fait | Coût | Ce qu'elle laisse ouvert |
| --- | --- | --- | --- |
| **O1** — Aligner `CLAUDE.md` | Retirer du fichier la règle d'exécution anticipée, garder deux cases. | S, documentation seule | Les CGV continuent de s'appuyer sur un consentement non enregistré. |
| **O2** — Enregistrer ce qui est déjà collecté | `terms_accepted_at`, `data_consent_at`, `terms_version` sur `appointment` ; propriété `auclairdeso.legal.terms-version` affichée sur la page des CGV et envoyée avec la demande. | M, migration V16 | On sait alors, pour chaque réservation, quel texte a été accepté et quand. Ne traite pas l'exécution anticipée. |
| **O3** — Case d'exécution anticipée explicite | Affichée seulement quand le créneau choisi tombe dans les quatorze jours, et enregistrée (`early_execution_requested_at`). | M, migration V16 + champ conditionnel dans le Signal Form | C'est l'option qui rend l'article 6 des CGV prouvable. |
| **O4** — Modifier les CGV plutôt que le code | Écrire noir sur blanc que réserver un créneau à moins de quatorze jours **est** la demande, et retenir la date du créneau comme preuve. | S, texte seul | Repose sur une preuve dérivée, perdue à l'effacement du compte. |

O1 et O4 se combinent ; O2 et O3 aussi. O2 seul est le meilleur rapport entre le coût et ce qu'on
gagne : on cesse de dépendre d'un seul horodatage opaque.

### C.3 — À faire valider par un professionnel

Ces points relèvent du droit, pas de l'audit technique. À soumettre à un avocat ou à un juriste en
droit de la consommation :

- Une séance réservée à **date et heure fixes** relève-t-elle du droit de rétractation de
  l'article L221-18 du code de la consommation, ou de l'exception des prestations fournies à une
  date déterminée (L221-28) ? La réponse change tout l'article 6 des CGV.
- Une demande d'exécution anticipée **implicite** (« en choisissant un créneau plus proche »)
  suffit-elle, et que faut-il en conserver comme preuve ?
- La version des conditions générales doit-elle être archivée commande par commande, ou la date
  d'acceptation suffit-elle si l'historique des textes est conservé ailleurs ?
- Le consentement au traitement des données sensibles (motif de consultation, photo) doit-il être
  séparé du consentement aux conditions générales — aujourd'hui ils le sont, c'est le point le plus
  solide du dispositif.

---

## 5. Volet D — Textes publics

Lus intégralement : `fe/public/home/home-content.ts`, `home-page.html`,
`fe/public/about/about-content.ts`, `about-page.html`, `presentation-video.html`,
`fe/public/offerings/offerings-page.html`, `offering.ts`, `fe/booking/booking-page.html`, les trois
pages légales, `fe/layout/site-footer.html`, `fe/layout/site-header.html`,
`fe/account/account-erasure.ts`, et le jeu de données de développement
`backend/src/main/resources/db/dev/R__dev_offerings.sql`.

### D.1 — Ce qui est bon, et qu'il faut garder

Aucune promesse de résultat, aucun conseil médical, aucune formulation laissant croire qu'une
pathologie peut être guérie dans les textes écrits à la main. Les garde-fous sont explicites et bien
placés :

| Texte | Fichier | État |
| --- | --- | --- |
| « Un accompagnement par le magnétisme, en présence ou à distance. Il vient en complément d'un suivi médical, jamais à sa place. » | `fe/public/home/home-content.ts:15-17` | CONFORME |
| « Aucune promesse de résultat — Une séance est un accompagnement : elle ne garantit rien et ne vous engage à rien pour la suite. » | `fe/public/about/about-content.ts:47-50` | CONFORME |
| « Ni médecine, ni droit, ni finance — Les séances ne remplacent ni un avis ou un traitement médical, ni un conseil juridique ou financier. » | `fe/public/about/about-content.ts:51-56` | CONFORME |
| « Les séances proposées sont un accompagnement de bien-être. Elles ne remplacent ni un avis, ni un diagnostic, ni un traitement médical… En cas de problème de santé, consultez un professionnel de santé. » | `fe/public/legal/mentions-legales-page.html:23-29` | CONFORME |
| « Elles **ne remplacent ni un avis ni un traitement médical**, et aucun résultat n'est promis. » | `fe/public/legal/conditions-generales-page.html:19-24` | CONFORME |
| Le même avertissement répété en pied de **toutes** les pages. | `fe/layout/site-footer.html:31-34` | CONFORME |
| Le sous-titre de l'accueil ne mentionne pas le cabinet, conformément à la spec 3.4 qui interdit d'afficher l'adresse. | `fe/public/home/home-page.html:20-23` | CONFORME |
| Les mots « cancer », « radiothérapie », « chimiothérapie », « guérir » n'apparaissent **nulle part** dans le dépôt. | `grep -riE "cancer\|radiothérapie\|chimiothérapie\|guéri"` : aucun résultat | CONFIRMÉ |

Le commentaire d'en-tête de `home-content.ts:3-6` — « Rule for every text: describe the session,
never promise a result » — et celui de `about-content.ts:1-6` disent la règle et elle est tenue.

### D.2 — Le vrai risque : les descriptions de prestations

| # | Constat | Preuve | État | Gravité | Effort |
| --- | --- | --- | --- | --- | --- |
| D1 | La description d'une prestation est un texte libre de 4000 caractères, affiché tel quel sur la page publique, sans validation, sans relecture, sans avertissement dans l'éditeur. | `be/catalog/OfferingDraft.java:23-24`, `fe/public/offerings/offerings-page.html:19`, `fe/admin/offerings/offering-editor-page.html` (aucun garde-fou) | CONFIRMÉ | Important | S (avertissement) |
| D2 | La spec 3.2 liste elle-même « Tendinite, eczéma / urticaire, zona, hernie discale / lombalgie, abcès dentaires » comme des **prestations** de magnétisme. Saisies telles quelles, elles feraient du site une liste de pathologies traitées. | `docs/specification.md:67` | CONFIRMÉ (le texte de la spec) / SUPPOSÉ (qu'il soit saisi) | Important | S |

So a cité comme motifs l'abcès dentaire, le zona, la lombalgie, l'eczéma et les douleurs liées aux
traitements de radiothérapie ou de chimiothérapie. Aucun ne doit apparaître comme soigné, et le
cancer ne doit jamais apparaître comme guérissable. Aujourd'hui aucun de ces mots n'est dans le
dépôt : il s'agit donc de **prévention**, pas de correction.

**Reformulation proposée, non appliquée**, pour la prestation unique de magnétisme du catalogue
arrêté avec So (une seule prestation à 60 €, sans sous-catégories) :

> **Magnétisme — coupe-feu**
>
> Une séance de magnétisme, en présence ou à distance, de 30 à 45 minutes selon la séance.
> Le magnétisme est un accompagnement de bien-être : il ne soigne pas, ne remplace ni un avis
> ni un traitement médical, et aucun résultat n'est promis. Si vous suivez un traitement,
> parlez-en d'abord à votre médecin.

Et la règle à tenir pour toute description à venir : **ne jamais présenter un motif comme une
prestation, ni dresser une liste de ce qui serait traité.** Si So veut dire qui vient la voir, la
forme sûre est une phrase factuelle sur sa pratique, sans verbe de guérison et sans nom de
pathologie. Une protection légère et utile, effort S : une phrase d'avertissement au-dessus du champ
« Description » dans l'éditeur (`fe/admin/offerings/offering-editor-page.html`), rappelant que les
allégations thérapeutiques sont interdites.

### D.3 — Écarts de texte, avec la phrase exacte et une reformulation proposée

| # | Phrase exacte | Fichier | Reformulation proposée (non appliquée) | État | Gravité |
| --- | --- | --- | --- | --- | --- |
| D3 | « Réglé en ligne à la réservation » | `fe/public/offerings/offerings-page.html:30` | Supprimer la branche `FULL_ONLINE`. Ne garder que la phrase `ON_SITE`, déjà juste : « Réglé après la séance, par virement, en espèces ou par paiement mobile ». | CONFIRMÉ | Bloquant |
| D4 | « Acompte de {{ offering.depositCents \| euros }} à la réservation » | `fe/public/offerings/offerings-page.html:33-35` | Supprimer la branche `DEPOSIT_ONLINE`, et retirer l'option de l'éditeur (`fe/admin/offerings/admin-offering.ts:22-26`) pour qu'elle ne puisse plus être choisie. | CONFIRMÉ | Bloquant |
| D5 | « Une séance passée est conservée **sans votre nom**, avec sa note, que la loi impose de garder dix ans. » | `fe/account/account-erasure.ts:13-15` | « Une séance passée est conservée avec sa note, que la loi impose de garder dix ans : elle garde le nom qui figurait sur la note, et votre adresse électronique en est retirée. » | CONFIRMÉ | Important |
| D6 | « Deux nombres par jour : les pages vues et les visiteurs. Votre adresse réseau sert seulement à ne pas vous compter deux fois dans la journée ; elle n'est pas enregistrée, et le lien est oublié chaque nuit. Aucun profil, aucun suivi d'un jour sur l'autre. » | `fe/public/legal/confidentialite-page.html:50-55` | Retirer le bloc `<dt>/<dd>` et la ligne correspondante du tableau de conservation (`:85-87`) jusqu'à la livraison de la tranche 5B. Une politique de confidentialité ne décrit que ce qui existe. | CONFIRMÉ | Important |

### D.4 — Confort

| # | Constat | Preuve | État | Gravité |
| --- | --- | --- | --- | --- |
| D7 | Le jeu de données de développement contient « Contact avec un défunt — 1 h » à 90 €, absent du catalogue arrêté avec So et de la spec 3.2, et des prix qui ne sont pas les bons (45 € pour la guidance 30 min, qui est à 50 €). Marqué « Fictional content » et jamais public — mais c'est le genre de texte qui se recopie. | `backend/src/main/resources/db/dev/R__dev_offerings.sql:1`, `5-13` | CONFIRMÉ | Confort |
| D8 | Le même jeu de données utilise `FULL_ONLINE` et `DEPOSIT_ONLINE`, ce qui fait que l'écran de développement montre en permanence un paiement en ligne inexistant — et normalise l'erreur D3/D4. | `R__dev_offerings.sql:7`, `10`, `19` | CONFIRMÉ | Confort |
| D9 | La section « Son parcours » (`JOURNEY`) et la transcription de la vidéo sont volontairement vides, et leur section se masque. C'est le bon comportement : mieux vaut rien qu'un texte que So n'a pas dit. À remplir avec elle. | `fe/public/about/about-content.ts:34-38`, `21-26`, `fe/public/about/about-page.html:13-33` | CONFIRMÉ | Confort |

---

## 6. Questions à poser à So et décisions à prendre

1. **Durées variables.** Annonce-t-on « 30 à 45 min » (il faut une colonne de plus) ou réserve-t-on
   45 min en affichant 45 min ? Même question pour le rééquilibrage, 45 min à 1 h.
2. **Remise de parrainage : quand ?** Accordée **à la réservation** — seule option compatible avec
   l'émission automatique de la note — ou à la clôture, ce qui demande de pouvoir modifier le
   montant avant émission ?
3. **Remise de parrainage : sous quelle forme sur la note ?** Une ligne de remise visible (brut,
   remise, net) ou directement le prix net ? L'arrêté demande un décompte.
4. **Code de parrainage.** Qui le génère, combien de temps est-il valable, est-il utilisable une
   seule fois, et le parrain doit-il avoir eu au moins une séance honorée ?
5. **Question ciblée : canal de réponse.** Enregistre-t-on le choix de la cliente (SMS ou
   Messenger) ? Le pseudo Messenger est-il obligatoire, comme le dit la spec 5.2 ?
6. **Délai de 48 h de la question ciblée.** Calculé en sautant les jours de fermeture, ou simplement
   annoncé « sauf congés » sans calcul ?
7. **Consentements.** Laquelle des options O1 à O4 du volet C ? Et faut-il faire relire les CGV par
   un professionnel avant l'ouverture (recommandé, vu C.3) ?
8. **Domiciliation professionnelle.** À arrêter avant la première note émise : l'adresse y est
   obligatoire et figée (S2).
9. **Médiateur de la consommation.** Toujours à choisir ; les CGV affichent l'avertissement en
   attendant.
10. **Paiement en ligne.** Confirme-t-on qu'il reste hors périmètre ? Si oui, on retire
    `FULL_ONLINE` et `DEPOSIT_ONLINE` de l'interface d'administration et de la page publique (S1).
11. **Vidéos par prestation.** Une par prestation, avec transcription ? Les deux guidances sont
    prêtes ; faut-il attendre les autres pour livrer, ou afficher au cas par cas ?

---

## 7. Ce que je n'ai pas pu vérifier, et pourquoi

- **`deploy/.env` n'a pas été ouvert**, par consigne. Les valeurs réelles de `BILLING_*` et
  `LEGAL_*` ne sont donc pas vérifiées : seuls les défauts de
  `backend/src/main/resources/application.yaml:65-82` le sont. Le constat S2 porte sur le défaut,
  pas sur ce qui est effectivement déployé en recette — il est possible que `.env` soit déjà rempli.
- **Aucun test n'a été exécuté** (`./mvnw verify`, `npx ng test`) : l'audit est en lecture seule.
  La couverture annoncée est établie en lisant les tests, pas en les lançant. Un test peut donc
  exister et échouer, ou être désactivé, sans que je le sache.
- **Le contenu réel de la table `offering` en recette est inconnu.** Le volet D porte sur le
  mécanisme et sur le jeu de données de développement. Les descriptions que So a pu saisir depuis
  l'administration ne sont pas auditées — et ce sont elles qui portent le risque D1/D2. **À relire
  dès que le catalogue réel est saisi.**
- **L'unicité d'usage du lien magique** dépend de `JdbcOneTimeTokenService` (Spring Security), hors
  du dépôt : marqué SUPPOSÉ. La durée de 10 minutes, elle, est dans le code.
- **Rien n'a été vérifié dans un navigateur** : accessibilité réelle (AXE, navigation au clavier,
  contrastes), rendu SSR des pages serveur, comportement sur téléphone. L'objectif WCAG AA n'est
  donc pas évalué ici.
- **Les écrans d'administration n'ont pas été audités** au-delà de ce qui touche le catalogue et la
  facturation : l'agenda, les fiches clientes et les fermetures ont été lus côté backend, mais leurs
  gabarits ne l'ont pas été exhaustivement.
- **Un fichier `deploy/.env.swp`** (brouillon d'éditeur, 12 Ko) traîne dans le répertoire de
  travail. Il est bien ignoré par Git (`.gitignore:20`, vérifié avec `git check-ignore`) et je ne
  l'ai pas ouvert : aucune fuite dans le dépôt. Il mérite tout de même d'être supprimé, puisqu'il
  contient une copie de travail des secrets.
