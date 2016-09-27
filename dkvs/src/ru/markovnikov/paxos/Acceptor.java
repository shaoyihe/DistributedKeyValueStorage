package ru.markovnikov.paxos;

import ru.markovnikov.messages.*;

import java.util.HashMap;

/**
 * Created by nikita on 19.05.16.
 */
public class Acceptor {
    private int id;
    private volatile Ballot ballot;
    private Node machine;
    private HashMap<Integer, ProposalValue> accepted;
    public Acceptor(int id, Node machine) {
        this.id = id;
        this.machine = machine;
        this.ballot = new Ballot(machine.container.lastBallotNum - 1, Node.propertiesParser.ids().get(0));
        this.accepted = new HashMap<>();
    }

    public void receiveMessage(AcceptorMessage message) {
        if (message instanceof PhaseTwoRequest) {
            PhaseTwoRequest temp = (PhaseTwoRequest) message;
            if (temp.getPayload().ballotNum.equals(ballot)) {
                accepted.put(temp.getPayload().slot, temp.getPayload());
                machine.logger.logPaxos("RecieveMessage() in Acceptor " + id, "Acceptor accepted " + ballot.toString());
            }
            machine.sendToNode(message.getSource(), new PhaseTwoResponse(id, ballot, temp.getPayload()));
        } else if (message instanceof PhaseOneRequest) {
            if (ballot.less(message.ballotNum)) {
                ballot = message.ballotNum;
                machine.logger.logPaxos("RecieveMessage() in Acceptor " + id, "Acceptor adopted " + ballot);
            }
            machine.sendToNode(message.getSource(), new PhaseOneResponse(id, message.ballotNum, ballot, accepted.values()));
        } else throw new IllegalStateException("Invalid message");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Ballot getBallot() {
        return ballot;
    }

    public void setBallot(Ballot ballot) {
        this.ballot = ballot;
    }

    public Node getMachine() {
        return machine;
    }

    public void setMachine(Node machine) {
        this.machine = machine;
    }

    public HashMap<Integer, ProposalValue> getAccepted() {
        return accepted;
    }

    public void setAccepted(HashMap<Integer, ProposalValue> accepted) {
        this.accepted = accepted;
    }
}
