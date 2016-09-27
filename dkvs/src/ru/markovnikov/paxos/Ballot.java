package ru.markovnikov.paxos;

/**
 * Created by nikita on 07.05.16.
 */
public class Ballot implements Comparable<Ballot> {
    public int ballotNumber;
    public int leaderId;

    public Ballot(int ballotNumber, int leaderId) {
        this.ballotNumber = ballotNumber;
        this.leaderId = leaderId;
    }

    @Override
    public int compareTo(Ballot o) {
        int result = new Integer(this.ballotNumber).compareTo(o.ballotNumber);
        if (result == 0)
            return new Integer(o.leaderId).compareTo(this.leaderId);
        return result;
    }

    public boolean less(Ballot o) {
        return this.compareTo(o) < 0;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Ballot
                && (ballotNumber == ((Ballot) other).ballotNumber)
                && (leaderId == ((Ballot) other).leaderId);
    }

    @Override
    public String toString() {
        return "" + ballotNumber + ";" + leaderId;
    }

    public static Ballot parse(String s) {
        String[] parts = s.split(";");
        return new Ballot(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }



    public void setBallotNumber(int ballotNumber) {
        this.ballotNumber = ballotNumber;
    }

    public void setLeaderId(int leaderId) {
        this.leaderId = leaderId;
    }
}
