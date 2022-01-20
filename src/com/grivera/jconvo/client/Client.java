package com.grivera.jconvo.client;

import com.grivera.jconvo.commons.user.User;
import com.grivera.jconvo.commons.user.message.Message;
import com.grivera.jconvo.commons.user.message.MessageIntent;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Client {

    private final User user;
    private final BlockingQueue<Message> messagesToSend = new LinkedBlockingQueue<>();
    private Thread sendThread;
    private Thread receiveThread;
    private Consumer<Message> onReceiveMessage = message -> {};

    /**
     *
     * Opens a new Client Socket in the JConvo API
     *
     * @param address the address of the Server
     * @param port the port of the address
     * @param username the requested username
     * @throws IOException if the server refuses the connection
     */
    public Client(String address, int port, String username) throws IOException {

        this(new Socket(address, port), username);

    }
    public Client(Socket socket, String username) throws IOException {

        this.user = new User(socket, username);

    }

    public void start() {

        this.sendThread = new Thread(() -> {

            while (!Thread.currentThread().isInterrupted()) {

                try {

                    this.sendMessage(this.messagesToSend.poll(10, TimeUnit.MINUTES));

                } catch (InterruptedException e) {

                    e.printStackTrace();
                    this.disconnect();

                }

            }
        });

        this.receiveThread = new Thread(() -> {

            while (!Thread.currentThread().isInterrupted()) {

                this.receiveMessage();

            }

        });

        sendThread.start();
        receiveThread.start();

    }

    public void setOnReceiveMessage(Consumer<Message> onReceiveMessage) {

        this.onReceiveMessage = onReceiveMessage;

    }

    public void requestToSend(String messageContent) {

        this.messagesToSend.add(new Message(this.user.getUsername(), MessageIntent.SEND, messageContent));

    }

    private void sendMessage(Message message) {

        if (message == null) return;

        if (this.user.isConnected()) {

            this.user.sendMessage(message);

        }

    }

    private void receiveMessage() {

            Message message = this.user.receiveMessage();
            this.onReceiveMessage.accept(message);

    }

    public void disconnect() {

        this.sendThread.interrupt();
        this.receiveThread.interrupt();
        this.user.close();

    }

}
