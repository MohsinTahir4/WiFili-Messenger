package com.example.lanchat.desktop;

import javax.swing.*;

public class DesktopMain {
    public static void main(String[] args) {
        if (args.length > 0 && ("web".equalsIgnoreCase(args[0]) || "--web".equalsIgnoreCase(args[0]))) {
            WebServer.main(args);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            DesktopChatFrame frame = new DesktopChatFrame();
            frame.setVisible(true);
        });
    }
}
