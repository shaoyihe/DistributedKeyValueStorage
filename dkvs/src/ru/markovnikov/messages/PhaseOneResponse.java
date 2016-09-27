package ru.markovnikov.messages;

import ru.markovnikov.paxos.Ballot;
import ru.markovnikov.paxos.ProposalValue;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nikita on 18.05.16.
 */
public class PhaseOneResponse extends LeaderMessage {
    private Ballot originalBallot;
    private Ballot ballotNum;
    private Collection<ProposalValue> proposalValues;

    public PhaseOneResponse(int fromId, Ballot originalBallot, Ballot ballotNum, Collection<ProposalValue> proposalValues) {
        super(fromId);
        this.originalBallot = originalBallot;
        this.ballotNum = ballotNum;
        this.proposalValues = proposalValues;
    }

    @Override
    public String toString() {
        return "p1b " + fromId + " " + originalBallot.toString() + " " + ballotNum.toString() + " " +
                proposalValues.stream().map(ProposalValue::toString).collect(Collectors.joining("_#_"));
    }

    public static PhaseOneResponse parse(String[] parts) {
        if (!parts[0].equals("p1b"))
            throw new IllegalArgumentException("PhaseOneResponse must starts with \"p1b\"");
        int fromId = Integer.parseInt(parts[1]);
        Ballot originalBallot = Ballot.parse(parts[2]);
        Ballot ballotNum = Ballot.parse(parts[3]);
        String[] ss = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(parts, 4, parts.length))).stream().collect(Collectors.joining(" ")).split("_#_");
        List<ProposalValue> proposalValueList = new ArrayList<>(Arrays.asList(ss)).stream()
                .filter(x -> x.length() > 0)
                .map(x -> ProposalValue.parse(x.split(" ")))
                .collect(Collectors.toList());
        return new PhaseOneResponse(fromId, originalBallot, ballotNum, new LinkedHashSet<>(proposalValueList));

    }

    public Ballot getOriginalBallot() {
        return originalBallot;
    }

    public void setOriginalBallot(Ballot originalBallot) {
        this.originalBallot = originalBallot;
    }

    public Ballot getBallotNum() {
        return ballotNum;
    }

    public void setBallotNum(Ballot ballotNum) {
        this.ballotNum = ballotNum;
    }

    public Collection<ProposalValue> getProposalValues() {
        return proposalValues;
    }

    public void setProposalValues(Collection<ProposalValue> proposalValues) {
        this.proposalValues = proposalValues;
    }
}
