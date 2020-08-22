package server.gui;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringConfig {

    @Bean
    public ServerGUI serverGUI() {
        return new ServerGUI();
    }
}
