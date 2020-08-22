package client;

import common.Library;
import network.SocketThread;
import network.SocketThreadListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;

public class ClientGUI extends JFrame implements ActionListener, Thread.UncaughtExceptionHandler, SocketThreadListener {


    private static final int WIDTH = 700;
    private static final int HEIGHT = 350;

    private final JTextArea log = new JTextArea();

    private final JPanel panelTop = new JPanel(new GridLayout(2, 3));
    private final JPanel panelBtnInput = new JPanel(new GridLayout(1, 2)); //объединие в одну панель кнопки регистрация и логин Java 3_2
    private final JPanel panelLog = new JPanel(new BorderLayout()); // создание панель для центральной зоны лог и список юзеров Java 3_2
    private final JTextField tfIPAddress = new JTextField("127.0.0.1");
    private final JTextField tfPort = new JTextField("8189");
    private final JCheckBox cbAlwaysOnTop = new JCheckBox("Always on top");
    private final JTextField tfLogin = new JTextField("ivan");
    private final JPasswordField tfPassword = new JPasswordField("123");
    private final JButton btnLogin = new JButton("Login");
    private final JButton btnRegistration = new JButton("Registration"); // добавление кнопки регистрации Java 3_2
    private final JButton btnChangeNick = new JButton("Change nick"); // добавление кнопки изменения ника java 3-2


    private final JPanel panelBottom = new JPanel(new BorderLayout());
    private final JPanel panelBtnDisconnect = new JPanel(new GridLayout(1, 2)); // панель для кнопок дисконект и смены ника java 3-2
    private final JButton btnDisconnect = new JButton("<html><b>Disconnect</b></html>");
    private final JTextField tfMessage = new JTextField();
    private final JButton btnSend = new JButton("Send");

    private final JList<String> userList = new JList<>();
    private boolean shownIoErrors = false;
    private SocketThread socketThread;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss: ");
    private final String WINDOW_TITLE = "Chat";

    private ClientGUI() {
        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setTitle(WINDOW_TITLE);
        setSize(WIDTH, HEIGHT);
        log.setEditable(false);
        log.setLineWrap(true);
        JScrollPane scrollLog = new JScrollPane(log);
        JScrollPane scrollUser = new JScrollPane(userList);
        scrollUser.setPreferredSize(new Dimension(100, 0));
        cbAlwaysOnTop.addActionListener(this);
        btnSend.addActionListener(this);
        tfMessage.addActionListener(this);
        btnLogin.addActionListener(this);
        btnDisconnect.addActionListener(this);
        btnRegistration.addActionListener(this); // обработчик на кнопку регистрации Java 3_2
        btnChangeNick.addActionListener(this); // обработчик на кнопку смены ника Java 3_2
        panelBottom.setVisible(false);

        panelLog.add(scrollLog, BorderLayout.CENTER); // добавление тексовой зоны на отдельную панель Java 3_2
        panelLog.add(scrollUser, BorderLayout.EAST);


        // добавление кнопки регистарции и логин в отдельную панель Java 3_2
        panelBtnInput.add(btnLogin);
        panelBtnInput.add(btnRegistration);

        panelTop.add(tfIPAddress);
        panelTop.add(tfPort);
        panelTop.add(cbAlwaysOnTop);
        panelTop.add(tfLogin);
        panelTop.add(tfPassword);
        panelTop.add(panelBtnInput); // добавлнении панели с кнопками входа на верхнюю панель Java 3_2

        panelBtnDisconnect.add(btnDisconnect);
        panelBtnDisconnect.add(btnChangeNick);
        panelBottom.add(panelBtnDisconnect, BorderLayout.WEST);
        panelBottom.add(tfMessage, BorderLayout.CENTER);
        panelBottom.add(btnSend, BorderLayout.EAST);

        add(panelLog, BorderLayout.CENTER); // размещение панели с тектсовой зоной на окно Java 3_2
        add(panelTop, BorderLayout.NORTH);
        add(panelBottom, BorderLayout.SOUTH);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { // Event Dispatching Thread
                new ClientGUI();
            }
        });
    }

    private void connect() {
        Library.setTypeInput(btnLogin.getText());
        try {
            Socket socket = new Socket(tfIPAddress.getText(), Integer.parseInt(tfPort.getText()));
            socketThread = new SocketThread(this, "Client", socket);
           //socketThread.start(); // так как сокет на стороне клиента запускаю поток не во кострукторе, а отдельно Java 3-4
        } catch (IOException e) {
            showException(Thread.currentThread(), e);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == cbAlwaysOnTop) {
            setAlwaysOnTop(cbAlwaysOnTop.isSelected());
        } else if (src == btnSend || src == tfMessage) {
            sendMessage();
        } else if (src == btnLogin) {
            connect();
        } else if (src == btnDisconnect) {
            socketThread.close();
        } else if (src == btnChangeNick) {
            changeNickUser();
        } else if (src == btnRegistration) { // обработчик события нажатия кнопки Регистрация Java 3_2
            registrationUser();
        } else {
            throw new RuntimeException("Unknown source: " + src);
        }
    }

    /**
     * смена ника пользователя Java 3-2
     */
    private void changeNickUser() {
        String newNickName = JOptionPane.showInputDialog(this, "Введите новый ник");
        if (newNickName == null) {
            JOptionPane.showMessageDialog(this, "Поля нового NickName обязательно для заполнения");
            return;
        }
        String login = tfLogin.getText();
        String password = tfPassword.getText();
        socketThread.sendMessage(Library.getChangeNick(login, password, newNickName));
    }

    /***
     * регистрация нового пользователя Java_3-2
     */
    private void registrationUser() {
        Library.setTypeInput(btnRegistration.getText());
        try {
            Socket socket = new Socket(tfIPAddress.getText(), Integer.parseInt(tfPort.getText()));
            socketThread = new SocketThread(this, "Client", socket);
        } catch (IOException e) {
            showException(Thread.currentThread(), e);
        }
    }



    private void sendMessage() {
        String msg = tfMessage.getText();
        if(Censorship.checkWords(msg)) { // проверка на недопустимые слова по цензуре JAVA 3-3
            putLog("Недопустимые слова");
            tfMessage.setText(null);
            return;
        }
        if ("".equals(msg)) return;
        tfMessage.setText(null);
        tfMessage.requestFocusInWindow();
        socketThread.sendMessage(Library.getTypeBcastClient(msg));
    }

    /**
     * Метод добавления чата в файл Java 3-3
     *
     * @param msg
     * @param username
     */
    private void wrtMsgToLogFile(String msg, String username) throws IOException {
        File file = new File(username + "log.txt");
        file.createNewFile();
        try (FileWriter out = new FileWriter(file, true)) {
            out.write(msg + "\n");
            out.flush();
        } catch (IOException e) {
            if (!shownIoErrors) {
                shownIoErrors = true;
                showException(Thread.currentThread(), e);
            }
        }
    }

    private void putLog(String msg) {
        if ("".equals(msg)) return;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    wrtMsgToLogFile(msg, tfLogin.getText());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log.append(msg + "\n");
                log.setCaretPosition(log.getDocument().getLength());

            }
        });
    }

    /**
     * Метод загрузки послдений 100 строк в чат JAVA 3-3
     */
    private void loadHistoryChat(){
        File file = new File(tfLogin.getText() + "log.txt");
        Path path = Paths.get(tfLogin.getText() + "log.txt");
        int lineCount = 0;
        try {
            lineCount = (int) Files.lines(path).count();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int lineExclude;
        String line;

        if (lineCount > 100) {
            lineExclude = lineCount - 100;
        } else {
            lineExclude = 0;
        }
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            while ((line = in.readLine()) != null) {
                if (lineExclude > 0) {
                    lineExclude--;
                } else {
                    log.append(line + "\n");
                    log.setCaretPosition(log.getDocument().getLength());
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void showException(Thread t, Throwable e) {
        String msg;
        StackTraceElement[] ste = e.getStackTrace();
        if (ste.length == 0)
            msg = "Empty Stacktrace";
        else {
            msg = "Exception in " + t.getName() + " " +
                    e.getClass().getCanonicalName() + ": " +
                    e.getMessage() + "\n\t at " + ste[0];
        }
        JOptionPane.showMessageDialog(null, msg, "Exception", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        showException(t, e);
        System.exit(1);
    }

    /**
     * Socket thread listener methods
     */

    @Override
    public void onSocketStart(SocketThread thread, Socket socket) {
        putLog("Start");
    }

    @Override
    public void onSocketStop(SocketThread thread) {
        panelBottom.setVisible(false);
        panelTop.setVisible(true);
        setTitle(WINDOW_TITLE);
        userList.setListData(new String[0]);
        log.setText(""); // очищение текста в чате при дисконекте Java 3-3

    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        panelBottom.setVisible(true);
        panelTop.setVisible(false);
        String login = tfLogin.getText();
        String password = new String(tfPassword.getPassword());
        thread.sendMessage(Library.getAuthRequest(login, password));
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        handleMessage(msg);
        loadHistoryChat();
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        // showException(thread, exception);
    }

    private void handleMessage(String msg) {
        String[] arr = msg.split(Library.DELIMITER);
        String msgType = arr[0];
        switch (msgType) {
            case Library.AUTH_ACCEPT:
                setTitle(WINDOW_TITLE + " entered with nickname: " + arr[1]);
                break;
            case Library.AUTH_DENIED:
                putLog(msg);
                break;
            case Library.MSG_FORMAT_ERROR:
                putLog(msg);
                socketThread.close();
                break;
            case Library.TYPE_BROADCAST:
                putLog(DATE_FORMAT.format(Long.parseLong(arr[1])) +
                        arr[2] + ": " + arr[3]);
                break;
            case Library.USER_LIST:
                String users = msg.substring(Library.USER_LIST.length() +
                        Library.DELIMITER.length());
                String[] usersArr = users.split(Library.DELIMITER);
                Arrays.sort(usersArr);
                userList.setListData(usersArr);
                break;
            default:
                throw new RuntimeException("Unknown message type: " + msg);
        }
    }
}
