# MyJavaDebugger

Un débogueur Java scriptable construit avec l'API JDI (Java Debug Interface), conçu selon les principes SOLID.

## 🎯 Fonctionnalités

- **Débogage interactif** : Step into, Step over, Continue, Breakpoints
- **Mode Recording** : Enregistre automatiquement toute l'exécution pour navigation ultérieure
- **Mode Replay** : Naviguez dans l'historique d'exécution (back/forward)
- **Interface graphique** : GUI moderne avec thème sombre
- **Architecture SOLID** : Code extensible et maintenable

## 🚀 Démarrage rapide

### Prérequis

- Java 17+ (avec JDK incluant `tools.jar` pour JDI)
- Gradle 8.x

### Lancer le débogueur

```bash
# Mode console
./gradlew runDebugger

# Mode GUI
./gradlew runGUI

# Mode Recording (enregistre puis permet le replay)
./gradlew runRecording
```

## 📖 Commandes disponibles

### Navigation
| Commande | Description |
|----------|-------------|
| `step` | Step into - entre dans les méthodes |
| `step-over` | Step over - exécute sans entrer dans les méthodes |
| `continue` | Continue jusqu'au prochain breakpoint |

### Historique (mode replay)
| Commande | Description |
|----------|-------------|
| `back` | Recule d'un pas dans l'historique |
| `forward` | Avance d'un pas dans l'historique |
| `history` | Affiche l'aperçu de l'historique d'exécution |

### Inspection
| Commande | Description |
|----------|-------------|
| `frame` | Affiche le frame courant |
| `stack` | Affiche la pile d'appels |
| `temporaries` | Affiche les variables locales |
| `receiver` | Affiche le receveur (this) |
| `receiver-variables` | Affiche les variables d'instance du receveur |
| `method` | Affiche la méthode courante |
| `arguments` | Affiche les arguments de la méthode |
| `print-var <nom>` | Affiche la valeur d'une variable |

### Breakpoints
| Commande | Description |
|----------|-------------|
| `break <fichier> <ligne>` | Ajoute un breakpoint |
| `break-once <fichier> <ligne>` | Breakpoint one-shot (s'arrête une fois) |
| `break-on-count <fichier> <ligne> <n>` | S'arrête après n passages |
| `break-before-method-call <méthode>` | S'arrête avant l'appel d'une méthode |
| `breakpoints` | Liste tous les breakpoints |

### Aide
| Commande | Description |
|----------|-------------|
| `help` | Affiche l'aide complète |
| `help <commande>` | Aide pour une commande spécifique |

## 🏗️ Architecture SOLID

Le projet suit les 5 principes SOLID :

### Structure des packages

```
src/main/java/
├── commands/          # Commandes utilisateur (Command Pattern)
├── dbg/               # Classes principales du debugger
├── execution/         # Stratégies d'exécution Live/Replay (Strategy Pattern)
├── handlers/          # Gestionnaires d'événements JDI (Strategy Pattern)
├── io/                # Entrée/Sortie abstraites (DIP)
├── managers/          # Gestionnaires métier (SRP)
├── models/            # Modèles de données
└── gui/               # Interface graphique
    ├── AbstractDebuggerGUI.java  # Classe abstraite commune (Template Method)
    ├── ModernDebuggerGUI.java    # GUI moderne
    ├── ModernScriptableDebuggerGUI.java  # Contrôleur GUI moderne
    ├── ScriptableDebuggerGUI.java        # Contrôleur ancienne GUI
    ├── components/    # Composants UI réutilisables
    └── theme/         # Système de thèmes
```

### Hiérarchie des classes GUI

```
AbstractDebuggerGUI (Template Method Pattern)
    ├── ModernScriptableDebuggerGUI  → ModernDebuggerGUI
    └── ScriptableDebuggerGUI        → DebuggerGUI
```

La classe abstraite `AbstractDebuggerGUI` contient :
- La logique de connexion JDI
- La boucle d'événements
- La capture de sortie
- La gestion des threads

Les sous-classes implémentent uniquement :
- `initializeGUI()` - Création de la fenêtre
- `onBreakpoint()` / `onStep()` - Réaction aux événements
- `setInitialBreakpoint()` - Configuration du breakpoint initial

### Principes appliqués

| Principe | Application |
|----------|-------------|
| **S** - Single Responsibility | Chaque classe a une seule responsabilité |
| **O** - Open/Closed | Extensible via `EventHandler`, `ExecutionStrategy`, `Theme` |
| **L** - Liskov Substitution | Toutes les implémentations sont substituables |
| **I** - Interface Segregation | Interfaces petites et spécifiques |
| **D** - Dependency Inversion | Dépendances sur abstractions (`InputReader`, `ResultPresenter`) |

### Design Patterns utilisés

- **Command Pattern** : Encapsulation des commandes utilisateur
- **Strategy Pattern** : `EventHandler`, `ExecutionStrategy`
- **Factory Pattern** : `CommandFactory`, `CommandInterpreter`
- **Registry Pattern** : `EventHandlerRegistry`
- **Composite Pattern** : Composants GUI

## 🎨 Interface Graphique

L'interface graphique moderne comprend :

- **Barre d'outils** : Boutons Continue, Step Over, Step Into, Stop
- **Panel de code source** : Affichage avec coloration syntaxique et breakpoints
- **Pile d'appels** : Navigation dans les frames
- **Inspecteur de variables** : Arbre des variables avec expansion
- **Console de debug** : Messages et sortie du programme

### Thèmes

Le système de thèmes permet de personnaliser l'apparence :

```java
// Utiliser un thème personnalisé
ThemeManager.getInstance().setTheme(new MonTheme());
```

## 🔧 Extensibilité

### Ajouter une nouvelle commande

```java
// 1. Créer la commande
public class MaCommande implements Command {
    @Override
    public CommandResult execute(DebuggerState state) {
        // Logique...
        return CommandResult.success("Message", data);
    }
}

// 2. L'enregistrer
interpreter.registerCommand("ma-commande", 
    args -> new MaCommande(args[0]),
    "Description de ma commande", 
    CommandCategory.INSPECTION);
```

### Ajouter un nouveau handler d'événement

```java
public class MonHandler implements EventHandler<MonEvent> {
    @Override
    public boolean canHandle(Event event) {
        return event instanceof MonEvent;
    }
    
    @Override
    public EventHandlerResult handle(MonEvent event, DebuggerState state) {
        // Traitement...
        return EventHandlerResult.continueExecution();
    }
}

// Enregistrement
eventHandlerRegistry.register(new MonHandler());
```

### Ajouter un nouveau thème

```java
public class LightTheme implements Theme {
    @Override public Color getBackgroundPrimary() { return Color.WHITE; }
    @Override public Color getTextPrimary() { return Color.BLACK; }
    // ... autres méthodes
}
```

## 📁 Fichiers principaux

| Fichier | Description |
|---------|-------------|
| `ScriptableDebugger.java` | Debugger console principal |
| `ModernDebuggerGUI.java` | Interface graphique principale |
| `ModernScriptableDebuggerGUI.java` | Contrôleur GUI |
| `CommandInterpreter.java` | Interpréteur de commandes |
| `EventHandlerRegistry.java` | Registry des handlers d'événements |
| `ExecutionStrategy.java` | Interface pour les stratégies d'exécution |

## 📄 Documentation

Une documentation détaillée de l'architecture est disponible dans [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## 🤝 Contribution

1. Fork le projet
2. Créez votre branche (`git checkout -b feature/AmazingFeature`)
3. Committez vos changements (`git commit -m 'Add AmazingFeature'`)
4. Push sur la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une Pull Request

## 📝 Licence

Ce projet est sous licence MIT - voir le fichier [LICENSE](LICENSE) pour plus de détails.

## 🙏 Remerciements

- API JDI (Java Debug Interface) de Oracle
- Inspiré par les debuggers Pharo/Smalltalk pour la navigation dans l'historique
