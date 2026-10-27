package com.example.lanchat.desktop;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DesktopChatFrame extends JFrame implements DesktopLanManager.Listener {

    private final DesktopLanManager lanManager;
    private DefaultListModel<String> peerListModel;
    private JList<String> peerJList;
    private JPanel chatMessagesPanel;
    private JTextField tfMessage;
    private JLabel lblActivePeerName;
    private JLabel lblStatusInfo;

    private DesktopLanManager.DesktopPeer activePeer;

    // Call Video Window
    private JFrame callFrame;
    private JLabel lblVideoDisplay;
    private JLabel lblCallTimer;

    public DesktopChatFrame() {
        super("WiFili Messenger - Desktop (Linux & Windows)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(null);

        lanManager = DesktopLanManager.getInstance();
        lanManager.registerListener(this);
        lanManager.start();

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // Top Header Bar
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setBackground(new Color(15, 23, 42));
        topHeader.setPreferredSize(new Dimension(1000, 60));
        topHeader.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel lblTitle = new JLabel("WiFili Messenger • Desktop");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);

        String localIp = getLocalIp();
        lblStatusInfo = new JLabel("👤 " + lanManager.getDeviceName() + " | 📶 IP: " + localIp);
        lblStatusInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblStatusInfo.setForeground(new Color(203, 213, 225));

        JButton btnScan = new JButton("⚡ Scan LAN");
        btnScan.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnScan.setBackground(new Color(30, 101, 243));
        btnScan.setForeground(Color.WHITE);
        btnScan.setFocusPainted(false);
        btnScan.addActionListener(e -> {
            lanManager.announce();
            JOptionPane.showMessageDialog(this, "Scanning LAN network for nearby devices...", "LAN Scan", JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightHeader.setOpaque(false);
        rightHeader.add(lblStatusInfo);
        rightHeader.add(btnScan);

        topHeader.add(lblTitle, BorderLayout.WEST);
        topHeader.add(rightHeader, BorderLayout.EAST);
        add(topHeader, BorderLayout.NORTH);

        // Main Split Content (Left Sidebar + Right Chat Panel)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(260);

        // Left Sidebar (Peers List)
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(241, 245, 249));

        JLabel lblPeersHeader = new JLabel(" Connected LAN Users");
        lblPeersHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblPeersHeader.setBorder(new EmptyBorder(12, 12, 12, 12));
        lblPeersHeader.setForeground(new Color(15, 23, 42));

        peerListModel = new DefaultListModel<>();
        peerJList = new JList<>(peerListModel);
        peerJList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        peerJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        peerJList.setFixedCellHeight(44);
        peerJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int index = peerJList.getSelectedIndex();
                List<DesktopLanManager.DesktopPeer> list = lanManager.getPeersList();
                if (index >= 0 && index < list.size()) {
                    activePeer = list.get(index);
                    lblActivePeerName.setText("💬 Chatting with: " + activePeer.getName() + " (" + activePeer.getIp() + ")");
                }
            }
        });

        JScrollPane scrollPeers = new JScrollPane(peerJList);
        scrollPeers.setBorder(null);

        sidebar.add(lblPeersHeader, BorderLayout.NORTH);
        sidebar.add(scrollPeers, BorderLayout.CENTER);
        splitPane.setLeftComponent(sidebar);

        // Right Chat Area
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(new Color(248, 250, 252));

        // Chat Header
        JPanel chatHeader = new JPanel(new BorderLayout());
        chatHeader.setBackground(Color.WHITE);
        chatHeader.setPreferredSize(new Dimension(700, 50));
        chatHeader.setBorder(new EmptyBorder(8, 16, 8, 16));

        lblActivePeerName = new JLabel("Select a LAN user on the left to chat or call");
        lblActivePeerName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblActivePeerName.setForeground(new Color(15, 23, 42));

        JPanel callBtnsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        callBtnsPanel.setOpaque(false);

        JButton btnAudioCall = new JButton("📞 Audio Call");
        btnAudioCall.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAudioCall.setBackground(new Color(34, 197, 94));
        btnAudioCall.setForeground(Color.WHITE);
        btnAudioCall.addActionListener(e -> initiateCall("audio"));

        JButton btnVideoCall = new JButton("🎥 Video Call");
        btnVideoCall.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnVideoCall.setBackground(new Color(99, 102, 241));
        btnVideoCall.setForeground(Color.WHITE);
        btnVideoCall.addActionListener(e -> initiateCall("video"));

        callBtnsPanel.add(btnAudioCall);
        callBtnsPanel.add(btnVideoCall);

        chatHeader.add(lblActivePeerName, BorderLayout.WEST);
        chatHeader.add(callBtnsPanel, BorderLayout.EAST);
        chatPanel.add(chatHeader, BorderLayout.NORTH);

        // Chat Timeline
        chatMessagesPanel = new JPanel();
        chatMessagesPanel.setLayout(new BoxLayout(chatMessagesPanel, BoxLayout.Y_AXIS));
        chatMessagesPanel.setBackground(new Color(248, 250, 252));
        chatMessagesPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JScrollPane scrollMessages = new JScrollPane(chatMessagesPanel);
        scrollMessages.setBorder(null);
        chatPanel.add(scrollMessages, BorderLayout.CENTER);

        // Bottom Input Area
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setPreferredSize(new Dimension(700, 60));
        inputPanel.setBorder(new EmptyBorder(10, 16, 10, 16));

        tfMessage = new JTextField();
        tfMessage.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tfMessage.addActionListener(e -> sendTextMessage());

        JButton btnAttach = new JButton("📎 Attach File");
        btnAttach.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAttach.addActionListener(e -> attachAndSendFile());

        JButton btnSend = new JButton("Send ➔");
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSend.setBackground(new Color(30, 101, 243));
        btnSend.setForeground(Color.WHITE);
        btnSend.addActionListener(e -> sendTextMessage());

        JPanel inputBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        inputBtns.setOpaque(false);
        inputBtns.add(btnAttach);
        inputBtns.add(btnSend);

        inputPanel.add(tfMessage, BorderLayout.CENTER);
        inputPanel.add(inputBtns, BorderLayout.EAST);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        splitPane.setRightComponent(chatPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private void initiateCall(String callType) {
        if (activePeer == null) {
            JOptionPane.showMessageDialog(this, "Please select an active LAN user on the left list first!", "Not Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        lanManager.sendCallSignal(activePeer.getIp(), 10, callType);
        openCallFrame(activePeer.getName(), activePeer.getIp(), callType, false);
    }

    private void openCallFrame(String peerName, String peerIp, String callType, boolean isIncoming) {
        if (callFrame != null) callFrame.dispose();

        callFrame = new JFrame(callType.toUpperCase() + " Call - WiFili Desktop");
        callFrame.setSize(500, 420);
        callFrame.setLocationRelativeTo(this);
        callFrame.setLayout(new BorderLayout());

        JPanel callInfo = new JPanel(new GridLayout(3, 1, 4, 4));
        callInfo.setBackground(new Color(15, 23, 42));
        callInfo.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel lblName = new JLabel("👤 " + peerName + " (" + peerIp + ")", SwingConstants.CENTER);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblName.setForeground(Color.WHITE);

        lblCallTimer = new JLabel(isIncoming ? "Incoming " + callType + " call..." : "Calling " + peerName + "...", SwingConstants.CENTER);
        lblCallTimer.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblCallTimer.setForeground(new Color(148, 163, 184));

        callInfo.add(lblName);
        callInfo.add(lblCallTimer);

        callFrame.add(callInfo, BorderLayout.NORTH);

        lblVideoDisplay = new JLabel("Video Stream Area", SwingConstants.CENTER);
        lblVideoDisplay.setOpaque(true);
        lblVideoDisplay.setBackground(Color.BLACK);
        lblVideoDisplay.setForeground(Color.GRAY);
        if ("video".equalsIgnoreCase(callType)) {
            callFrame.add(lblVideoDisplay, BorderLayout.CENTER);
        }

        JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 12));
        ctrlPanel.setBackground(new Color(15, 23, 42));

        JButton btnMute = new JButton("🎙️ Mute Mic");
        btnMute.addActionListener(e -> lanManager.toggleMute());

        JButton btnHangup = new JButton("🔴 Hangup");
        btnHangup.setBackground(new Color(239, 68, 68));
        btnHangup.setForeground(Color.WHITE);
        btnHangup.addActionListener(e -> {
            lanManager.sendCallSignal(peerIp, 13, callType);
            lanManager.stopCall();
            callFrame.dispose();
        });

        ctrlPanel.add(btnMute);
        ctrlPanel.add(btnHangup);

        callFrame.add(ctrlPanel, BorderLayout.SOUTH);
        callFrame.setVisible(true);

        if (!isIncoming) {
            lanManager.startAudioCall(peerIp);
        }
    }

    private void sendTextMessage() {
        if (activePeer == null) {
            JOptionPane.showMessageDialog(this, "Please select an active LAN user on the left list first!", "Not Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String text = tfMessage.getText().trim();
        if (text.isEmpty()) return;

        tfMessage.setText("");
        appendMessage("You", text, new Color(30, 101, 243));

        lanManager.sendText(activePeer.getIp(), text, null, err ->
                JOptionPane.showMessageDialog(this, "Failed to send: " + err, "Error", JOptionPane.ERROR_MESSAGE));
    }

    private void attachAndSendFile() {
        if (activePeer == null) {
            JOptionPane.showMessageDialog(this, "Please select an active LAN user on the left list first!", "Not Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            appendMessage("You", "Sending file: " + file.getName() + " (" + file.length() / 1024 + " KB)...", new Color(30, 101, 243));
            lanManager.sendFile(activePeer.getIp(), file, "file",
                    pct -> {},
                    () -> appendMessage("You", "Sent file: " + file.getName(), new Color(30, 101, 243)),
                    err -> JOptionPane.showMessageDialog(this, "You are not connected to " + activePeer.getName() + ".\n\n" + err, "File Send Failed", JOptionPane.ERROR_MESSAGE));
        }
    }

    private void appendMessage(String sender, String body, Color headerColor) {
        JPanel msgBubble = new JPanel(new BorderLayout(4, 4));
        msgBubble.setBackground(Color.WHITE);
        msgBubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        JLabel lblSender = new JLabel(sender + " • " + timeStr);
        lblSender.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSender.setForeground(headerColor);

        JLabel lblBody = new JLabel("<html><body style='width: 450px;'>" + body + "</body></html>");
        lblBody.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        msgBubble.add(lblSender, BorderLayout.NORTH);
        msgBubble.add(lblBody, BorderLayout.CENTER);

        chatMessagesPanel.add(msgBubble);
        chatMessagesPanel.add(Box.createVerticalStrut(8));
        chatMessagesPanel.revalidate();
        chatMessagesPanel.repaint();
    }

    @Override
    public void onPeersUpdated(List<DesktopLanManager.DesktopPeer> peerList) {
        SwingUtilities.invokeLater(() -> {
            peerListModel.clear();
            for (DesktopLanManager.DesktopPeer p : peerList) {
                peerListModel.addElement("👤 " + p.getName() + " (" + p.getIp() + ")");
            }
        });
    }

    @Override
    public void onMessageReceived(String senderIp, String senderName, String body, String type, String filePath, String fileName) {
        SwingUtilities.invokeLater(() -> appendMessage(senderName, body, new Color(15, 23, 42)));
    }

    @Override
    public void onCallOfferReceived(String senderIp, String senderName, String callType) {
        SwingUtilities.invokeLater(() -> {
            int option = JOptionPane.showConfirmDialog(this,
                    "Incoming " + callType.toUpperCase() + " call from " + senderName + " (" + senderIp + "). Accept?",
                    "Incoming Call", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                lanManager.sendCallSignal(senderIp, 11, callType);
                openCallFrame(senderName, senderIp, callType, true);
                lanManager.startAudioCall(senderIp);
            } else {
                lanManager.sendCallSignal(senderIp, 12, callType);
            }
        });
    }

    @Override
    public void onCallAcceptReceived(String senderIp) {
        SwingUtilities.invokeLater(() -> {
            if (lblCallTimer != null) lblCallTimer.setText("Connected");
        });
    }

    @Override
    public void onCallDeclineReceived(String senderIp) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Call declined by peer.", "Call Status", JOptionPane.INFORMATION_MESSAGE);
            if (callFrame != null) callFrame.dispose();
        });
    }

    @Override
    public void onCallEndReceived(String senderIp) {
        SwingUtilities.invokeLater(() -> {
            lanManager.stopCall();
            if (callFrame != null) callFrame.dispose();
            JOptionPane.showMessageDialog(this, "Call ended.", "Call Status", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    @Override
    public void onRemoteVideoFrame(byte[] jpegData, int camId) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(jpegData));
            if (img != null && lblVideoDisplay != null) {
                SwingUtilities.invokeLater(() -> {
                    Image scaled = img.getScaledInstance(lblVideoDisplay.getWidth(), lblVideoDisplay.getHeight(), Image.SCALE_FAST);
                    lblVideoDisplay.setIcon(new ImageIcon(scaled));
                    lblVideoDisplay.setText("");
                });
            }
        } catch (Exception ignored) {}
    }
}
