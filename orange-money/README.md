# SIRA Orange Money — application indépendante

Cette application est séparée de SIRA Business tout en étant administrée depuis le même centre de contrôle.

## Principe

- APK indépendante : identité, données locales et cycle de vie séparés.
- Même panel administrateur SIRA : licences, activation, suspension, configuration et commandes distantes.
- Base locale dédiée : aucune lecture directe de la base SIRA Business.
- Fonctionnement hors ligne pour l'historique, les calculs de commissions, les soldes et les rapports disponibles localement.
- Synchronisation cloud facultative lorsque le réseau revient.

## Modèle de licence

`SIRA-OM-XXXX-XXXX-XXXX`

La licence porte les capacités préconfigurées par l'administrateur :

- opérations Orange Money
- calcul des commissions
- caisse / solde
- historiques
- rapports journaliers
- limites utilisateurs/appareils
- thème et identité de l'application
- modèle d'affichage Essentiel / Standard / Complet

## Architecture

`Panel SIRA Admin -> Control Plane commun -> API Orange Money -> APK SIRA Orange Money`

Le secret d'un fournisseur de paiement ne doit jamais être embarqué dans l'APK.

## Règle UX

Aucun écran de configuration technique pour l'opérateur. L'administrateur préconfigure le produit depuis le panel et le commerçant voit uniquement les actions utiles.