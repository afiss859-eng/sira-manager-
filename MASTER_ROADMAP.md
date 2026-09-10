# SIRA Manager — MASTER ROADMAP

Dernière vérification : 2026-09-10

## Légende
- 🟢 TERMINÉ — vérifié par code + test/build ou vérification runtime.
- 🟡 PARTIEL — fonctionne mais dépend d’une configuration externe ou d’une validation complémentaire.
- 🔴 BLOQUÉ — échec reproductible empêchant le parcours.
- ⚪ À FAIRE — non implémenté ou non vérifié.

## SIRA Business
- 🟢 Structure Android + build debug : CI valide tests + APK debug.
- 🟡 Licence : signature + expiration serveur présentes ; quota multi-appareils et révocation centrale restent à persister.
- 🟡 Ventes / produits / stock / clients / fournisseurs / commandes : composants présents ; parcours complet encore à régressionner sur appareil.
- 🟡 Caisse / reçus / impression Bluetooth : composants présents ; validation matérielle nécessaire.
- 🟡 Hors connexion / synchronisation : stockage local présent ; conflits, reprise réseau et sauvegarde à valider sur appareil.
- 🟡 OTA obligatoire : client + API centralisée présents ; release complète et installation appareil restent à valider.

## SIRA Orange Money
- 🟢 Correction Compose + tests unitaires : vérifiés par CI.
- 🟢 Historique local SQLite v2 et écrans Portefeuille / Encaisser / Transférer / Historique / Sécurité : intégrés.
- 🟢 Build debug : CI valide tests + APK debug.
- 🟡 Encaissement/transfert réel opérateur : nécessite API/identifiants opérateur et tests terrain.
- 🟡 Licence Orange Money : validation client présente ; contrôle central durable et test appareil restent à finaliser.
- 🟡 OTA obligatoire : intégration présente ; release complète nécessaire pour validation de bout en bout.

## Administration centrale
- 🟢 Interface Web responsive moderne : `public/admin.html`.
- 🟢 Connexion admin : `public/admin-login.html` + session signée HttpOnly de 8 h.
- 🟢 Génération de licence protégée : `/api/licenses/create` exige une session admin.
- 🟢 Vue d’ensemble, licences, applications, OTA, sécurité : écrans intégrés.
- 🟡 Persistance centrale des licences/utilisateurs/boutiques/activité : stockage durable à brancher.
- 🟡 Révocation persistante et quotas multi-appareils : non simulés tant que le stockage central n’est pas branché.

## Licences / sécurité
- 🟢 Secret de licence côté serveur uniquement.
- 🟢 Expiration signée + refus serveur des licences expirées.
- 🟢 Session admin signée HMAC, cookie HttpOnly/Secure/SameSite=Strict, expiration 8 h.
- 🟢 Tests unitaires licence + session admin inclus dans CI.
- 🟡 MFA / fournisseur d’identité externe : non branché.

## OTA / Releases
- 🟢 API `/api/updates/latest` active et conçue pour sélectionner uniquement une release contenant les deux APK.
- 🟢 Pipeline release dual-APK : tests/build/signature/publication préparés.
- 🟡 Release GitHub réelle : aucune release complète n’a encore été vérifiée comme publiée.
- 🟡 Signature release : secrets GitHub de keystore à configurer.
- 🟡 Installation Android de l’APK : logique intégrée ; test appareil requis.

## Déploiement
- 🟢 Projet Vercel `sira-manager-admin` actif.
- 🟡 La version actuellement commitée nécessite une nouvelle publication Vercel pour apparaître sur le domaine de production.
- 🟡 Déploiement GitHub automatique : workflow CI retiré tant que les identifiants Vercel ne sont pas configurés, afin d’éviter les faux échecs.

## Critère de finalisation globale
Le projet passe à 🟢 global lorsque les deux APK release signés sont publiés et vérifiés dans une même GitHub Release, l’API OTA répond en production avec ces APK, le parcours licence est testé sur appareil, et le stockage central des licences/révocations/quota ainsi que l’authentification admin renforcée sont opérationnels.
