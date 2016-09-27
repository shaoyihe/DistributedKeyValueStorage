package ru.markovnikov.messages;

import ru.markovnikov.paxos.ProposalValue;

/**
 * Created by nikita on 18.05.16.
 */
public class PhaseTwoRequest extends AcceptorMessage {

    private ProposalValue payload;

    public PhaseTwoRequest(int fromId, ProposalValue payload) {
        super(fromId, payload.ballotNum);
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "p2a " + fromId + " " + payload.toString();
    }

    public ProposalValue getPayload() {
        return payload;
    }

    public void setPayload(ProposalValue payload) {
        this.payload = payload;
    }
}
