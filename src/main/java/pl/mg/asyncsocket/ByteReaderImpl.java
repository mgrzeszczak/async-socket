package pl.mg.asyncsocket;

final class ByteReaderImpl implements ByteReader {

    private ByteBuffer buffer;

    ByteReaderImpl(ByteBuffer buffer){
        this.buffer = buffer;
    }

    @Override
    public void read(byte[] buffer, int position, int size) throws InsufficientByteAmountException{
        this.buffer.read(buffer,position,size);
    }
}
