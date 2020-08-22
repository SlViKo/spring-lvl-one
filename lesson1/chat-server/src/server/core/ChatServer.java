package server.core;

import common.Library;
import network.ServerSocketThread;
import network.ServerSocketThreadListener;
import network.SocketThread;
import network.SocketThreadListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;

@Service
public class ChatServer implements ServerSocketThreadListener, SocketThreadListener {

    private static final Logger logger = Logger.getLogger(ChatServer.class.getName()); // использовал стандартный логер JAVA 3-6
    private final ChatServerListener listener;
    private ServerSocketThread server;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss: ");
    private Vector<SocketThread> clients = new Vector<>();
    private ExecutorService executor; // Java 3-4

    public ChatServer(ChatServerListener listener) {
        this.listener = listener;
    }

    public void start(int port) {
        if (server != null && server.isAlive())
            putLog("Server already started");
        else {
            //xecutor = Executors.newFixedThreadPool(4); // инизиализирую ExecutorService Java 3-4
            server = new ServerSocketThread(this, "Server", port, 2000);
            //executor.execute(new ServerSocketThread(this, "Server", port, 2000));
            try {
                Handler h = new FileHandler("logg.log", true);
                h.setFormatter(new SimpleFormatter());  // java 3-6
                logger.addHandler(h);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        if (server == null || !server.isAlive()) {
            putLog("Server is not running");
        } else {
            server.interrupt();
        }
        //executor.shutdownNow();

    }

    private void putLog(String msg) {
        msg = DATE_FORMAT.format(System.currentTimeMillis()) +
                Thread.currentThread().getName() + ": " + msg;
        listener.onChatServerMessage(msg);
        logger.log(Level.INFO, msg); // добавление записи в лог сервера JAVA 3-6
    }

    /**
     * Server methods
     */

    @Override
    public void onServerStart(ServerSocketThread thread) {
        putLog("Server thread started");
        SqlClient.connect();
    }

    @Override
    public void onServerStop(ServerSocketThread thread) {
        putLog("Server thread stopped");
        SqlClient.disconnect();
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).close();
        }

    }

    @Override
    public void onServerSocketCreated(ServerSocketThread thread, ServerSocket server) {
        putLog("Server socket created");

    }

    @Override
    public void onServerTimeout(ServerSocketThread thread, ServerSocket server) {
//        putLog("Server timeout");

    }

    @Override
    public void onSocketAccepted(ServerSocketThread thread, ServerSocket server, Socket socket) {
        putLog("Client connected");
        String name = "SocketThread " + socket.getInetAddress() + ":" + socket.getPort();
        //executor.execute(new ClientThread(this, name, socket)); // добавил поток созданного сокета после подключения в  ExecutorService в Java 3-4
        new ClientThread(this, name, socket);
    }

    @Override
    public void onServerException(ServerSocketThread thread, Throwable exception) {
        exception.printStackTrace();
    }

    /**
     * Socket methods
     */

    @Override
    public synchronized void onSocketStart(SocketThread thread, Socket socket) {
        putLog("Socket created");

    }

    @Override
    public synchronized void onSocketStop(SocketThread thread) {
        ClientThread client = (ClientThread) thread;
        clients.remove(thread);
        if (client.isAuthorized() && !client.isReconnecting()) {
            sendToAuthClients(Library.getTypeBroadcast("Server",
                    client.getNickname() + " disconnected"));
        }
        sendToAuthClients(Library.getUserList(getUsers()));

    }

    @Override
    public synchronized void onSocketReady(SocketThread thread, Socket socket) {

        clients.add(thread);
    }

    @Override
    public synchronized void onReceiveString(SocketThread thread, Socket socket, String msg) throws SQLException {
        ClientThread client = (ClientThread) thread;
        if (client.isAuthorized()) {
            handleAuthMessage(client, msg);
        } else {
            handleNonAuthMessage(client, msg);
        }
    }

    private void handleNonAuthMessage(ClientThread client, String msg) {
        String[] arr = msg.split(Library.DELIMITER);
        if (arr.length != 4 || !arr[0].equals(Library.AUTH_REQUEST)) {
            client.msgFormatError(msg);
            return;
        }
        String login = arr[2];
        String password = arr[3];
        String nickname;
        if (arr[1].equals("Login")) {
            nickname = SqlClient.getNickname(login, password);
            if (nickname == null) {
                putLog("Invalid login attempt: " + login);
                client.authFail();
                return;
            } else {
                ClientThread oldClient = findClientByNickname(nickname);
                client.authAccept(nickname);
                if (oldClient == null) {
                    sendToAuthClients(Library.getTypeBroadcast("Server", nickname + " connected"));
                } else {
                    oldClient.reconnect();
                    clients.remove(oldClient);
                }
            }
        } else if (arr[1].equals("Registration")) { // добавление обработки регитсрации нового пользователя Java 3-2
            nickname = SqlClient.getNickname(login);
            if (nickname == null) {
                try {
                    SqlClient.createUser(login, password);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                putLog("Invalid login attempt: " + login);
                client.authFail();
                return;
            }
        }
        sendToAuthClients(Library.getUserList(getUsers()));
    }

    private void handleAuthMessage(ClientThread client, String msg) throws SQLException {
        String[] arr = msg.split(Library.DELIMITER);
        String msgType = arr[0];
        switch (msgType) {
            case Library.TYPE_BCAST_CLIENT:
                sendToAuthClients(Library.getTypeBroadcast(
                        client.getNickname(), arr[1]));
                break;
            case Library.CHANGE_NICK: //добавление изменения ника пользователя Java 3-2
                String login = arr[1];
                String password = arr[2];
                String newNickname = arr[3];
                if (!SqlClient.isNickname(newNickname)) {
                    String nickname = SqlClient.getNickname(login, password);
                    ClientThread oldClient = findClientByNickname(nickname);
                    oldClient.reconnect();
                    clients.remove(oldClient);
                    SqlClient.updateNickname(login, password, newNickname);
                }
                break;
            default:
                client.sendMessage(Library.getMsgFormatError(msg));
        }
    }

    // launch4j
    private void sendToAuthClients(String msg) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            client.sendMessage(msg);
        }
    }

    @Override
    public synchronized void onSocketException(SocketThread thread, Exception exception) {
        exception.printStackTrace();
    }

    private String getUsers() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            sb.append(client.getNickname()).append(Library.DELIMITER);
        }
        return sb.toString();
    }

    private synchronized ClientThread findClientByNickname(String nickname) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            if (client.getNickname().equals(nickname))
                return client;
        }
        return null;
    }

}
