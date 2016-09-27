package ru.markovnikov.messages;

/**
 * Created by nikita on 17.05.16.
 */
public class DeleteRequest extends ClientRequest {
    private String key;

    public DeleteRequest(int fromId, String key) {
        this.fromId = fromId;
        this.key = key;
    }

    @Override
    public String toString() {
        return "delete " + fromId + " " + key;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GetRequest && this.toString().equals(other.toString());
    }


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
