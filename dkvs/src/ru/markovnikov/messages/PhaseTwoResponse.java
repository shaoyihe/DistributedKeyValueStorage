package ru.markovnikov.messages;

import ru.markovnikov.paxos.Ballot;
import ru.markovnikov.paxos.ProposalValue;

/**
 * Created by nikita on 19.05.16.
 */
public class PhaseTwoResponse extends LeaderMessage {
    private Ballot ballot;
    private ProposalValue proposalValue;

    public PhaseTwoResponse(int fromId, Ballot ballot, ProposalValue proposalValue) {
        super(fromId);
        this.ballot = ballot;
        this.proposalValue = proposalValue;
    }

    @Override
    public String toString() {
        return "p2b " + fromId + " " + ballot.toString() + " " + proposalValue.toString();
    }

    public Ballot getBallot() {
        return ballot;
    }

    public void setBallot(Ballot ballot) {
        this.ballot = ballot;
    }

    public ProposalValue getProposalValue() {
        return proposalValue;
    }

    public void setProposalValue(ProposalValue proposalValue) {
        this.proposalValue = proposalValue;
    }
}
