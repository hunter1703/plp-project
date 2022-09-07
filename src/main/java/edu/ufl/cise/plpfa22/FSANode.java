package edu.ufl.cise.plpfa22;

import java.util.*;

public class FSANode {
    private final Map<Character, List<FSANode>> next;
    private final boolean isAccepting;

    public FSANode(boolean isAccepting) {
        this.next = new HashMap<>();
        this.isAccepting = isAccepting;
    }

    public void addTransition(final char ch, final FSANode nextTransition) {
        next.computeIfAbsent(ch, key -> new LinkedList<>()).add(nextTransition);
    }

    public List<FSANode> getNextNodes(final char ch) {
        if (next.containsKey(ch)) {
            return next.get(ch);
        }
        return Collections.emptyList();
    }

    public boolean isAccepting() {
        return isAccepting;
    }
}
