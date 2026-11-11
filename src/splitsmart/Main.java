package splitsmart;

import splitsmart.storage.DataStore;
import splitsmart.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for the SplitSmart application.
 * Initializes persistent data and launches the desktop Swing interface
 * safely on the Event Dispatch Thread (EDT).
 */
public class Main {

    public static void main(String[] args) {
        // Initialize persistent DataStore
        DataStore.getInstance();

        // Launch UI strictly on the Swing Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel base or default cross-platform
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
