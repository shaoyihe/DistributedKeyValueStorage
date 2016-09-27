package ru.markovnikov.messages;

/**
 * Created by nikita on 17.05.16.
 */
public class SetRequest extends ClientRequest {
    private String key;
    private String value;

    public SetRequest(int fromId, String key, String value) {
        this.fromId = fromId;
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "set " + fromId + " " + key + " " + value;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SetRequest && this.toString().equals(o.toString());
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
