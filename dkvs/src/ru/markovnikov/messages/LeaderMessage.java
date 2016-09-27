package ru.markovnikov.messages;

/**
 * Created by nikita on 17.05.16.
 */
public abstract class LeaderMessage extends Message {
    public LeaderMessage(int fromId) {
        this.fromId = fromId;
    }
}
