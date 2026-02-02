package gui.theme;

/**
 * Gestionnaire de thème global (Singleton)
 * Permet de changer le thème de l'application dynamiquement
 */
public class ThemeManager {

    private static ThemeManager instance;
    private Theme currentTheme;

    private ThemeManager() {
        this.currentTheme = DarkTheme.getInstance();
    }

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public Theme getTheme() {
        return currentTheme;
    }

    public void setTheme(Theme theme) {
        this.currentTheme = theme;
    }
}
