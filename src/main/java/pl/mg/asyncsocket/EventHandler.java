package pl.mg.asyncsocket;

interface EventHandler<T extends Message> {

    void onOpen(Connection connection);
    void onMessage(Connection connection, T message);
    void onError(Connection connection, Exception e);
    void onClose(Connection connection);

}
