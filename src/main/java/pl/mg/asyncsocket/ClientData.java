package pl.mg.asyncsocket;

final class ClientData<T extends Message> extends InstanceData<T> {

    private final EventHandler<T> eventHandler;
    private final String host;

    ClientData(MessageReader<T> messageReader, int port, EventHandler<T> eventHandler, String host) {
        super(messageReader, port);
        this.eventHandler = eventHandler;
        this.host = host;
    }

    EventHandler<T> getEventHandler() {
        return eventHandler;
    }

    String getHost() {
        return host;
    }
}
