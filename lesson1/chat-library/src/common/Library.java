package common;

public class Library {
    /*
    /auth_request±login±password
    /auth_accept±nickname
    /auth_error
    /broadcast±msg
    /msg_format_error±msg
    /user_list±user1±user2±user3±....
    * */
    public static final String DELIMITER = "±";
    public static final String AUTH_REQUEST = "/auth_request";
    public static final String AUTH_ACCEPT = "/auth_accept";
    public static final String CHANGE_NICK = "/change_nick"; // добавление нового типа запроса смены ника
    public static final String AUTH_DENIED = "/auth_denied";
    public static final String MSG_FORMAT_ERROR = "/msg_format_error";
    // если мы вдруг не поняли, что за сообщение и не смогли разобрать
    public static final String TYPE_BROADCAST = "/bcast";
    // то есть сообщение, которое будет посылаться всем
    public static final String TYPE_BCAST_CLIENT = "/client_msg";
    public static final String USER_LIST = "/user_list";
    public static String typeInput; // тип режима входа Java 3_2

    public static void setTypeInput(String typeInput) {
        Library.typeInput = typeInput;
    }

    public static String getTypeBcastClient(String msg) {
        return TYPE_BCAST_CLIENT + DELIMITER + msg;
    }

    public static String getUserList(String users) {
        return USER_LIST + DELIMITER + users;
    }

    public static String getAuthRequest(String login, String password) {
        return AUTH_REQUEST + DELIMITER + typeInput + DELIMITER + login + DELIMITER + password; // добавил тип режима входа Java 3-2
    }

    public static String getAuthAccept(String nickname) {
        return AUTH_ACCEPT + DELIMITER + nickname;
    }

    public static String getAuthDenied() {
        return AUTH_DENIED;
    }

    public static String getMsgFormatError(String message) {
        return MSG_FORMAT_ERROR + DELIMITER + message;
    }

    public static String getTypeBroadcast(String src, String message) {
        return TYPE_BROADCAST + DELIMITER + System.currentTimeMillis() +
                DELIMITER + src + DELIMITER + message;
    }

    /**
     * метод смены ника Java 3-2
     * @param login
     * @param password
     * @param newNickName
     * @return
     */
    public static String getChangeNick(String login, String password, String newNickName) {
        return CHANGE_NICK + DELIMITER + login + DELIMITER + password + DELIMITER + newNickName;
    }
}
