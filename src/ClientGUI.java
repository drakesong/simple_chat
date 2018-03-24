import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClientGUI extends JFrame implements ActionListener {

    private static final long serialVersionUID = 2018L;
    private JLabel label;
    private JTextField textField;
    private JTextField tfServer, tfPort;
    private JButton login, logout, whoIsIn;
    private JTextArea textArea;
    private boolean connected;
    private Client client;
    private int defaultPort;
    private String defaultHost;

    ClientGUI(String host, int port) {
        super("Chat Client");
        defaultPort = port;
        defaultHost = host;

        JPanel northPanel = new JPanel(new GridLayout(3, 1));
        JPanel serverAndPort = new JPanel(new GridLayout(1, 5, 1, 3));
        tfServer = new JTextField(host);
        tfPort = new JTextField("" + port);
        tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

        serverAndPort.add(new JLabel("Server Address:  "));
        serverAndPort.add(tfServer);
        serverAndPort.add(new JLabel("Port Number:  "));
        serverAndPort.add(tfPort);
        serverAndPort.add(new JLabel(""));
        northPanel.add(serverAndPort);

        label = new JLabel("Enter your username below", SwingConstants.CENTER);
        northPanel.add(label);
        textField = new JTextField("Anonymous");
        textField.setBackground(Color.WHITE);
        northPanel.add(textField);
        add(northPanel, BorderLayout.NORTH);

        textArea = new JTextArea("Welcome to the Chat room\n", 80, 80);
        JPanel centerPanel = new JPanel(new GridLayout(1, 1));
        centerPanel.add(new JScrollPane(textArea));
        textArea.setEditable(false);
        add(centerPanel, BorderLayout.CENTER);

        login = new JButton("Login");
        login.addActionListener(this);
        logout = new JButton("Logout");
        logout.addActionListener(this);
        logout.setEnabled(false);
        whoIsIn = new JButton("Who is in");
        whoIsIn.addActionListener(this);
        whoIsIn.setEnabled(false);

        JPanel southPanel = new JPanel();
        southPanel.add(login);
        southPanel.add(logout);
        southPanel.add(whoIsIn);
        add(southPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        setVisible(true);
        textField.requestFocus();
    }

    void append(String str) {
        textArea.append(str);
        textArea.setCaretPosition(textArea.getText().length() - 1);
    }

    void connectionFailed() {
        login.setEnabled(true);
        logout.setEnabled(false);
        whoIsIn.setEnabled(false);
        label.setText("Enter your username below");
        textField.setText("Anonymous");
        tfPort.setText("" + defaultPort);
        tfServer.setText(defaultHost);
        tfServer.setEditable(false);
        tfPort.setEditable(false);
        textField.removeActionListener(this);
        connected = false;
    }

    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        if (o == logout) {
            client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
            return;
        }

        if (o == whoIsIn) {
            client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));
            return;
        }

        if (connected) {
            client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, textField.getText()));
            textField.setText("");
            return;
        }

        if (o == login) {
            String username = textField.getText().trim();
            if (username.length() == 0) {
                return;
            }
            String server = tfServer.getText().trim();
            if (server.length() == 0) {
                return;
            }
            String portNumber = tfPort.getText().trim();
            if (portNumber.length() == 0) {
                return;
            }
            int port = 0;
            try {
                port = Integer.parseInt(portNumber);
            } catch (Exception en) {
                return;
            }

            client = new Client(server, port, username, this);
            if (!client.start()) {
                return;
            }
            textField.setText("");
            label.setText("Enter your message below");
            connected = true;

            login.setEnabled(false);
            logout.setEnabled(true);
            whoIsIn.setEnabled(true);
            tfServer.setEditable(false);
            tfPort.setEditable(false);
            textField.addActionListener(this);
        }
    }

    public static void main(String[] args) {
        new ClientGUI("localhost", 1500);
    }
}
