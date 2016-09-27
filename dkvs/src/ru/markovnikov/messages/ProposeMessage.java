package ru.markovnikov.messages;

import ru.markovnikov.paxos.Command;

/**
 * Created by nikita on 17.05.16.
 */
public class ProposeMessage extends LeaderMessage {
    public int slot;
    private Command request;

    public ProposeMessage(int fromId, int slot, Command command) {
        super(fromId);
        this.slot = slot;
        this.request = command;
    }

    @Override
    public String toString() {
        return "propose " + fromId + " " + slot + " " + request.toString();
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public Command getRequest() {
        return request;
    }

    public void setRequest(Command request) {
        this.request = request;
    }
}
