package ru.markovnikov.messages;

/**
 * Created by nikita on 17.05.16.
 */
public class GetRequest extends ClientRequest {
    private String key;

    public GetRequest(int fromId, String key) {
        this.fromId = fromId;
        this.key = key;
    }

    @Override
    public String toString() {
        return "get " + fromId + ", " + key;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GetRequest && this.toString().equals(o.toString());
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
