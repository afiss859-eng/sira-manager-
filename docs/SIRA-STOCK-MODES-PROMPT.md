# Prompt d’implémentation — SIRA Stock 3 niveaux

## Rôle
Tu es l’architecte produit et ingénieur senior de SIRA Manager. Fais évoluer le moteur de stock sans alourdir l’expérience utilisateur.

## Vision non négociable
SIRA doit avoir **un moteur multifonction puissant derrière une interface légère**. L’utilisateur final ne doit pas être confronté à des centaines de paramètres. Toute la complexité technique, les règles de stock et les fonctions avancées sont déterminées à la création/configuration de l’API par l’administrateur SIRA.

Principe : **« Utiliser la traçabilité des grands ERP sans imposer leur complexité. »**

Ne pas créer un écran de configuration interminable. Utiliser des profils prédéfinis, des valeurs sûres et des fonctions activées automatiquement selon le niveau choisi.

---

## 1. Les trois modèles SIRA

### MODE BOUTIQUE
Pour petit commerce, boutique, kiosque, magasin, vendeur indépendant.

Activer automatiquement :
- produits et catégories ;
- stock simple ;
- entrées/sorties ;
- ventes et achats ;
- scanner code-barres/QR ;
- recherche rapide ;
- inventaire simple ;
- alertes de stock faible ;
- fournisseurs/clients ;
- tickets et impression Bluetooth ;
- reçus et proformas ;
- fonctionnement offline-first ;
- synchronisation cloud ;
- historique des mouvements essentiels ;
- prix d’achat, prix de vente, marge simple ;
- retours et pertes simples.

Interface : quelques actions visibles, aucun menu logistique complexe.

### MODE ENTREPÔT NATIONAL
Pour entreprise avec plusieurs magasins, dépôts et flux importants à l’échelle nationale.

Inclure tout le Mode Boutique + :
- multi-boutiques ;
- multi-entrepôts ;
- emplacements internes ;
- transferts inter-sites ;
- stock physique, réservé, disponible et en transit ;
- lots et numéros de série ;
- dates d’expiration ;
- FIFO/FEFO ;
- inventaire cyclique ;
- réapprovisionnement ;
- seuils minimum/maximum ;
- suggestions de commande ;
- réception et contrôle des marchandises ;
- préparation/picking ;
- retours fournisseurs ;
- stock dormant et surstock ;
- coûts d’approvisionnement ;
- traçabilité complète des mouvements ;
- rôles employés/magasinier/manager ;
- tableaux de bord multi-sites ;
- import/export en masse ;
- synchronisation robuste et reprise après coupure réseau.

Interface : les fonctions avancées apparaissent uniquement lorsque le contexte le justifie.

### MODE ENTREPÔT INTERNATIONAL
Pour flux nationaux + import/export, réseaux multi-pays et volumes élevés.

Inclure tout le Mode National + :
- multi-pays et multi-devises ;
- unités et conversions ;
- import/export ;
- coûts de transport et frais d’approvisionnement ;
- coût réel/landed cost ;
- lots/séries et traçabilité renforcée ;
- transferts internationaux ;
- documentation logistique ;
- règles de stock par pays/site ;
- prévisions de demande ;
- réapprovisionnement intelligent ;
- analyse de rotation ;
- stock dormant ;
- simulation d’achat ;
- automatisations ;
- API/intégrations partenaires ;
- audit complet ;
- analytique avancée ;
- capacités adaptées aux gros volumes.

Ne pas afficher ces fonctions aux utilisateurs du Mode Boutique.

---

## 2. Principe de configuration côté administrateur

Lors de la création de l’API / licence / organisation, l’administrateur choisit **un seul modèle** :

`BOUTIQUE | NATIONAL | INTERNATIONAL`

Cette sélection détermine les capacités disponibles côté serveur et côté application.

L’utilisateur final ne choisit pas librement des dizaines de paramètres.

Le serveur doit exposer un objet de capacités compact, par exemple :

```json
{
  "stockModel": "BOUTIQUE",
  "features": {
    "barcode": true,
    "multiStore": false,
    "multiWarehouse": false,
    "locations": false,
    "lots": false,
    "serialNumbers": false,
    "expiry": false,
    "transfers": false,
    "purchaseSuggestions": false,
    "forecasting": false,
    "international": false,
    "advancedAudit": false
  }
}
```

Ne pas permettre au client de s’octroyer des droits en modifiant le JSON local. Les capacités doivent être contrôlées côté serveur et vérifiées côté APK.

---

## 3. Moteur de stock commun aux trois niveaux

Créer une abstraction unique de mouvement :

```text
STOCK_IN
STOCK_OUT
TRANSFER
RESERVATION
RELEASE
RETURN
ADJUSTMENT
DAMAGE
LOSS
RECEIPT
SHIPMENT
```

Chaque mouvement doit conserver au minimum :
- organisation/commerce ;
- produit ;
- quantité ;
- unité ;
- source ;
- destination ;
- utilisateur/appareil ;
- date/heure ;
- référence de document ;
- raison ;
- identifiant unique/idempotency key.

Le stock affiché doit être dérivé des mouvements et permettre de distinguer :

`Physique - Réservé = Disponible`

avec le stock en transit séparé.

Ne pas multiplier les tables métier sans nécessité ; privilégier un moteur central et des vues adaptées au niveau.

---

## 4. Fonctionnement de masse

Concevoir le système pour plusieurs milliers de produits et de gros volumes de mouvements sans ralentir l’interface.

Règles :
- pagination systématique ;
- recherche indexée ;
- traitement par lots ;
- synchronisation incrémentale ;
- idempotence ;
- file offline de mouvements ;
- déduplication ;
- écritures transactionnelles ;
- agrégats/KPI séparés des historiques lourds ;
- ne jamais charger tout le stock en mémoire sur mobile ;
- ne jamais recalculer tout l’historique lors d’une vente simple ;
- pré-calculer les indicateurs utiles.

Pour les imports massifs : utiliser une file de traitement avec progression, succès/échecs par ligne et reprise.

---

## 5. SIRA Copilote IA

Brancher le copilote sur l’API IA existante de SIRA, côté serveur uniquement.

Variables prévues :
- `AI_MODEL_API_KEY`
- `AI_MODEL_BASE_URL`

Ne jamais exposer la clé dans l’APK, le navigateur ou le dépôt.

Le copilote doit être conscient du **stockModel** et des permissions de l’utilisateur.

Exemples de capacités :
- « Quels produits vont manquer bientôt ? »
- « Quels produits sont dormants ? »
- « Pourquoi ma marge baisse ? »
- « Prépare une proposition de commande. »
- « Analyse les ventes des 30 derniers jours. »
- « Donne-moi les écarts d’inventaire. »
- « Crée une proforma avec ces produits. »
- « Explique cette différence de stock. »

L’IA ne doit pas inventer des chiffres : elle doit lire les données SIRA autorisées et signaler lorsqu’une donnée est absente.

Les actions sensibles doivent demander confirmation avant écriture : commande, suppression, ajustement massif, modification de prix, etc.

---

## 6. UI/UX

Conserver le design Apple/SIRA déjà en place : clair, sobre, rapide, mobile-first.

Réduire les paramètres visibles.

L’écran Stock doit privilégier :
- recherche ;
- scan ;
- état du stock ;
- action rapide ;
- alerte ;
- mouvement récent.

Les fonctions avancées doivent apparaître progressivement et uniquement pour les modèles qui les supportent.

Exemple :

`Boutique → Stock → Scanner → Vente`

`National → Stock → Entrepôts → Transfert / Inventaire / Réapprovisionnement`

`International → Stock → Logistique → Import/Export / Coûts / Traçabilité`

---

## 7. Nouvelles fonctions prioritaires

Ajouter ou préparer :

1. Stock Radar : normal / à surveiller / rupture / surstock / dormant.
2. Dead Stock : détection des produits sans mouvement.
3. Réapprovisionnement intelligent.
4. Transfert recommandé entre sites.
5. Inventaire cyclique.
6. FEFO/FIFO selon le modèle.
7. Landed cost au niveau international.
8. Scan rapide code-barres/QR.
9. Offline-first + synchronisation fiable.
10. Import/export massif.
11. Audit et traçabilité.
12. SIRA Copilote IA.
13. Proforma et documents commerciaux.
14. Notifications utiles, sans spam.
15. Statistiques adaptées au niveau choisi.

---

## 8. Paramètres : politique stricte

Il faut **interdire la prolifération des paramètres**.

Maximum visible pour l’administrateur : quelques choix structurants :

- Modèle : Boutique / National / International ;
- devise principale ;
- pays ;
- règles de notifications simples ;
- éventuellement quelques valeurs métier essentielles.

Tout le reste doit utiliser des valeurs par défaut sûres.

Les paramètres techniques (index, batch size, retry, cache, synchronisation, règles internes, permissions fines) restent côté système et ne sont pas exposés à l’utilisateur final.

---

## 9. Sécurité et isolation

Chaque mouvement et chaque donnée doivent être liés à l’organisation/commerce.

Un utilisateur ne doit jamais pouvoir accéder aux données d’une autre organisation en modifiant un identifiant côté mobile.

Les permissions doivent être vérifiées côté serveur.

Toutes les actions sensibles doivent laisser une trace d’audit selon le modèle activé.

---

## 10. Compatibilité avec l’existant SIRA

Ne pas casser :
- scanner CameraX/ML Kit ;
- ventes ;
- stock actuel ;
- caisse ;
- clients ;
- reçus ;
- impression Bluetooth ;
- synchronisation ;
- licences et contrôle à distance ;
- écran Proforma ;
- slider de niveau de détail.

Le niveau de détail UI et le stockModel sont deux notions différentes :
- `stockModel` = capacités autorisées par l’administrateur ;
- `detailLevel` = quantité d’information affichée à l’utilisateur.

---

## 11. Résultat attendu

À la fin, SIRA doit donner l’impression d’être une application extrêmement simple, alors que son moteur peut gérer progressivement :

`Boutique → Entrepôt national → Entrepôt international`

Le produit doit être **configuré par profil**, pas par une jungle de paramètres.

Construire le moteur une seule fois, puis exposer des interfaces adaptées à chaque niveau.

Toujours privilégier : **simplicité → rapidité → traçabilité → évolutivité**.
