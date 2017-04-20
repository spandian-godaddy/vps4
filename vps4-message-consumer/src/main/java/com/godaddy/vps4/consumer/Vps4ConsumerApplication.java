package com.godaddy.vps4.consumer;

import ch.qos.logback.classic.Level;
import com.godaddy.vps4.handler.Vps4MessageHandler;
import com.google.inject.Injector;

public class Vps4ConsumerApplication {

    public static void main(String[] args) {

        // Added Ability to set log level for kafka pacakges to debug issues if any. 
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.kafka");
        root.setLevel(Level.INFO);
        
        Injector injector = Vps4ConsumerInjector.newInstance();
        
        // create kafka consumers and start listening to messages on the topic
        Vps4ConsumerManager manager = injector.getInstance(Vps4ConsumerManager.class);
        manager.createConsumerGroup(injector.getInstance(Vps4MessageHandler.class));

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                manager.gracefullyShutDownConsumerGroup(false, 10);
            }
        });
    }

}
