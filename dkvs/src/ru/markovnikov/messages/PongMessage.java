package ru.markovnikov.messages;

/**
 * Created by nikita on 17.05.16.
 */
public class PongMessage extends Message {
    public PongMessage(int fromId) {
        this.fromId = fromId;
    }
    @Override
    public String toString() {
        return "pong";
    }
}
