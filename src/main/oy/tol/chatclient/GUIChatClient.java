package oy.tol.chatclient;

import javax.swing.*;
import javax.swing.border.Border;

import java.awt.*;

import oy.tol.chat.ChangeTopicMessage;
import oy.tol.chat.ChatMessage;
import oy.tol.chat.ErrorMessage;
import oy.tol.chat.ListChannelsMessage;
import oy.tol.chat.Message;
import oy.tol.chat.StatusMessage;

import java.time.LocalDateTime;
import java.util.List;

public class GUIChatClient extends JFrame implements ChatClientDataProvider {

    private static String DEFAULT_USER_NAME = "DefaultUser";
    private static boolean DEFAULT_COMPONENT_VISIBILITY = false;

    private JPanel channelListPanel;

    private JPanel messageArea;
    private JTextField messageField;

    private boolean running = true;
    private String username = DEFAULT_USER_NAME;
    private String currentChannel = "";
    private ChatTCPClient tcpClient = null;
    private int serverPort = 10000;
    private String currentServer = "localhost";

    private boolean channelVisibility = true; //DEFAULT_COMPONENT_VISIBILITY;

    private JButton btnChangeName;

    public GUIChatClient() {
        setTitle("Chat Client");
        setSize(1000, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(createLeftPanel(), BorderLayout.WEST);
        add(createCenterPanel(), BorderLayout.CENTER);

        tcpClient = new ChatTCPClient(this);
        new Thread(tcpClient).start();

        setVisible(true);
        updateChannelList();
    }

    // Käyttöliittymän vasen paneeli
    private JPanel createLeftPanel() { 
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(200, 0));

        // Yläpaneeli
        JPanel topPanel = new JPanel(new BorderLayout());

        JButton btnBack = new JButton("←");
        btnBack.setFocusable(false); 
        btnBack.setMargin(new Insets(2, 8, 2, 8));
        btnBack.addActionListener(e -> returnToMainView());

        btnChangeName = new JButton(username);
        btnChangeName.setFocusable(false);
        btnChangeName.addActionListener(e -> openNameChange());

        topPanel.add(btnBack, BorderLayout.WEST);
        topPanel.add(btnChangeName, BorderLayout.CENTER);

        panel.add(topPanel, BorderLayout.NORTH);   

        channelListPanel = new JPanel();
        channelListPanel.setLayout(new BoxLayout(channelListPanel, BoxLayout.Y_AXIS));

        panel.add(new JScrollPane(channelListPanel), BorderLayout.CENTER);

        // Alapaneeli
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1));

        JButton btnNewChannel = new JButton("+ New Channel");
        btnNewChannel.addActionListener(e -> openNewChannel());

        JLabel connectionStatus = new JLabel(" Connected");

        bottomPanel.add(btnNewChannel, BorderLayout.NORTH);
        bottomPanel.add(connectionStatus, BorderLayout.SOUTH);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // Keskipaneeli 
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Yläpaneeli
        JPanel topPanel = new JPanel(new BorderLayout());

        JButton btnHelp = new JButton("Help");
        btnHelp.addActionListener(e -> openHelp());

        JLabel channelTitle = new JLabel(currentChannel);
        channelTitle.setVisible(channelVisibility);

        // TODO Kuvake tekstin tilalle
        JButton btnChangeChannelTopic = new JButton("Change topic");
        btnChangeChannelTopic.addActionListener(e -> openTopicChange());
        btnChangeChannelTopic.setVisible(channelVisibility);

        topPanel.add(channelTitle, BorderLayout.CENTER);
        topPanel.add(btnHelp, BorderLayout.EAST);
        topPanel.add(btnChangeChannelTopic, BorderLayout.WEST);

        panel.add(topPanel, BorderLayout.NORTH); 

        // Viestikenttä
        messageArea = new JPanel();
        JScrollPane scroll = new JScrollPane(messageArea);
        messageArea.setLayout(new BoxLayout(messageArea, BoxLayout.Y_AXIS));

        // Alapaneeli
        JPanel bottomPanel = new JPanel(new BorderLayout());

        messageField = new JTextField();
        JButton btnSend = new JButton("Send");


        btnSend.addActionListener(e -> sendMessage());

        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(btnSend, BorderLayout.EAST);
        bottomPanel.setVisible(channelVisibility);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // Viestin lähettäminen TODO? VIESTIN NÄKYMINEN PALVELIMEN TOIMINNASTA RIIPPUEN
    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                    "Message cannot be empty",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        tcpClient.postChatMessage(text);
        messageField.setText("");
    }


    // Normaalin viestin lisääminen viestialueelle
    private void addMessage(String text, String nick) {
        JPanel messagePanel = new JPanel(new BorderLayout());
        JLabel message = new JLabel(text);
        JLabel nickName = new JLabel(nick);

        message.setHorizontalAlignment(SwingConstants.LEFT);
        nickName.setHorizontalAlignment(SwingConstants.LEFT);

        messagePanel.add(message, BorderLayout.CENTER);
        messagePanel.add(nickName, BorderLayout.SOUTH);

        messageArea.add(messagePanel);
    }

    // Yksityisviestin lisääminen viestialueelle
    private void addPrivateMessage(String text, String nick) {
        JPanel messagePanel = new JPanel(new BorderLayout());
        JLabel message = new JLabel(text);
        JLabel nickName = new JLabel("Private Message - " + nick);

        message.setHorizontalAlignment(SwingConstants.LEFT);
        nickName.setHorizontalAlignment(SwingConstants.LEFT);

        message.setBackground(new Color(134, 151, 176));

        messagePanel.add(message, BorderLayout.CENTER);
        messagePanel.add(nickName, BorderLayout.SOUTH);

        messageArea.add(messagePanel);
    }

    //TODO perusnäkymään palaaminen
    private void returnToMainView() {
        channelVisibility = false;
    }

    // Käyttäjänimen vaihtaminen
    private void openNameChange() {
        JDialog dialog = new JDialog(this, "Change username", true);
        dialog.setLayout(new GridLayout(3,1));
        dialog.setSize(300, 180);

        JTextField usernameField = new JTextField(username);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2));

        JButton btnChangeUsername = new JButton("Change Username");
        btnChangeUsername.addActionListener(e -> {
            String newUsername = usernameField.getText().trim();
            if (newUsername.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Username cannot be empty",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            username = newUsername;

            btnChangeName.setText(newUsername);

            dialog.dispose();
        });

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnCancel);
        btnPanel.add(btnChangeUsername);

        dialog.add(new JLabel(" New Username:"));
        dialog.add(usernameField);
        dialog.add(btnPanel);

        dialog.setVisible(true);
    }

    // Uuden kanavan luominen
    private void openNewChannel() {
        JDialog dialog = new JDialog(this, "Create New Channel", true);
        dialog.setLayout(new GridLayout(3,1));
        dialog.setSize(300, 180);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2));

        JTextField field = new JTextField();
        JButton btnCreate = new JButton("Create");
        JButton btnCancel = new JButton("Cancel");

        btnCreate.addActionListener(e -> {
            String topic = field.getText().trim();
            if (topic.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Channel topic cannot be empty",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            joinChannel(topic);
            dialog.dispose();
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnCancel);
        btnPanel.add(btnCreate);

        dialog.add(new JLabel(" Channel topic:"));
        dialog.add(field);
        dialog.add(btnPanel);
        dialog.setVisible(true);
    }

    // Help-osion avaaminen TODO kunnon ohjeet
    private void openHelp() {
        JOptionPane.showMessageDialog(this,
                "Help:\n- Select a channel from the list\n"
              + "- Type a message at the bottom\n"
              + "- Use Settings to change username\n"
              + "- Use + New Channel to create a channel\n",
                "Help",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // Kanavan aiheen vaihtaminen
    private void openTopicChange() {
        JDialog dialog = new JDialog(this, "Change channel topic", true);
        dialog.setLayout(new GridLayout(3,1));
        dialog.setSize(300, 180);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2));

        JTextField field = new JTextField();

        JButton btnChangeTopic = new JButton("Change topic");
        btnChangeTopic.addActionListener(e -> {
            String newTopic = field.getText().trim();
            if (newTopic.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Channel topic cannot be empty",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            tcpClient.changeTopicTo(newTopic);
            dialog.dispose();
        });

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnCancel);
        btnPanel.add(btnChangeTopic);

        dialog.add(new JLabel("New channel topic:"));
        dialog.add(field);
        dialog.add(btnPanel);
        dialog.setVisible(true);
    }

    // Päivitetään kanavalista, kun client käynnistetään. Sen jälkeen 5 sekunnin välein 
    // (käyttäjän ei tarvitse manuaalisti päivittää napista tms.)
    private void updateChannelList() {
        int delay = 5000;

        requestChannels();

        new javax.swing.Timer(delay, e -> requestChannels()).start();   
    }

    // Kanavalistan nappien lissäminen
    private void addChannelButton(String channelName) {
        JButton btn = new JButton("# " + channelName);

        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));  
        btn.setMinimumSize(new Dimension(0, 20));
        btn.setPreferredSize(new Dimension(0, 20));

        String channelNameString = channelName.replaceAll("\\s*\\(\\d+\\)$", "");
    
        btn.addActionListener(e -> {
            joinChannel(channelNameString.trim().toLowerCase());
        });

        channelListPanel.add(btn);
        channelListPanel.revalidate();
        channelListPanel.repaint();
    }

    // Kanavalle liittyminen
    private void joinChannel(String channelName) {
        tcpClient.changeChannelTo(channelName);
        currentChannel = channelName;
        channelVisibility = true;
        messageArea.removeAll();
        requestChannels();
    }

    // Pyydetään palvelimelta kanavalista
    private void requestChannels() {
        if (tcpClient != null && tcpClient.isConnected()) {
            tcpClient.listChannels();
        }
    }

    // TODO implementoi message handling
    @Override
	public boolean handleReceived(Message message) {
		boolean continueReceiving = true;
		switch (message.getType()) {
            // Lisätään uudet viestit viestikenttään
			case Message.CHAT_MESSAGE: {
				if (message instanceof ChatMessage) {
					ChatMessage msg = (ChatMessage)message;
					if (msg.isDirectMessage()) {
						addPrivateMessage(msg.getMessage(), msg.getNick());
					} else {
						addMessage(msg.getMessage(), msg.getNick());
					}
				}
				break;
			}

            // Kanavien lisääminen listaan
			case Message.LIST_CHANNELS: {
				ListChannelsMessage msg = (ListChannelsMessage)message;
				List<String> channels = msg.getChannels();
				if (null != channels) {
                    SwingUtilities.invokeLater(() -> {
                        channelListPanel.removeAll();
                            for (String c : channels) {
                                addChannelButton(c);
                            }
                            channelListPanel.revalidate();
                            channelListPanel.repaint();
                    });
				}
				break;
			}

            // Muutetaan kanavan aihetta
			case Message.CHANGE_TOPIC: {
				ChangeTopicMessage msg = (ChangeTopicMessage)message;
                currentChannel = msg.getTopic();
				break;
			}

			case Message.STATUS_MESSAGE: {
				StatusMessage msg = (StatusMessage)message;
				//TODO lisää status?
				break;
			}

			/* case Message.ERROR_MESSAGE: {
				ErrorMessage msg = (ErrorMessage)message;
				printPrompt(LocalDateTime.now(), "SERVER", msg.getError(), colorError);
				if (msg.requiresClientShutdown()) {
					continueReceiving = false;
					running = false;
					println("\nPress enter", colorError);
				}
				break;
			} */

			default:
				break;
		}
		return continueReceiving;
	} 

    @Override
	public String getServer() {
		return currentServer;
	}

	@Override
	public int getPort() {
        return serverPort;
	}

	@Override
	public String getNick() {
		return username;
	}

    @Override
	public void connectionClosed() {
		running = false;
	}

    // Main metodi, josta GUI client ajetaan
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUIChatClient::new);
    }
}
