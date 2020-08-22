package network;

import java.net.Socket;
import java.sql.SQLException;

public interface SocketThreadListener {
    void onSocketStart(SocketThread thread, Socket socket);
    void onSocketStop(SocketThread thread);

    void onSocketReady(SocketThread thread, Socket socket);
    void onReceiveString(SocketThread thread, Socket socket, String msg) throws SQLException;

    void onSocketException(SocketThread thread, Exception exception);
}
