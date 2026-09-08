# SIRA Manager — MASTER ROADMAP

Dernière vérification : 2026-09-08

## Légende
- 🟢 TERMINÉ — vérifié par code + test/build ou vérification runtime.
- 🟡 PARTIEL — fonctionne mais dépend d’une configuration externe ou d’une validation complémentaire.
- 🔴 BLOQUÉ — échec reproductible empêchant le parcours.
- ⚪ À FAIRE — non implémenté ou non vérifié.

## SIRA Business
- 🟢 Structure Android + build debug : vérifiée par CI.
- 🟡 Licence : client/serveur présents, validation en conditions réelles à confirmer.
- 🟡 Ventes / produits / stock / clients / fournisseurs / commandes : composants présents, parcours complet encore à régressionner sur appareil.
- 🟡 Caisse / reçus / impression Bluetooth : composants présents, validation matérielle nécessaire.
- 🟡 Hors connexion / synchronisation : stockage local présent, conflits et reprise réseau à valider sur appareil.
- 🟡 OTA obligatoire : client présent et route centralisée codée, déploiement Vercel + release complète à valider.

## SIRA Orange Money
- 🟢 Correction du blocage Compose : MainActivity utilise désormais ComponentActivity.
- 🟢 Historique local avec migration SQLite v2 : type d’opération et contrepartie persistés.
- 🟢 Écrans Portefeuille / Encaisser / Transférer / Historique / Sécurité : intégrés dans l’UI.
- 🟡 Encaissement/transfert réel opérateur : nécessite API/identifiants opérateur et tests terrain.
- 🟡 Licence Orange Money : client de validation présent, endpoint production à confirmer.
- 🟡 OTA obligatoire : intégration présente, release complète nécessaire pour validation de bout en bout.

## Administration centrale
- 🟢 Interface Web responsive moderne : intégrée dans `public/index.html`.
- 🟢 Vue d’ensemble, licences, applications, OTA, activité, sécurité : écrans intégrés.
- 🟡 Persistance centrale utilisateurs/boutiques/activité : dépend d’un backend durable authentifié, non simulé côté navigateur.
- 🟡 Authentification administrateur forte : à brancher sur le fournisseur d’identité retenu.
- 🟡 Gestion complète des versions/releases depuis l’interface : lecture OTA intégrée, publication encore portée par GitHub Actions.

## OTA / Releases
- 🟢 API `/api/updates/latest` : validation de l’application, comparaison de versions et sélection d’APK.
- 🟢 API OTA : sélection uniquement d’une release GitHub contenant les deux APK.
- 🟡 Release GitHub réelle : aucune release publiée au moment de la vérification.
- 🟡 Signature release : workflow prévu, secrets de signature à vérifier dans l’environnement GitHub.
- 🟡 Installation Android de l’APK : logique intégrée, test appareil requis.

## Déploiement
- 🟡 Projet Vercel `sira-manager-admin` existant et actif.
- 🔴 Au moment de l’audit, le domaine de production ne servait pas `/api/updates/latest` (HTTP 404) et le projet Vercel n’était pas relié au dépôt GitHub.
- 🟡 Déploiement de la version du dépôt sur le projet Vercel : à revalider après publication effective.

## Sécurité
- 🟡 Aucune clé opérateur ne doit être mise dans l’APK ; configuration externe requise.
- 🔴 La signature licence V2 utilise encore un secret statique dans le code serveur ; à migrer vers une variable d’environnement avant production.
- 🟡 Permissions, stockage, téléchargement APK et provider Android : à auditer sur appareil release.

## Critère de finalisation
Le projet ne passe à 🟢 global que lorsque les deux APK release sont publiés dans une même GitHub Release, l’API OTA répond en production, le parcours licence est testé sur appareil et le panneau admin est déployé depuis cette même base de code.
