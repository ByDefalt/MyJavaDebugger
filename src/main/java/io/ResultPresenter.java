package io;

import commands.CommandResult;

/**
 * Interface pour l'affichage des résultats (Dependency Inversion Principle)
 * Permet de découpler le debugger de la destination de sortie (console, GUI, etc.)
 */
public interface ResultPresenter {

    /**
     * Affiche un résultat de commande
     */
    void displayResult(CommandResult result);

    /**
     * Affiche un message d'information
     */
    void info(String message);

    /**
     * Affiche un message d'erreur
     */
    void error(String message);

    /**
     * Affiche un message de debug/warning
     */
    void warn(String message);
}
