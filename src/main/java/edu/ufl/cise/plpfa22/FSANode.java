package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.IToken.Kind;

import java.util.*;

public class FSANode {
    private final Map<Character, List<FSANode>> next;
    private final boolean isAccepting;
    private final Kind kind;

    public FSANode(boolean isAccepting, Kind kind) {
        this.next = new HashMap<>();
        this.isAccepting = isAccepting;
        this.kind = kind;
    }

    public void addTransition(final Character ch, final FSANode nextTransition) {
        next.computeIfAbsent(ch, key -> new LinkedList<>()).add(nextTransition);
    }

    public List<FSANode> getNextNodes(final Character ch) {
        if (next.containsKey(ch)) {
            return next.get(ch);
        }
        return Collections.emptyList();
    }

    public boolean isAccepting() {
        return isAccepting;
    }

    public Kind getKind() {
        return kind;
    }
}
