import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Server {
    private static int uniqueId;
    private ArrayList<ClientThread> clientList;
    private ServerGUI serverGUI;
    private SimpleDateFormat simpleDate;
    private int port;
    private boolean keepGoing;

    public Server(int port) {
        this(port, null);
    }

    public Server(int port, ServerGUI serverGUI) {
        this.serverGUI = serverGUI;
        this.port = port;
        simpleDate = new SimpleDateFormat("HH:mm:ss");
        clientList = new ArrayList<ClientThread>();
    }

    public void start() {
        keepGoing = true;
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (keepGoing) {
                display("Server waiting for Clients on port " + port + ".");

                Socket socket = serverSocket.accept();
                if (!keepGoing) {
                    break;
                }
                ClientThread clientThread = new ClientThread(socket);
                clientList.add(clientThread);
                clientThread.start();
            }

            try {
                serverSocket.close();
                for (int i = 0; i < clientList.size(); i++) {
                    ClientThread tc = clientList.get(i);
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    } catch (IOException e) {
                        display("Exception: " + e);
                    }
                }
            } catch (Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        } catch (IOException e) {
            String msg = simpleDate.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }

    protected void stop() {
        keepGoing = false;
        try {
            new Socket("localhost", port);
        } catch (Exception e) {
            display("Exception: " + e);
        }
    }

    private void display(String msg) {
        String time = simpleDate.format(new Date()) + " " + msg;
        if (serverGUI == null) {
            System.out.println(time);
        } else {
            serverGUI.appendEvent(time + "\n");
        }
    }

    private synchronized void broadcast(String message) {
        String time = simpleDate.format(new Date());
        String messageLf = time + " " + message + "\n";

        if (serverGUI == null) {
            System.out.print(messageLf);
        } else {
            serverGUI.appendRoom(messageLf);
        }

        for (int i = clientList.size(); i >= 0; i--) {
            ClientThread clientThread = clientList.get(i);

            if (!clientThread.writeMsg(messageLf)) {
                clientList.remove(i);
                display("Disconnected Client " + clientThread.username + " removed from list.");
            }
        }
    }

    synchronized void remove(int id) {
        for (int i = 0; i < clientList.size(); i++) {
            ClientThread clientThread = clientList.get(i);

            if (clientThread.id == id) {
                clientList.remove(i);
                return;
            }
        }
    }

    public static void main(String[] args) {
        int portNumber = 1500;
        switch (args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;
        }

        Server server = new Server(portNumber);
        server.start();
    }

    class ClientThread extends Thread {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage chatMessage;
        String date;

        ClientThread(Socket socket) {
            id = uniqueId++;
            this.socket = socket;
            System.out.println("Thread trying to create Object Input/Output Streams");

            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
                display(username + " just connected.");
            } catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            } catch (ClassNotFoundException e) {
                display("Exception: " + e);
            }
            date = new Date().toString() + "\n";
        }

        public void run() {
            boolean keepGoing = true;
            while (keepGoing) {
                try {
                    chatMessage = (ChatMessage) sInput.readObject();
                } catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                } catch (ClassNotFoundException e) {
                    break;
                }

                String message = chatMessage.getMessage();
                switch (chatMessage.getType()) {
                    case ChatMessage.MESSAGE:
                        broadcast(username + ": " + message);
                        break;
                    case ChatMessage.LOGOUT:
                        display(username + " disconnected with a LOGOUT message.");
                        keepGoing = false;
                        break;
                    case ChatMessage.WHOISIN:
                        writeMsg("List of the users connected at " + simpleDate.format(new Date()) + "\n");
                        for (int i = 0; i < clientList.size(); i++) {
                            ClientThread clientThread = clientList.get(i);
                            writeMsg((i + 1) + ") " + clientThread.username + " since " + clientThread.date);
                        }
                        break;
                }
            }
            remove(id);
            close();
        }

        private void close() {
            try {
                if (sOutput != null) {
                    sOutput.close();
                }
            } catch (Exception e) {
                display("Exception: " + e);
            }

            try {
                if (sInput != null) {
                    sInput.close();
                }
            } catch (Exception e) {
                display("Exception: " + e);
            }

            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
                display("Exception: " + e);
            }
        }

        private boolean writeMsg(String msg) {
            if (!socket.isConnected()) {
                close();
                return false;
            }

            try {
                sOutput.writeObject(msg);
            } catch (IOException e) {
                display("Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }
    }
}

