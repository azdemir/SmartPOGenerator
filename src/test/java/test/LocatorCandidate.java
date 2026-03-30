package test;

public class LocatorCandidate {
    final String strategy;
    final String value;
    final String preferredName;
    final int score;

    LocatorCandidate(String strategy, String value, String preferredName, int score) {
        this.strategy = strategy;
        this.value = value;
        this.preferredName = preferredName;
        this.score = score;
    }
}
