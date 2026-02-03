package gui.theme;
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
