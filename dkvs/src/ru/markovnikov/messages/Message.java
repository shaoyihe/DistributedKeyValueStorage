package ru.markovnikov.messages;

import ru.markovnikov.paxos.Ballot;
import ru.markovnikov.paxos.Command;
import ru.markovnikov.paxos.ProposalValue;

import java.util.Arrays;

/**
 * Created by nikita on 17.05.16.
 */
public abstract class Message {
    protected String text;
    protected int fromId;

    public int getSource() {
        return fromId;
    }

    public static Message parse(int fromId, String[] parts) {
        switch (parts[0]) {
            case "node":
                return new NodeMessage(Integer.parseInt(parts[1]));
            case "ping":
                return new PingMessage(fromId);
            case "pong":
                return new PongMessage(fromId);
            case "decision":
                return new DecisionMessage(Integer.parseInt(parts[1]), Command.parse(Arrays.copyOfRange(parts, 2, parts.length)));
            case "propose":
                return new ProposeMessage(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Command.parse(Arrays.copyOfRange(parts, 3, parts.length)));
            case "p1a":
                return new PhaseOneRequest(Integer.parseInt(parts[1]), Ballot.parse(parts[2]));
            case "p2a":
                return new PhaseTwoRequest(Integer.parseInt(parts[1]), ProposalValue.parse(Arrays.copyOfRange(parts, 2, parts.length)));
            case "p1b":
                return PhaseOneResponse.parse(parts);
            case "p2b":
                return new PhaseTwoResponse(Integer.parseInt(parts[1]), Ballot.parse(parts[2]), ProposalValue.parse(Arrays.copyOfRange(parts, 3, parts.length)));
            default:
                throw new IllegalArgumentException("Unknown message.");
        }
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

}
