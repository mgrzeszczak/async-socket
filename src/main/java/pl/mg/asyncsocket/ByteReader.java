package pl.mg.asyncsocket;

public interface ByteReader {

    void read(byte[] buffer, int position, int size) throws InsufficientByteAmountException;

}
