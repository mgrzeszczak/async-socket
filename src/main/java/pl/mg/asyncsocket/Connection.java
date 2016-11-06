package pl.mg.asyncsocket;

public interface Connection {

    void send(Message message);
    void close();
    String getHost();
    int getPort();

}
