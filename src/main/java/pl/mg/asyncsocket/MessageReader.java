package pl.mg.asyncsocket;

public interface MessageReader<T extends Message> {

    T readMessage(ByteReader reader) throws InsufficientByteAmountException;

}
