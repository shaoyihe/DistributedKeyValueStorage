package ru.markovnikov.messages;

/**
 * Created by nikita on 17.05.16.
 */
public class NodeMessage extends Message {
    public NodeMessage(int fromId) {
        this.fromId = fromId;
    }

    @Override
    public String toString() {
        return "node " + fromId;
    }
}
