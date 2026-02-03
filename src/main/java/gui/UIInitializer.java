package gui;
import io.GUILogger;
import io.Logger;
import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
public class UIInitializer {
    public UIContext initializeAndWait(int timeoutSeconds) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        UIContext context = new UIContext();
        SwingUtilities.invokeLater(() -> {
            context.gui = new DebuggerGUI();
            context.logger = new GUILogger(context.gui::appendDebugLog, Logger.Level.DEBUG);
            latch.countDown();
        });
        boolean initialized = latch.await(timeoutSeconds, TimeUnit.SECONDS);
        if (!initialized) {
            throw new IllegalStateException("UI initialization timeout");
        }
        return context;
    }
    public static class UIContext {
        public DebuggerGUI gui;
        public Logger logger;
    }
}
