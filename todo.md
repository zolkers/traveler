# Traveler Todo

Etat courant: le kernel modulaire, les features route par type, les garde-fous d'architecture, le no-progress tick-based, le swim ascent et le meilleur suivi de ligne sont en place. Ce fichier liste ce qui reste pour rendre la codebase encore plus simple a modifier et plus robuste en jeu.

## Priorite 1 - Valider et tuner le suivi de ligne en conditions reelles

- [ ] Instrumenter les snapshots/debug frames avec `signedLateralError`, `lateralCorrection`, `distanceOnPath`, `pathOffset` effectif et action courante.
  - Fichiers probables: `modules/core/src/main/java/dev/traveler/core/navigation/steering/SteeringPlan.java`, `modules/core/src/main/java/dev/traveler/core/navigation/plan/NavigationSteeringDebug.java`, `modules/core/src/main/java/dev/traveler/core/debug/DebugTextFormatter.java`.
  - Tests: `PathSteeringControllerTest`, `NavigationDebugSnapshotTest`, `PathDebugRenderModelTest`.
- [ ] Ajouter un scenario de suivi de ligne long avec oscillation laterale: depart decale, vitesse vers la ligne, vitesse qui s'eloigne, puis virage.
  - Objectif: prouver que le damping reduit les oscillations sans rendre le player mou.
  - Test cible: `modules/core/src/test/java/dev/traveler/core/navigation/steering/PathSteeringControllerTest.java`.
- [ ] Tuner les defaults ajoutes dans `TravelerSettings`: derivative gain, minimum path offset, reduction de lookahead par erreur laterale, action approach offset.
  - Fichier: `modules/core/src/main/java/dev/traveler/core/settings/TravelerSettings.java`.
  - Verification: `.\gradlew.bat --no-daemon :core:test --tests dev.traveler.core.navigation.steering.PathSteeringControllerTest`.
- [ ] Faire une passe terrain Minecraft sur marche, jump, climb, drop, swim.
  - Capturer au moins: ligne droite, diagonale, virage serre, saut en bord de block, echelle/vigne, sortie d'eau.
  - Noter les valeurs qui oscillent avant de changer les constantes.

## Priorite 2 - Finir l'interchangeabilite du kernel

- [ ] Ajouter une matrice de modules kernel plus explicite: route-only, route+navigation, debug-only, recovery-only, noop modules.
  - Fichiers probables: `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/api`, `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/spi`, `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/impl`, `modules/core/src/main/java/dev/traveler/core/pathfinder/kernel/noop`.
  - Test cible: `modules/core/src/test/java/dev/traveler/core/pathfinder/kernel/PathfinderKernelInterchangeabilityTest.java`.
- [ ] Ajouter un test qui retire chaque module un par un et verifie que le kernel degrade proprement.
  - Cas attendus: pas de controller -> route calculee sans frame de controle; pas de debug -> pathfinding toujours OK; pas de recovery -> navigation fonctionne sans action de recuperation; pas de feature jump/climb/swim -> route impossible ou alternative, jamais crash.
- [ ] Rendre les erreurs de module typees dans `PathfinderKernelResult`.
  - Objectif: aucune erreur string-only quand un module manque ou refuse une capability.

## Priorite 3 - Nettoyer les derniers points durs de navigation

- [ ] Deplacer les controllers de traversal restants hors de `navigation.plan` vers un package plus intentionnel.
  - Exemple actuel: `modules/core/src/main/java/dev/traveler/core/navigation/plan/JumpTraversalController.java`.
  - Destination probable: `navigation/internal` ou `navigation/locomotion`, selon si le type doit rester public au package.
  - Tests: `NavigationFramePlannerTest`, `ControlProjectorTest`.
- [ ] Reduire la dependance transitoire de `ControlProjector` a `NavigationFramePlan`.
  - Etat actuel: `ControlProjector` sait projeter `TraversalIntent`, mais garde encore une surcharge qui accepte `NavigationFramePlan`.
  - Objectif: garder la surcharge seulement si elle sert vraiment d'adaptateur, sinon faire passer `TraversalIntent` partout.
- [ ] Decouper `NavigationFramePlanner` si de nouveaux comportements arrivent.
  - Sous-composants candidats: action timing, speed intent, steering target, progress/reached policy, action-specific locomotion.
  - Regle: chaque action doit pouvoir changer sans toucher toute la planification.

## Priorite 4 - Rendre les features de traversal encore plus plug-and-play

- [ ] Ajouter un test par feature retiree: walk, jump, climb, drop, swim.
  - Fichier cible: `modules/core/src/test/java/dev/traveler/core/route/RouteSearchServiceTest.java` ou un nouveau test dedie aux features.
  - Attendu: la feature absente ne casse pas le resolver; elle retire seulement les transitions et route steps correspondants.
- [ ] Verifier que chaque feature expose clairement ses providers.
  - Fichiers: `modules/core/src/main/java/dev/traveler/core/route/internal/features/walk`, `jump`, `climb`, `drop`, `swim`.
  - Regle: pas d'import direct entre feature internals; passer par les interfaces du package assigne.
- [ ] Ajouter des fixtures de monde minimales par feature.
  - Objectif: eviter que les tests jump/climb/swim dependent de gros mondes difficiles a lire.

## Priorite 5 - Revoir les valeurs spatial shared

- [ ] Decider si `WorldPoint` et `HorizontalVector` restent dans `dev.traveler.core.common.geometry` ou migrent vers `dev.traveler.core.world.geometry`.
  - Etat actuel: ils sont dans `common.geometry`.
  - Si migration: faire une vraie migration stricte, sans wrappers de compatibilite.
  - Tests: `DependencyGuardTest`, `NavigationSnapshotTest`, tous les tests route/navigation qui importent ces types.
- [ ] Si `common.geometry` reste, documenter pourquoi ce package est autorise comme couche sous `world`, `route` et `navigation`.
  - Fichier probable: `project.md`.

## Priorite 6 - Observabilite et debug utilisables en jeu

- [ ] Rendre les layers debug plus riches dans le rendu MC: active, prepared, pending, rejected, target, failure junction, lateral correction, action boundary.
  - Core deja present: `DebugLayer`, `DebugFrame`, `PathDebugRenderModel`.
  - Cote integration probable: modules Minecraft renderer/debug overlay.
- [ ] Ajouter une commande ou option qui dump un frame de navigation complet.
  - Contenu: path target, steering target, nearest point, signed lateral error, movement vector, action, recovery action.
- [ ] Ajouter un rapport court quand une route echoue.
  - Contenu: start, goal, budget, feature active/inactive, raison route, dernier noeud considere si disponible.

## Priorite 7 - Qualite repo et garde-fous

- [ ] Ajouter un check anti-timeout/flaky sur `CommandModuleTestHarness`.
  - Contexte: un `check` complet a deja eu un timeout async non reproductible dans `command_buildmycommand`, puis le test isole et le check complet sont passes.
  - Objectif: remplacer le spin wait fragile par une attente conditionnelle plus explicite ou un drain deterministe.
- [ ] Garder `.\gradlew.bat --no-daemon check` comme gate final avant chaque push.
- [ ] Quand une task touche navigation en jeu, ajouter au minimum un test core cible plus une note de validation terrain.

## Definition of Done globale

- [ ] Chaque feature de movement a une interface claire, une implementation standard, une implementation noop si utile, et des tests d'absence.
- [ ] Retirer un controller, une feature ou un debug module degrade le systeme proprement au lieu de casser le pathfinder.
- [ ] Le route planner produit des intentions route-only; navigation reste seule proprietaire de l'execution.
- [ ] Les snapshots/debug sont assez stables pour diagnostiquer les erreurs sans lire tout le code.
- [ ] `.\gradlew.bat --no-daemon check` passe.
