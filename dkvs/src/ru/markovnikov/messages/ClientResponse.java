package ru.markovnikov.messages;

/**
 * Created by nikita on 19.05.16.
 */
public class ClientResponse extends ReplicaMessage {
    private String data;

    public ClientResponse(int fromId, String data) {
        this.fromId = fromId;
        this.data = data;
    }

    @Override
    public String toString() {
        return this.data;
    }
}
