# SIRA Manager — MASTER ROADMAP

Dernière vérification : 2026-09-08

## Légende
- 🟢 TERMINÉ — vérifié par code + test/build ou vérification runtime.
- 🟡 PARTIEL — fonctionne mais dépend d’une configuration externe ou d’une validation complémentaire.
- 🔴 BLOQUÉ — échec reproductible empêchant le parcours.
- ⚪ À FAIRE — non implémenté ou non vérifié.

## SIRA Business
- 🟢 Structure Android + build debug : une exécution CI a validé tests + APK debug.
- 🟡 Licence : client/serveur présents ; persistance/révocation centrale et test appareil restent à valider.
- 🟡 Ventes / produits / stock / clients / fournisseurs / commandes : composants présents ; parcours complet encore à régressionner sur appareil.
- 🟡 Caisse / reçus / impression Bluetooth : composants présents ; validation matérielle nécessaire.
- 🟡 Hors connexion / synchronisation : stockage local présent ; conflits, reprise réseau et sauvegarde à valider sur appareil.
- 🟡 OTA obligatoire : client + API centralisée en production ; release complète et installation appareil restent à valider.

## SIRA Orange Money
- 🟢 Correction Compose : MainActivity utilise ComponentActivity et les imports/coroutines nécessaires.
- 🟢 Historique local SQLite v2 : type d’opération et contrepartie persistés.
- 🟢 Écrans Portefeuille / Encaisser / Transférer / Historique / Sécurité : intégrés.
- 🟡 Build/test CI : un correctif de compilation est poussé ; nouvelle exécution CI en cours de validation.
- 🟡 Encaissement/transfert réel opérateur : nécessite API/identifiants opérateur et tests terrain.
- 🟡 Licence Orange Money : validation client présente ; contrôle central durable et test appareil à confirmer.
- 🟡 OTA obligatoire : intégration présente ; release complète nécessaire pour validation de bout en bout.

## Administration centrale
- 🟢 Interface Web responsive moderne : intégrée dans `public/index.html` et déployée sur le projet Vercel existant.
- 🟢 Vue d’ensemble, licences, applications, OTA, sécurité : écrans intégrés.
- 🟡 Persistance centrale utilisateurs/boutiques/activité : dépend encore d’un backend durable authentifié.
- 🟡 Authentification administrateur forte : fournisseur d’identité à brancher.
- 🟡 Publication des versions : pipeline GitHub Actions ajouté ; publication réelle encore conditionnée aux secrets de signature.

## OTA / Releases
- 🟢 API `/api/updates/latest` : active en production et testée pour `business` et `orange`.
- 🟢 API OTA : n’utilise qu’une release GitHub complète contenant les deux APK.
- 🟢 Pipeline release : tests Business + Orange, builds release, vérification des deux APK, checksum et publication d’une release unique.
- 🟡 Release GitHub réelle : aucune release n’était publiée au dernier audit.
- 🟡 Signature release : workflow prêt ; secrets GitHub de signature à configurer.
- 🟡 Installation Android de l’APK : logique intégrée ; test appareil requis.

## Déploiement
- 🟢 Projet Vercel `sira-manager-admin` actif.
- 🟢 Déploiement production vérifié : la page admin et `/api/updates/latest` répondent réellement.
- 🟡 Domaine stable : alias de production actif ; validation externe additionnelle recommandée.

## Sécurité
- 🟡 Aucune clé opérateur ne doit être mise dans l’APK ; configuration externe requise.
- 🟢 Secret de signature licence retiré du code serveur ; `SIRA_LICENSE_SECRET` / `SIRA_ADMIN_SECRET` est requis côté serveur.
- 🟡 Permissions, stockage, téléchargement APK et provider Android : audit release/appareil encore requis.

## Critère de finalisation
Le projet ne passe à 🟢 global que lorsque les deux APK release sont publiés et vérifiés dans une même GitHub Release, l’API OTA répond en production avec ces APK, le parcours licence est testé sur appareil et le panneau admin dispose de son authentification/persistance centrale réelles.
