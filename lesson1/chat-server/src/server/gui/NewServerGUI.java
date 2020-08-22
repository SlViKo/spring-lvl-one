package server.gui;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.swing.*;

public class NewServerGUI {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { // Event Dispatching Thread
               // ApplicationContext context = new ClassPathXmlApplicationContext("spring-context.xml");
                ApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);
                ServerGUI serverGUI = context.getBean("serverGUI", ServerGUI.class);

            }
        });
    }
}
