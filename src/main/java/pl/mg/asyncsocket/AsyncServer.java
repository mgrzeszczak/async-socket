package pl.mg.asyncsocket;

public interface AsyncServer<T extends Message> extends ServerEventHandler<T>,MessageReader<T> {

}
