package client;

public class Censorship {
    static String[] forbiddenWords = {"утка", "курица", "карась"};

    public static boolean checkWords(String msg) {
        Boolean check = false;
        String[] msgWords = msg.split(" ");
        for (int i = 0; i < msgWords.length; i++) {
            for (int j = 0; j < forbiddenWords.length; j++) {
                if (msgWords[i].toLowerCase().equals(forbiddenWords[j])) {
                    check = true;
                    break;
                }
            }
            if (check) break;
        }
        return check;
    }
}
