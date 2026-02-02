package io;

/**
 * Interface pour la lecture des entrées utilisateur (Dependency Inversion Principle)
 * Permet de découpler le debugger de la source d'entrée (console, fichier, GUI, etc.)
 */
public interface InputReader {

    /**
     * Lit une ligne d'entrée de l'utilisateur
     * @return La ligne lue, ou null si pas d'entrée disponible
     */
    String readLine();

    /**
     * Affiche un prompt avant de lire
     * @param prompt Le prompt à afficher
     * @return La ligne lue
     */
    String readLine(String prompt);

    /**
     * Vérifie si l'entrée est disponible
     */
    boolean hasInput();

    /**
     * Ferme le reader
     */
    void close();
}
