package ru.markovnikov.messages;

/**
 * Created by nikita on 17.05.16.
 */
public class PingMessage extends Message {
    public PingMessage(int fromId) {
        this.fromId = fromId;
    }

    @Override
    public String toString() {
        return "ping from " + fromId;
    }
}
