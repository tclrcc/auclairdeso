# Au clair de So — Spécification fonctionnelle

> Document de référence du comportement attendu de l'application. **À lire avant de développer toute fonctionnalité métier.**
>
> Les décisions viennent des échanges avec la praticienne (octobre 2026). Les points marqués **[À confirmer]** sont des hypothèses retenues en attendant sa réponse : on les applique, mais on les garde faciles à changer (configuration plutôt que constantes enfouies dans le code).

| Version | Date | Changement |
| --- | --- | --- |
| 1 | 2026-10-07 | Première version, après l'entretien avec la praticienne |

---

## 1. Contexte

| Élément | Valeur |
| --- | --- |
| Activité | Médium en micro-entreprise, en activité secondaire à côté d'un emploi salarié à temps plein |
| SIRET | 922 896 626 00012 |
| Lieu | Chazey-sur-Ain (01) |
| Clientèle | France uniquement, personnes majeures |
| Fuseau horaire | Europe/Paris, pour tout l'affichage et tous les calculs |
| Image | Médiumnité moderne, sans clichés de « voyante » |
| Mise en avant | La **voyance** d'abord, le magnétisme ensuite, le rééquilibrage énergétique |
| Usage de l'administration | Principalement depuis un **téléphone** : tous les écrans d'administration sont conçus pour mobile d'abord |
| Agenda actuel | Google Agenda, sans lien avec l'application (pas de synchronisation demandée) |

Objectifs du site : vitrine, prise de rendez-vous en ligne, vidéos de présentation, identité visuelle propre. Les réseaux sociaux (Instagram, Facebook) restent alimentés en parallèle.

---

## 2. Rôles et connexion

| Rôle | Qui | Accès |
| --- | --- | --- |
| `CLIENT` | Toute personne qui se connecte | Réserver, voir et annuler ses rendez-vous |
| `PRACTITIONER` | La praticienne | Toute l'administration |
| `ADMIN` | Le développeur | Toute l'administration + réinitialisation des mots de passe de l'équipe |

- Les rôles du personnel sont attribués par la **configuration** (`AUCLAIRDESO_IDENTITY_ADMINS`, `AUCLAIRDESO_IDENTITY_PRACTITIONERS`), jamais depuis l'interface.
- **Clientes** : connexion par lien magique envoyé par email (valable 10 minutes, usage unique). Le compte est créé à la première connexion.
- **Personnel** : lien magique **et** mot de passe pour accéder à `/admin` (double authentification). Pas de « mot de passe oublié » par email : un `ADMIN` efface le mot de passe, qui est redéfini à la connexion suivante.
- Évolution envisagée : clé d'accès (*passkey*, empreinte ou Face ID) pour simplifier la connexion de la praticienne sur son téléphone.

---

## 3. Prestations

### 3.1 Catégories

| Catégorie | Place sur le site |
| --- | --- |
| Voyance | Principale, mise en avant |
| Magnétisme | Secondaire |
| Rééquilibrage énergétique | Secondaire, avec des forfaits plus tard |

### 3.2 Prestations connues

La liste complète (noms, durées, prix, descriptions) sera fournie par la praticienne et saisie depuis l'administration.

| Prestation | Catégorie | Prix | Particularités |
| --- | --- | --- | --- |
| Question ciblée | Voyance | 15 € | **Sans créneau horaire**, réponse via Messenger ou SMS |
| Consultation complète 30 min | Voyance | 50 € | |
| Consultation approfondie 1 h | Voyance | 80 € | |
| Tirage de cartes | Voyance | à préciser | Toujours à distance |
| Coupage de feu | Magnétisme | à préciser | À distance ou en présence (surtout en présence) |
| Tendinite, eczéma / urticaire, zona, hernie discale / lombalgie, abcès dentaires | Magnétisme | à préciser | À distance ou en présence |
| Rééquilibrage énergétique | Rééquilibrage | à préciser | Forfaits à définir plus tard |

### 3.3 Règles

- Le **prix est le même** quel que soit le mode de consultation.
- Chaque prestation définit sa **durée** et sa **pause après la séance** :
  - 30 minutes de pause après une séance de 30 minutes ;
  - 15 minutes de pause après une séance d'1 heure.
- Les prestations de **voyance exigent une photo** de la cliente (voir 5.2).
- Une prestation n'est jamais supprimée, seulement **masquée** (les rendez-vous passés y font référence).
- Le **slug** (adresse publique) est calculé à partir du nom à la création, puis ne change plus.

### 3.4 Modes de consultation

| Mode | Code | Règles |
| --- | --- | --- |
| Chez la praticienne | `IN_PERSON` | Réservé aux clientes **de confiance** **[À confirmer : marquage « de confiance » dans la fiche cliente, seules ces clientes voient ce mode]**. L'adresse n'est **jamais affichée** sur le site : elle figure seulement dans l'email de confirmation. |
| Chez la cliente | `CLIENT_HOME` | Dans un rayon de **20 km autour de Chazey-sur-Ain**. Adresse complète demandée. La distance est vérifiée par la praticienne lors de la validation (calcul automatique plus tard). Frais de déplacement : plus tard. |
| Visio | `VIDEO` | Par **Messenger** (Instagram à terme). Le pseudo Messenger de la cliente est demandé. |
| Téléphone | `PHONE` | **La cliente appelle la praticienne** à l'heure du rendez-vous. Le numéro à appeler figure dans l'email de confirmation. |

Il n'y a **pas** de séance par écrit.

### 3.5 Paiement

- **Aucun paiement en ligne** pour l'instant (pas de carte, pas d'acompte, pas de frais).
- La cliente paie **après la séance**, par **virement** ou en **espèces**.
- La praticienne marque chaque séance honorée comme **payée** (montant, moyen de paiement). Ces informations alimentent les bilans et le livre des recettes (voir 8 et 10).
- Le champ `payment_policy` du catalogue est conservé (valeur `ON_SITE`) en prévision d'un éventuel paiement en ligne.
- Pas de factures automatiques (voir la vérification en 10).

---

## 4. Planning

### 4.1 Plages d'ouverture

| Jour | Horaires |
| --- | --- |
| Lundi à jeudi | 14 h – 18 h |
| Vendredi à dimanche | Fermé |

Les plages sont stockées en base (`opening_hours`), modifiables plus tard depuis l'administration.

### 4.2 Calcul des créneaux

Dans chaque plage, les créneaux **s'enchaînent depuis l'heure d'ouverture** : chaque créneau commence quand le précédent et sa pause sont terminés. Quand une séance déjà réservée est sur le chemin, l'enchaînement reprend juste après cette séance et sa pause. Une séance doit **se terminer** avant la fermeture ; sa pause peut déborder.

Exemples d'un lundi vide :

| Prestation | Créneaux proposés |
| --- | --- |
| 1 h (pause 15 min) | 14 h 00, 15 h 15, 16 h 30 |
| 30 min (pause 30 min) | 14 h 00, 15 h 00, 16 h 00, 17 h 00 |

Exemple avec une séance de 30 min déjà réservée à 14 h : une prestation d'1 h propose 15 h 00 et 16 h 15.

### 4.3 Règles de réservation

| Règle | Valeur | Paramètre |
| --- | --- | --- |
| Séances par jour | 3 au maximum **[À confirmer : quelle que soit la durée]** | `auclairdeso.booking.max-sessions-per-day` |
| Réservation au plus tôt | Le **lendemain** (jamais le jour même) | `auclairdeso.booking.min-days-ahead` |
| Réservation au plus tard | **2 mois** à l'avance | `auclairdeso.booking.horizon` |
| Fermetures (vacances, maladie) | Jours entiers, saisis dans l'administration | table `closure` |
| Consultations urgentes | Saisies **à la main** par la praticienne, y compris en dehors des plages | — |

- Une demande **en attente** bloque son créneau, comme une séance confirmée.
- La **question ciblée** n'occupe pas de créneau et ne compte pas dans le maximum journalier **[À confirmer]**.
- Deux séances ne peuvent jamais se chevaucher : la base l'interdit (contrainte d'exclusion PostgreSQL sur les rendez-vous en attente ou confirmés).

---

## 5. Réservation

### 5.1 Parcours de la cliente

1. Elle choisit une prestation, un mode de consultation, puis un créneau.
2. Elle se connecte par lien magique (si ce n'est pas déjà fait).
3. Elle remplit le formulaire (voir 5.2), envoie sa photo si la prestation l'exige.
4. Elle coche les consentements : majorité, conditions générales, traitement de ses données.
5. La demande est enregistrée **en attente** ; la praticienne est prévenue.
6. La praticienne **confirme ou refuse** chaque demande. La cliente reçoit un email dans les deux cas.

La cliente **ne peut pas déplacer** un rendez-vous : elle annule et fait une nouvelle demande.

### 5.2 Informations demandées

| Information | Obligatoire | Remarque |
| --- | --- | --- |
| Nom, prénom | Oui | |
| Date de naissance | Oui | Doit attester de la **majorité** (18 ans) |
| Téléphone | Oui | |
| Ville | Non | |
| Motif de la réservation | Oui | Peut contenir des informations de santé : donnée sensible |
| Photo portrait | Oui pour la voyance | Visage visible, sans lunettes de soleil |
| Adresse complète | Oui pour `CLIENT_HOME` | |
| Pseudo Messenger | Oui pour `VIDEO` et la question ciblée | |

### 5.3 Règles d'éligibilité

- Une cliente ne peut avoir **qu'une seule réservation à venir** (en attente ou confirmée).
- Une cliente **bloquée** (voir 6) ne peut pas réserver. Elle voit un message neutre l'invitant à contacter directement la praticienne **[À confirmer]**.
- Pas de mineurs.

### 5.4 Question ciblée

- Pas de créneau : la cliente envoie sa question (et sa photo).
- La praticienne valide la demande, puis répond via **Messenger ou SMS**, selon le choix de la cliente.
- Délai de réponse annoncé : **48 h [À confirmer]**.

### 5.5 Statuts d'un rendez-vous

| Statut | Signification | Bloque le créneau |
| --- | --- | --- |
| `REQUESTED` | Demande en attente de la décision de la praticienne | Oui |
| `CONFIRMED` | Acceptée | Oui |
| `DECLINED` | Refusée par la praticienne | Non |
| `EXPIRED` | Non traitée à temps **[À confirmer : la veille de la séance à 21 h]**, la cliente est prévenue | Non |
| `CANCELLED` | Annulée, par la cliente (à temps ou tardivement) ou par la praticienne | Non |
| `COMPLETED` | Séance honorée (puis marquée payée) | — |
| `NO_SHOW` | La cliente ne s'est pas présentée | — |

---

## 6. Annulations, absences et liste noire

| Situation | Conséquence |
| --- | --- |
| La cliente annule **48 h ou plus** avant | Annulation libre, sans conséquence |
| La cliente annule **moins de 48 h** avant | Possible dans l'application, avec un avertissement. Comptée comme un **manquement**. |
| **2 manquements** | La cliente est **bloquée** automatiquement |
| La cliente **ne vient pas** | Marquée `NO_SHOW` par la praticienne, cliente **bloquée** immédiatement |
| La praticienne annule | Elle appelle la cliente ; l'application enregistre l'annulation et envoie un email |

- La praticienne peut **débloquer** une cliente depuis sa fiche.
- Le blocage s'appuie sur le compte (email) et sur le numéro de téléphone.
- La liste noire doit être mentionnée dans la politique de confidentialité et les conditions générales.

---

## 7. Notifications

| Destinataire | Contenu | Canal | Moment |
| --- | --- | --- | --- |
| Praticienne | Nouvelle demande | Email et/ou SMS (réglable dans les paramètres) | Immédiatement |
| Praticienne | Programme du lendemain (séances confirmées, demandes en attente) | Email | Chaque soir **[À confirmer : 19 h]** |
| Cliente | Lien de connexion | Email | À la demande |
| Cliente | Confirmation, avec adresse ou numéro à appeler selon le mode | Email | À la confirmation |
| Cliente | Refus, expiration ou annulation | Email | À l'événement |
| Cliente | Rappel | Email | **La veille à 12 h**, pour toute séance confirmée |

- Expéditeur : `auclairdeso@gmail.com`, envoyé par le serveur SMTP de Gmail avec un mot de passe d'application, en attendant un nom de domaine.
- SMS : fournisseur à choisir selon l'opérateur de la praticienne (API gratuite de Free Mobile si elle est chez Free, sinon Brevo, environ 5 centimes par SMS).

---

## 8. Administration

Réservée au personnel, conçue pour mobile.

| Écran | Contenu |
| --- | --- |
| Agenda | Demandes en attente en tête, puis séances à venir par jour. Actions : confirmer, refuser, annuler, marquer honorée / absente / payée. |
| Saisie manuelle | Créer un rendez-vous pris par téléphone ou Messenger, y compris en dehors des plages (urgences) |
| Fermetures | Ajouter et supprimer des jours de fermeture |
| Clientes | Liste, fiche (coordonnées, historique), **notes privées**, marquage « de confiance », blocage et déblocage |
| Séances | Créer, modifier, masquer les prestations (**fait**) |
| Statistiques | Voir ci-dessous |
| Encaissements | Séances payées, bilans, **livre des recettes** exportable |
| Équipe | Membres, réinitialisation des mots de passe par l'`ADMIN` (**fait**) |
| Paramètres | Notifications de la praticienne : email, SMS |

### Statistiques

Visibles par le personnel uniquement **[À confirmer : la praticienne et l'administrateur]**.

| Indicateur | Périodes |
| --- | --- |
| Visiteurs du site | Jour, semaine, mois |
| Rendez-vous demandés | Jour, semaine, mois |
| Rendez-vous « abandonnés » : refusés, expirés, annulés **[À confirmer : définition]** | Jour, semaine, mois |
| Rendez-vous honorés | Jour, semaine, mois |
| Rendez-vous par prestation et par durée | Mois, année |
| Chiffre d'affaires encaissé, par moyen de paiement | Mois, année |

La mesure d'audience doit respecter la vie privée (sans cookie de suivi, pour éviter un bandeau de consentement).

---

## 9. Données personnelles (RGPD)

Plusieurs données sont **sensibles** : motifs de consultation (parfois liés à la santé), photos, notes de la praticienne.

| Donnée | Accès | Conservation |
| --- | --- | --- |
| Compte (email, rôle, dates de connexion) | Personnel | Tant que le compte existe |
| Coordonnées et date de naissance | Personnel | Tant que le compte existe |
| Motif de consultation | Personnel | Avec le rendez-vous |
| Photo | Praticienne et administrateur | Supprimée **7 jours après la séance [À confirmer]** |
| Notes privées sur une cliente | Personnel | Tant que le compte existe |
| Liste noire | Personnel | Tant que le compte existe |

- Consentement explicite à la réservation pour le traitement des données sensibles.
- Collecte minimale : rien de plus que le tableau 5.2.
- Photos stockées hors de toute zone publique, jamais servies sans authentification.
- La cliente peut demander la suppression de son compte et de ses données.

---

## 10. Obligations légales et contenu

| Sujet | État |
| --- | --- |
| Mentions légales (SIRET, adresse professionnelle, hébergeur OVH) | À rédiger ; l'adresse du domicile ne doit pas apparaître (domiciliation à envisager) |
| Conditions générales (annulation 48 h, liste noire, majorité, paiement après séance) | À rédiger |
| Politique de confidentialité | À rédiger |
| Médiateur de la consommation | **Obligatoire** pour vendre à des particuliers ; à choisir |
| Avertissement sur la nature des séances | « Accompagnement complémentaire, qui ne remplace pas un avis ni un traitement médical ». Aucune promesse de guérison pour le magnétisme (allégations thérapeutiques interdites). |
| Livre des recettes | Obligatoire en micro-entreprise ; généré par l'application à partir des séances payées |
| Note pour une prestation de 25 € ou plus | **[À vérifier auprès de l'URSSAF ou de la CCI]** |
| Nom de domaine | À acheter (par exemple `auclairdeso.fr`), indispensable pour le HTTPS |
| Contenu de la vitrine (textes, photos, vidéos, témoignages) | Plus tard |

---

## 11. Hypothèses à confirmer

| # | Question | Hypothèse retenue |
| --- | --- | --- |
| 1 | « 3 séances maximum » vaut-il quelle que soit la durée ? | Oui, 3 séances quelle que soit leur durée |
| 2 | Comment réserver les séances chez la praticienne aux clientes de confiance ? | Marquage « de confiance » dans la fiche cliente ; seules ces clientes voient ce mode |
| 3 | Vérification des 20 km pour les séances chez la cliente | Manuelle, par la praticienne, à la validation |
| 4 | Délai d'expiration d'une demande non traitée | La veille de la séance à 21 h |
| 5 | Question ciblée : délai de réponse, maximum journalier, photo | Réponse sous 48 h, hors maximum journalier, photo obligatoire |
| 6 | Rendez-vous pris par téléphone ou Messenger | Saisis dans l'application par la praticienne |
| 7 | Vacances | Fermetures par jours entiers |
| 8 | Message affiché à une cliente bloquée | Message neutre invitant à contacter directement la praticienne |
| 9 | Conservation des photos | 7 jours après la séance |
| 10 | Heure du programme du soir | 19 h |
| 11 | Opérateur mobile de la praticienne (pour les SMS) | Inconnu |
| 12 | Rendez-vous visibles aussi dans Google Agenda | Non ; lien d'abonnement iCal possible plus tard |
| 13 | Définition d'un rendez-vous « abandonné » | Refusé, expiré ou annulé |
| 14 | Statistiques visibles par qui | Tout le personnel (praticienne et administrateur) |

---

## 12. Feuille de route

| Tranche | Contenu | État |
| --- | --- | --- |
| 0 | Dépôt, squelettes, CI | Fait |
| 1 | Catalogue des prestations, page « Séances et tarifs » | Fait |
| 2 | Comptes, lien magique, double authentification, administration des séances, sessions en base, limitation des demandes | Fait |
| 3A | Planning : plages, fermetures, calcul des créneaux | En cours |
| 3B | Demande de réservation par la cliente : formulaire, photo, question ciblée, éligibilité | À faire |
| 3C | Agenda et validation par la praticienne, saisie manuelle, annulations, absences, liste noire | À faire |
| 3D | Notifications : emails, SMS, rappel de la veille, programme du soir, expiration | À faire |
| 4 | Fiches clientes, notes privées, marquage « de confiance » | À faire |
| 5 | Statistiques, encaissements, livre des recettes | À faire |
| 6 | Vitrine : contenu, identité visuelle, vidéos, pages légales | À faire |
| 7 | Mise en production : nom de domaine, HTTPS, Docker, sauvegardes, Ubuntu 26.04 | À faire |

## 13. Hors périmètre pour l'instant

- Paiement en ligne (Stripe), acomptes, cartes cadeaux.
- Forfaits de rééquilibrage énergétique.
- Frais de déplacement et calcul automatique de la distance.
- Synchronisation avec Google Agenda.
- Messagerie Instagram / Messenger intégrée.
- Clientes à l'étranger et autres fuseaux horaires.
- Clé d'accès (*passkey*) pour le personnel.
