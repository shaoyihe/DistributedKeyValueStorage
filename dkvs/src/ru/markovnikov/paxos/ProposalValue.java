package ru.markovnikov.paxos;

import java.util.Arrays;

/**
 * Created by nikita on 18.05.16.
 */
public class ProposalValue {
    public Ballot ballotNum;
    public int slot;
    public Command command;

    public ProposalValue(Ballot ballotNum, int slot, Command command) {
        this.ballotNum = ballotNum;
        this.slot = slot;
        this.command = command;
    }


    public void setBallotNum(Ballot ballotNum) {
        this.ballotNum = ballotNum;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    @Override
    public String toString() {
        return "" + ballotNum.toString() + " " + slot + " " + command.toString();
    }

    public static ProposalValue parse(String[] parts) {
        return new ProposalValue(Ballot.parse(parts[0]), Integer.parseInt(parts[1]), Command.parse(Arrays.copyOfRange(parts, 2, parts.length)));
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ProposalValue && this.toString().equals(o.toString());
    }
}
