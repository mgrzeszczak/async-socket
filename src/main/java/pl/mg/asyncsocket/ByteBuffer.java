package pl.mg.asyncsocket;

final class ByteBuffer {

    private byte[] array;
    private byte[] snapshot;

    ByteBuffer(){
        this.array = new byte[0];
    }

    void read(byte[] buffer,int position, int size) throws InsufficientByteAmountException {
        if (array.length<size) throw new InsufficientByteAmountException();
        System.arraycopy(array,0,buffer,position,size);
        byte[] temp = new byte[array.length-size];
        System.arraycopy(array,size,temp,0,array.length-size);
        array = temp;
    }

    void push(byte[] buffer, int position, int size) {
        byte[] temp = new byte[size+array.length];
        System.arraycopy(array,0,temp,0,array.length);
        System.arraycopy(buffer,position,temp,array.length,size);
        array = temp;
    }

    void save(){
        snapshot = new byte[array.length];
        System.arraycopy(array,0,snapshot,0,array.length);
    }

    void restore(){
        array = snapshot;
    }

}
