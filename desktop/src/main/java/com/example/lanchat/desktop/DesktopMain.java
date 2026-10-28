package com.example.lanchat.desktop;

import javax.swing.*;

public class DesktopMain {
    public static void main(String[] args) {
        // Set Thread Uncaught Exception Handler
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null,
                    "WiFili Messenger Note:\n" + e.getMessage(),
                    "WiFili Messenger", JOptionPane.INFORMATION_MESSAGE)
            );
        });

        if (args.length > 0 && ("web".equalsIgnoreCase(args[0]) || "--web".equalsIgnoreCase(args[0]))) {
            try {
                WebServer.main(args);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            try {
                DesktopChatFrame frame = new DesktopChatFrame();
                frame.setVisible(true);
            } catch (Throwable t) {
                t.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Starting WiFili Desktop App...\n" + t.getMessage(),
                        "WiFili Messenger", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
}
