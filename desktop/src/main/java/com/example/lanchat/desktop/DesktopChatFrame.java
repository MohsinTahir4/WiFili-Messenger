package com.example.lanchat.desktop;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
    private DefaultListModel<DesktopLanManager.DesktopPeer> peerListModel;
    private JList<DesktopLanManager.DesktopPeer> peerJList;
    private JTextField tfSearch;
    private JPanel chatMessagesPanel;
    private JTextField tfMessage;
    private JLabel lblActivePeerName;
    private JLabel lblActivePeerStatus;
    private CircleLabel lblActiveAvatar;
    private JLabel lblStatusInfo;
    private JLabel lblEmptySidebar;

    private DesktopLanManager.DesktopPeer activePeer;

    // Call Video Window
    private JFrame callFrame;
    private JLabel lblVideoDisplay;
    private JLabel lblCallTimer;

    public DesktopChatFrame() {
        super("WiFili Messenger • Desktop Edition (Linux & Windows)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 720);
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
        topHeader.setPreferredSize(new Dimension(1080, 64));
        topHeader.setBorder(new EmptyBorder(10, 20, 10, 20));

        JPanel logoGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoGroup.setOpaque(false);

        JLabel lblTitle = new JLabel("WiFili Messenger");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSubTitle = new JLabel("• Web & Desktop Edition");
        lblSubTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubTitle.setForeground(new Color(56, 189, 248));

        logoGroup.add(lblTitle);
        logoGroup.add(lblSubTitle);

        String localIp = getLocalIp();
        lblStatusInfo = new JLabel("User: " + lanManager.getDeviceName() + "  |  IP: " + localIp);
        lblStatusInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblStatusInfo.setForeground(new Color(203, 213, 225));

        JButton btnScan = createStyledButton("Scan LAN Users", new Color(30, 101, 243));
        btnScan.addActionListener(e -> {
            lanManager.announce();
            List<DesktopLanManager.DesktopPeer> list = lanManager.getPeersList();
            JOptionPane.showMessageDialog(this,
                    "LAN Scan Triggered!\nFound " + list.size() + " active user(s) on your local Wi-Fi network.\n(Tip: You can also use '+ Add IP' if router blocks broadcast).",
                    "LAN Scan", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnManualAdd = createStyledButton("+ Add IP", new Color(15, 118, 110));
        btnManualAdd.setToolTipText("Manually connect to a device by entering its IP address (e.g. 192.168.1.50)");
        btnManualAdd.addActionListener(e -> {
            String ip = JOptionPane.showInputDialog(this, "Enter Device IP Address (e.g. 192.168.1.50):", "Manual IP Connection", JOptionPane.QUESTION_MESSAGE);
            if (ip != null && !ip.trim().isEmpty()) {
                String name = JOptionPane.showInputDialog(this, "Enter Device Name:", "Manual IP Connection", JOptionPane.QUESTION_MESSAGE);
                if (name == null || name.trim().isEmpty()) name = "Device-" + ip;
                final String finalName = name;
                
                DesktopLanManager.DesktopPeer manualPeer = new DesktopLanManager.DesktopPeer(ip.trim(), finalName.trim(), ip.trim());
                // Add directly to peer list
                boolean found = false;
                for(int i = 0; i < peerListModel.size(); i++) {
                    if(peerListModel.get(i).getIp().equals(ip.trim())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    peerListModel.addElement(manualPeer);
                }
                
                lanManager.sendText(ip.trim(), "Hello from " + lanManager.getDeviceName(), () -> {
                    JOptionPane.showMessageDialog(this, "Successfully connected to " + finalName + " (" + ip + ")!", "Connected", JOptionPane.INFORMATION_MESSAGE);
                }, err -> {
                    JOptionPane.showMessageDialog(this, "Added to list. Note: Connection ping test gave: " + err, "Manual Add", JOptionPane.INFORMATION_MESSAGE);
                });
            }
        });

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightHeader.setOpaque(false);
        rightHeader.add(lblStatusInfo);
        rightHeader.add(btnScan);
        rightHeader.add(btnManualAdd);

        topHeader.add(logoGroup, BorderLayout.WEST);
        topHeader.add(rightHeader, BorderLayout.EAST);
        add(topHeader, BorderLayout.NORTH);

        // Main Split View
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(320);
        splitPane.setDividerSize(2);

        // Left Sidebar (Users List)
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(30, 41, 59));

        JPanel sidebarHeader = new JPanel(new BorderLayout());
        sidebarHeader.setBackground(new Color(30, 41, 59));
        sidebarHeader.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel searchBox = new JPanel(new BorderLayout(8, 0));
        searchBox.setBackground(new Color(51, 65, 85));
        searchBox.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel lblSearchIcon = new JLabel("Search:");
        lblSearchIcon.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSearchIcon.setForeground(new Color(148, 163, 184));

        tfSearch = new JTextField();
        tfSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tfSearch.setForeground(Color.WHITE);
        tfSearch.setCaretColor(Color.WHITE);
        tfSearch.setBackground(new Color(51, 65, 85));
        tfSearch.setBorder(null);
        tfSearch.setToolTipText("Search connected LAN users by name or IP...");
        tfSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterPeerList(); }
            @Override public void removeUpdate(DocumentEvent e) { filterPeerList(); }
            @Override public void changedUpdate(DocumentEvent e) { filterPeerList(); }
        });

        searchBox.add(lblSearchIcon, BorderLayout.WEST);
        searchBox.add(tfSearch, BorderLayout.CENTER);
        sidebarHeader.add(searchBox, BorderLayout.CENTER);

        peerListModel = new DefaultListModel<>();
        peerJList = new JList<>(peerListModel);
        peerJList.setCellRenderer(new WebStylePeerRenderer());
        peerJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        peerJList.setBackground(new Color(30, 41, 59));
        peerJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                activePeer = peerJList.getSelectedValue();
                if (activePeer != null) {
                    lblActivePeerName.setText(activePeer.getName());
                    lblActivePeerStatus.setText("IP: " + activePeer.getIp() + "  •  Online");
                    lblActiveAvatar.setText(activePeer.getName().substring(0, 1).toUpperCase());
                }
            }
        });

        // Sidebar Empty State Label
        lblEmptySidebar = new JLabel("<html><body style='text-align:center; padding:30px 10px; color:#94A3B8;'>" +
                "<h1 style='color:#38BDF8; margin-bottom:8px;'>Searching...</h1>" +
                "<b style='color:#FFFFFF; font-size:13px;'>Searching for LAN Users...</b><br><br>" +
                "<span style='font-size:11px;'>No other WiFili users found on this Wi-Fi yet.</span><br><br>" +
                "<span style='font-size:11px; color:#38BDF8;'>Click 'Scan LAN Users' or '+ Add IP' to connect instantly!</span>" +
                "</body></html>", SwingConstants.CENTER);

        JScrollPane scrollPeers = new JScrollPane(peerJList);
        scrollPeers.setBorder(null);

        sidebar.add(sidebarHeader, BorderLayout.NORTH);
        sidebar.add(lblEmptySidebar, BorderLayout.SOUTH);
        sidebar.add(scrollPeers, BorderLayout.CENTER);
        splitPane.setLeftComponent(sidebar);

        // Right Chat Area
        JPanel chatArea = new JPanel(new BorderLayout());
        chatArea.setBackground(new Color(15, 23, 42));

        // Chat Header
        JPanel chatHeader = new JPanel(new BorderLayout());
        chatHeader.setBackground(new Color(30, 41, 59));
        chatHeader.setPreferredSize(new Dimension(760, 64));
        chatHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(71, 85, 105)),
                new EmptyBorder(8, 20, 8, 20)
        ));

        JPanel activeUserGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        activeUserGroup.setOpaque(false);

        lblActiveAvatar = new CircleLabel("?", new Color(99, 102, 241));
        lblActiveAvatar.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblActiveAvatar.setForeground(Color.WHITE);
        lblActiveAvatar.setPreferredSize(new Dimension(42, 42));

        JPanel nameGroup = new JPanel(new GridLayout(2, 1, 2, 2));
        nameGroup.setOpaque(false);

        lblActivePeerName = new JLabel("No User Selected");
        lblActivePeerName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblActivePeerName.setForeground(Color.WHITE);

        lblActivePeerStatus = new JLabel("Scan and select a LAN user on the left to start chatting");
        lblActivePeerStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblActivePeerStatus.setForeground(new Color(148, 163, 184));

        nameGroup.add(lblActivePeerName);
        nameGroup.add(lblActivePeerStatus);

        activeUserGroup.add(lblActiveAvatar);
        activeUserGroup.add(nameGroup);

        JPanel callBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        callBtns.setOpaque(false);

        JButton btnAudioCall = createStyledButton("Audio Call", new Color(34, 197, 94));
        btnAudioCall.addActionListener(e -> initiateCall("audio"));

        JButton btnVideoCall = createStyledButton("Video Call", new Color(99, 102, 241));
        btnVideoCall.addActionListener(e -> initiateCall("video"));

        JButton btnHelp = createStyledButton("How to Use", new Color(71, 85, 105));
        btnHelp.addActionListener(e -> openGuide());

        callBtns.add(btnAudioCall);
        callBtns.add(btnVideoCall);
        callBtns.add(btnHelp);

        chatHeader.add(activeUserGroup, BorderLayout.WEST);
        chatHeader.add(callBtns, BorderLayout.EAST);
        chatArea.add(chatHeader, BorderLayout.NORTH);

        // Chat Timeline
        chatMessagesPanel = new JPanel();
        chatMessagesPanel.setLayout(new BoxLayout(chatMessagesPanel, BoxLayout.Y_AXIS));
        chatMessagesPanel.setBackground(new Color(15, 23, 42));
        chatMessagesPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Initial Watermark Message
        JLabel lblWatermark = new JLabel("<html><body style='text-align:center; color:#94A3B8; font-size:12px;'>" +
                "End-to-End Direct LAN Wi-Fi Connection • 100% Offline & Private" +
                "</body></html>", SwingConstants.CENTER);
        lblWatermark.setAlignmentX(Component.CENTER_ALIGNMENT);
        chatMessagesPanel.add(lblWatermark);
        chatMessagesPanel.add(Box.createVerticalStrut(16));

        JScrollPane scrollMessages = new JScrollPane(chatMessagesPanel);
        scrollMessages.setBorder(null);
        scrollMessages.getVerticalScrollBar().setUnitIncrement(16);
        chatArea.add(scrollMessages, BorderLayout.CENTER);

        // Bottom Input Bar
        JPanel inputBar = new JPanel(new BorderLayout(12, 0));
        inputBar.setBackground(new Color(30, 41, 59));
        inputBar.setPreferredSize(new Dimension(760, 64));
        inputBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(71, 85, 105)),
                new EmptyBorder(12, 20, 12, 20)
        ));

        JButton btnEmoji = createStyledButton("Emoji", new Color(71, 85, 105));
        btnEmoji.addActionListener(e -> showEmojiPicker());

        JButton btnAttach = createStyledButton("Attach File", new Color(71, 85, 105));
        btnAttach.addActionListener(e -> attachAndSendFile());

        JPanel leftInputBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftInputBtns.setOpaque(false);
        leftInputBtns.add(btnEmoji);
        leftInputBtns.add(btnAttach);

        JPanel inputBoxWrapper = new JPanel(new BorderLayout());
        inputBoxWrapper.setBackground(new Color(51, 65, 85));
        inputBoxWrapper.setBorder(new EmptyBorder(6, 16, 6, 16));

        tfMessage = new JTextField();
        tfMessage.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tfMessage.setForeground(Color.WHITE);
        tfMessage.setCaretColor(Color.WHITE);
        tfMessage.setBackground(new Color(51, 65, 85));
        tfMessage.setBorder(null);
        tfMessage.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) sendTextMessage();
            }
        });

        inputBoxWrapper.add(tfMessage, BorderLayout.CENTER);

        JButton btnSend = createStyledButton("Send ➔", new Color(30, 101, 243));
        btnSend.addActionListener(e -> sendTextMessage());

        inputBar.add(leftInputBtns, BorderLayout.WEST);
        inputBar.add(inputBoxWrapper, BorderLayout.CENTER);
        inputBar.add(btnSend, BorderLayout.EAST);

        chatArea.add(inputBar, BorderLayout.SOUTH);
        splitPane.setRightComponent(chatArea);

        add(splitPane, BorderLayout.CENTER);
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    static class CircleLabel extends JLabel {
        private Color bgColor;

        public CircleLabel(String text, Color bgColor) {
            super(text, SwingConstants.CENTER);
            this.bgColor = bgColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class WebStylePeerRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel panel = new JPanel(new BorderLayout(12, 0));
            panel.setBorder(new EmptyBorder(10, 14, 10, 14));

            if (value instanceof DesktopLanManager.DesktopPeer) {
                DesktopLanManager.DesktopPeer peer = (DesktopLanManager.DesktopPeer) value;

                CircleLabel avatar = new CircleLabel(peer.getName().substring(0, 1).toUpperCase(), new Color(99, 102, 241));
                avatar.setFont(new Font("Segoe UI", Font.BOLD, 18));
                avatar.setForeground(Color.WHITE);
                avatar.setPreferredSize(new Dimension(44, 44));

                JPanel info = new JPanel(new GridLayout(2, 1, 2, 2));
                info.setOpaque(false);

                JLabel name = new JLabel(peer.getName());
                name.setFont(new Font("Segoe UI", Font.BOLD, 15));

                JLabel sub = new JLabel("IP: " + peer.getIp() + "  •  Online");
                sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));

                if (isSelected) {
                    panel.setBackground(new Color(51, 65, 85));
                    name.setForeground(Color.WHITE);
                    sub.setForeground(new Color(148, 163, 184));
                } else {
                    panel.setBackground(new Color(30, 41, 59));
                    name.setForeground(Color.WHITE);
                    sub.setForeground(new Color(148, 163, 184));
                }

                info.add(name);
                info.add(sub);

                panel.add(avatar, BorderLayout.WEST);
                panel.add(info, BorderLayout.CENTER);
            }
            return panel;
        }
    }

    private void filterPeerList() {
        String q = tfSearch.getText().trim().toLowerCase(Locale.ROOT);
        peerListModel.clear();
        List<DesktopLanManager.DesktopPeer> peers = lanManager.getPeersList();
        for (DesktopLanManager.DesktopPeer p : peers) {
            if (p.getName().toLowerCase(Locale.ROOT).contains(q) || p.getIp().contains(q)) {
                peerListModel.addElement(p);
            }
        }
        lblEmptySidebar.setVisible(peerListModel.isEmpty());
    }

    private String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private void showEmojiPicker() {
        String[] emojis = {"😀", "😂", "😍", "😎", "👍", "❤️", "🔥", "🎉", "🙏", "👋", "🥳", "💡", "💯", "📌", "🚀", "💬", "🙌", "😊"};
        String selected = (String) JOptionPane.showInputDialog(this, "Select Emoji:", "Emoji Picker",
                JOptionPane.PLAIN_MESSAGE, null, emojis, emojis[0]);
        if (selected != null) {
            tfMessage.setText(tfMessage.getText() + selected);
            tfMessage.requestFocus();
        }
    }

    private void initiateCall(String callType) {
        if (activePeer == null) {
            JOptionPane.showMessageDialog(this,
                    "⚠️ You are not connected to any user.\n\nHow to Fix:\n1. Select an online user from the left list.\n2. Or click 'Scan LAN Users' / '+ Add IP' to connect.",
                    "Not Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        lanManager.sendCallSignal(activePeer.getIp(), 10, callType);
        openCallFrame(activePeer.getName(), activePeer.getIp(), callType, false);
    }

    private void openCallFrame(String peerName, String peerIp, String callType, boolean isIncoming) {
        if (callFrame != null) callFrame.dispose();

        callFrame = new JFrame(callType.toUpperCase() + " Call • WiFili Desktop");
        callFrame.setSize(520, 440);
        callFrame.setLocationRelativeTo(this);
        callFrame.setLayout(new BorderLayout());

        JPanel callInfo = new JPanel(new GridLayout(3, 1, 4, 4));
        callInfo.setBackground(new Color(15, 23, 42));
        callInfo.setBorder(new EmptyBorder(24, 24, 24, 24));

        CircleLabel lblAvatar = new CircleLabel(peerName.substring(0, 1).toUpperCase(), new Color(99, 102, 241));
        lblAvatar.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblAvatar.setForeground(Color.WHITE);
        lblAvatar.setPreferredSize(new Dimension(72, 72));

        JLabel lblName = new JLabel("User: " + peerName + " (" + peerIp + ")", SwingConstants.CENTER);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblName.setForeground(Color.WHITE);

        lblCallTimer = new JLabel(isIncoming ? "Incoming " + callType + " call..." : "Calling " + peerName + "...", SwingConstants.CENTER);
        lblCallTimer.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblCallTimer.setForeground(new Color(148, 163, 184));

        callInfo.add(lblAvatar);
        callInfo.add(lblName);
        callInfo.add(lblCallTimer);

        callFrame.add(callInfo, BorderLayout.NORTH);

        lblVideoDisplay = new JLabel("Live Video Stream Area", SwingConstants.CENTER);
        lblVideoDisplay.setOpaque(true);
        lblVideoDisplay.setBackground(Color.BLACK);
        lblVideoDisplay.setForeground(Color.GRAY);
        if ("video".equalsIgnoreCase(callType)) {
            callFrame.add(lblVideoDisplay, BorderLayout.CENTER);
        }

        JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 16));
        ctrlPanel.setBackground(new Color(15, 23, 42));

        JButton btnMute = createStyledButton("Mute Mic", new Color(51, 65, 85));
        btnMute.addActionListener(e -> lanManager.toggleMute());

        JButton btnHangup = createStyledButton("Hangup", new Color(239, 68, 68));
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
            JOptionPane.showMessageDialog(this,
                    "⚠️ You are not connected to any user.\n\nHow to Fix:\n1. Select an online user from the left list.\n2. Or click 'Scan LAN Users' / '+ Add IP' to connect.",
                    "Not Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String text = tfMessage.getText().trim();
        if (text.isEmpty()) return;

        tfMessage.setText("");
        appendMessage("You", text, new Color(30, 101, 243), true);

        lanManager.sendText(activePeer.getIp(), text, null, err ->
                JOptionPane.showMessageDialog(this, "You are not connected to " + activePeer.getName() + ".\n\n" + err, "Send Failed", JOptionPane.ERROR_MESSAGE));
    }

    private void attachAndSendFile() {
        if (activePeer == null) {
            JOptionPane.showMessageDialog(this,
                    "⚠️ You are not connected to any user.\n\nHow to Fix:\n1. Select an online user from the left list.\n2. Or click 'Scan LAN Users' / '+ Add IP' to connect.",
                    "Not Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            appendMessage("You", "Sending file: " + file.getName() + " (" + file.length() / 1024 + " KB)...", new Color(30, 101, 243), true);
            lanManager.sendFile(activePeer.getIp(), file, "file",
                    pct -> {},
                    () -> appendMessage("You", "Sent file: " + file.getName(), new Color(30, 101, 243), true),
                    err -> JOptionPane.showMessageDialog(this, "You are not connected to " + activePeer.getName() + ".\n\n" + err, "File Send Failed", JOptionPane.ERROR_MESSAGE));
        }
    }

    private void openGuide() {
        JOptionPane.showMessageDialog(this,
                "How to Use WiFili Desktop (Linux & Windows):\n\n" +
                "1. Connect both devices to the SAME Wi-Fi or Mobile Hotspot.\n" +
                "2. Click 'Scan LAN Users' or '+ Add IP' to discover connected devices on your network.\n" +
                "3. Click any user on the left list to start chatting, sending files, or calling offline!",
                "How to Use", JOptionPane.INFORMATION_MESSAGE);
    }

    private void appendMessage(String sender, String body, Color headerColor, boolean isSentByMe) {
        JPanel msgRow = new JPanel(new FlowLayout(isSentByMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        msgRow.setOpaque(false);

        JPanel msgBubble = new JPanel(new BorderLayout(4, 4));
        msgBubble.setBackground(isSentByMe ? new Color(30, 101, 243) : new Color(51, 65, 85));
        msgBubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isSentByMe ? new Color(30, 101, 243) : new Color(71, 85, 105), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));

        String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        JLabel lblSender = new JLabel(sender + " • " + timeStr + (isSentByMe ? " ✓✓" : ""));
        lblSender.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblSender.setForeground(isSentByMe ? new Color(224, 242, 254) : new Color(148, 163, 184));

        JLabel lblBody = new JLabel("<html><body style='width: 400px; color:#FFFFFF;'>" + body + "</body></html>");
        lblBody.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        msgBubble.add(lblSender, BorderLayout.NORTH);
        msgBubble.add(lblBody, BorderLayout.CENTER);

        msgRow.add(msgBubble);

        chatMessagesPanel.add(msgRow);
        chatMessagesPanel.add(Box.createVerticalStrut(10));
        chatMessagesPanel.revalidate();
        chatMessagesPanel.repaint();
    }

    @Override
    public void onPeersUpdated(List<DesktopLanManager.DesktopPeer> peerList) {
        SwingUtilities.invokeLater(() -> filterPeerList());
    }

    @Override
    public void onMessageReceived(String senderIp, String senderName, String body, String type, String filePath, String fileName) {
        SwingUtilities.invokeLater(() -> appendMessage(senderName, body, new Color(148, 163, 184), false));
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
            if (lblCallTimer != null) lblCallTimer.setText("Connected • 00:00");
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
