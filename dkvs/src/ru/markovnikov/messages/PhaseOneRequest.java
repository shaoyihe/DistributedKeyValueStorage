package ru.markovnikov.messages;

import ru.markovnikov.paxos.Ballot;

/**
 * Created by nikita on 18.05.16.
 */
public class PhaseOneRequest extends AcceptorMessage {
    public PhaseOneRequest(int fromId, Ballot ballotNum) {
        super(fromId, ballotNum);
    }

    @Override
    public String toString() {
        return "p1a " + fromId + " " + ballotNum.toString();
    }
}
