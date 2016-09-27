package ru.markovnikov.messages;

import ru.markovnikov.paxos.Ballot;

/**
 * Created by nikita on 18.05.16.
 */
public class AcceptorMessage extends Message {

    public Ballot ballotNum;

    public AcceptorMessage(int fromId, Ballot ballotNum) {
        this.ballotNum = ballotNum;
        this.fromId = fromId;
    }
}
