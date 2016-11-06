package pl.mg.asyncsocket;

abstract class InstanceData<T extends Message> {

    private final MessageReader<T> messageReader;
    private final int port;

    InstanceData(MessageReader<T> messageReader, int port) {
        this.messageReader = messageReader;
        this.port = port;
    }

    MessageReader<T> getMessageReader() {
        return messageReader;
    }

    int getPort() {
        return port;
    }
}
