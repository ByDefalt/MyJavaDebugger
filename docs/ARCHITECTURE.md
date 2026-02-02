# Architecture SOLID du Debugger

Ce document décrit l'architecture refactorisée du debugger Java selon les principes SOLID.

## Structure des packages

```
src/main/java/
├── commands/          # Commandes utilisateur (Command Pattern)
├── dbg/               # Classes principales du debugger
├── execution/         # Stratégies d'exécution Live/Replay (Strategy Pattern)
├── handlers/          # Gestionnaires d'événements JDI (Strategy Pattern)
├── io/                # Entrée/Sortie abstraites (DIP)
├── managers/          # Gestionnaires métier (SRP)
├── models/            # Modèles de données
└── gui/               # Interface graphique (non refactorisé)
```

## Principes SOLID appliqués

### 1. Single Responsibility Principle (SRP)

Chaque classe a une seule responsabilité :

| Classe | Responsabilité |
|--------|---------------|
| `ScriptableDebugger` | Orchestration du debugger |
| `EventHandler` | Traitement d'un type d'événement |
| `BreakpointManager` | Gestion des breakpoints |
| `SnapshotRecorder` | Enregistrement des snapshots d'exécution |
| `CommandInterpreter` | Parsing et dispatch des commandes |
| `ExecutionStrategy` | Logique d'exécution (live ou replay) |
| `ResultPresenter` | Affichage des résultats |
| `InputReader` | Lecture des entrées utilisateur |

### 2. Open/Closed Principle (OCP)

Le code est ouvert à l'extension mais fermé à la modification :

- **Nouveaux événements** : Créer un nouveau `EventHandler` et l'enregistrer dans `EventHandlerRegistry`
- **Nouvelles commandes** : Utiliser `CommandInterpreter.registerCommand()` sans modifier le code existant
- **Nouveaux types de breakpoints** : Étendre `BreakpointManager`
- **Nouveau mode d'exécution** : Implémenter `ExecutionStrategy` (ex: remote debugging)

```java
// Exemple : Ajouter une nouvelle commande
interpreter.registerCommand("my-command", args -> new MyCommand(args[0]),
    "Description de ma commande", CommandCategory.INSPECTION);
```

```java
// Exemple : Ajouter un nouveau handler d'événement
public class MyEventHandler implements EventHandler<MyEvent> {
    @Override
    public boolean canHandle(Event event) {
        return event instanceof MyEvent;
    }
    
    @Override
    public EventHandlerResult handle(MyEvent event, DebuggerState state) {
        // Traitement...
        return EventHandlerResult.continueExecution();
    }
}

// Enregistrement
eventHandlerRegistry.register(new MyEventHandler());
```

### 3. Liskov Substitution Principle (LSP)

Toutes les implémentations sont substituables :

- `ConsoleInputReader` et toute autre implémentation de `InputReader`
- `ConsoleResultPresenter` et toute autre implémentation de `ResultPresenter`
- Tous les `EventHandler<T>` sont interchangeables dans le registry

### 4. Interface Segregation Principle (ISP)

Les interfaces sont spécifiques et petites :

- `Command` : une seule méthode `execute()`
- `InputReader` : méthodes de lecture uniquement
- `ResultPresenter` : méthodes d'affichage uniquement
- `EventHandler<T>` : `canHandle()` et `handle()` uniquement

### 5. Dependency Inversion Principle (DIP)

Les modules de haut niveau dépendent d'abstractions :

```java
public class ScriptableDebugger {
    // Dépendances sur des abstractions, pas des implémentations concrètes
    private final InputReader inputReader;
    private final ResultPresenter presenter;
    private final EventHandlerRegistry eventHandlerRegistry;
    
    // Injection via constructeur
    public ScriptableDebugger(boolean autoRecord, InputReader inputReader, ResultPresenter presenter) {
        this.inputReader = inputReader;
        this.presenter = presenter;
        // ...
    }
}
```

## Design Patterns utilisés

### Command Pattern
Chaque commande utilisateur est encapsulée dans une classe `Command` avec une méthode `execute()`.

### Factory Pattern
`CommandFactory` et `CommandInterpreter` utilisent des factories pour créer les commandes.

### Strategy Pattern
Les `EventHandler` et `ExecutionStrategy` implémentent le pattern Strategy.

### Registry Pattern
`EventHandlerRegistry` maintient une liste de handlers et dispatch les événements.

### Composite Pattern (GUI)
Les composants GUI sont composés pour former l'interface complète.

---

## Architecture GUI

### Structure des composants

```
gui/
├── ModernDebuggerGUI.java       # Frame principale (composition des composants)
├── ModernScriptableDebuggerGUI.java  # Contrôleur (implémente DebuggerController)
├── components/                   # Composants réutilisables
│   ├── SourceCodePanel.java     # Affichage du code avec syntaxe
│   ├── CallStackPanel.java      # Pile d'appels
│   ├── VariablesPanel.java      # Inspecteur de variables
│   ├── OutputPanel.java         # Console de debug
│   ├── ToolbarPanel.java        # Barre d'outils
│   └── StyledButton.java        # Bouton stylisé
└── theme/                        # Système de thèmes
    ├── Theme.java               # Interface du thème
    ├── DarkTheme.java           # Thème sombre
    └── ThemeManager.java        # Gestionnaire singleton
```

### Principes SOLID appliqués à la GUI

| Principe | Application |
|----------|-------------|
| **SRP** | Chaque composant a une seule responsabilité (affichage code, affichage variables, etc.) |
| **OCP** | Nouveaux thèmes via `Theme` interface, nouveaux composants sans modifier existants |
| **LSP** | Tous les thèmes sont substituables |
| **ISP** | Interfaces spécifiques : `ToolbarListener`, `CallStackListener`, `BreakpointListener` |
| **DIP** | `ModernDebuggerGUI` dépend de `DebuggerController` (abstraction) |

### Diagramme des composants GUI

```
┌─────────────────────────────────────────────────────────────┐
│                    ModernDebuggerGUI                        │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                    ToolbarPanel                       │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────┐  ┌──────────────────────────┐  │
│  │                         │  │     CallStackPanel       │  │
│  │    SourceCodePanel      │  ├──────────────────────────┤  │
│  │                         │  │     VariablesPanel       │  │
│  └─────────────────────────┘  └──────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                     OutputPanel                       │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
              ┌───────────────────────────────┐
              │      DebuggerController       │ (interface)
              └───────────────────────────────┘
                              △
                              │
              ┌───────────────────────────────┐
              │ ModernScriptableDebuggerGUI   │
              └───────────────────────────────┘
```

### Ajouter un nouveau thème

```java
public class LightTheme implements Theme {
    @Override public Color getBackgroundPrimary() { return Color.WHITE; }
    @Override public Color getTextPrimary() { return Color.BLACK; }
    // ... autres méthodes
}

// Utilisation
ThemeManager.getInstance().setTheme(new LightTheme());
```

### Ajouter un nouveau composant

```java
public class BreakpointsListPanel extends JPanel {
    private final Theme theme = ThemeManager.getInstance().getTheme();
    
    public interface BreakpointActionListener {
        void onBreakpointRemove(String key);
    }
    
    // ...
}
```

---

## Extensibilité

### Ajouter un nouveau type de breakpoint

1. Ajouter un nouveau type dans `Breakpoint.BreakpointType`
2. Ajouter la méthode de création dans `BreakpointManager`
3. Créer une nouvelle commande qui utilise le manager

### Ajouter un support GUI

La nouvelle architecture permet d'intégrer facilement :
- Utiliser `ModernDebuggerGUI` avec un contrôleur personnalisé
- Ou créer de nouveaux composants et les ajouter au layout

## Diagramme de classes simplifié

```
┌─────────────────────┐     ┌─────────────────┐
│ ScriptableDebugger  │────▶│ InputReader     │◀── ConsoleInputReader
│                     │     └─────────────────┘
│                     │     ┌─────────────────┐
│                     │────▶│ ResultPresenter │◀── ConsoleResultPresenter
│                     │     └─────────────────┘
│                     │     ┌─────────────────────┐
│                     │────▶│ EventHandlerRegistry│
└─────────────────────┘     └─────────────────────┘
                                      │
                                      ▼
                            ┌─────────────────┐
                            │ EventHandler<T> │
                            └─────────────────┘
                                      △
           ┌──────────────────────────┼──────────────────────────┐
           │                          │                          │
┌──────────────────┐    ┌─────────────────────┐    ┌──────────────────┐
│BreakpointEvent   │    │ StepEventHandler    │    │VMDisconnect      │
│Handler           │    │                     │    │EventHandler      │
└──────────────────┘    └─────────────────────┘    └──────────────────┘
```
