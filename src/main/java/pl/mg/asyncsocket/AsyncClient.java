package pl.mg.asyncsocket;

public interface AsyncClient<T extends Message> extends EventHandler<T>,MessageReader<T>{

}
