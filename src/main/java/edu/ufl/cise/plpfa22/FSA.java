package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.IToken.Kind;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class FSA {
    private final FSANode start;
    private List<FSANode> currNodes = new LinkedList<>();
    //will be true if advance() resulted into at least one state transition
    private boolean advanced = false;

    public FSA(FSANode start) {
        this.start = start;
        reset();
    }

    //returns list of types of token recognized
    public List<Kind> advance(final char test) {
        advanced = false;
        currNodes = currNodes.stream().flatMap(curr -> curr.getNextNodes(test).stream()).collect(Collectors.toList());
        advanced = !currNodes.isEmpty();
        return currNodes.stream().filter(FSANode::isAccepting).map(FSANode::getKind).collect(Collectors.toList());
    }

    public void reset() {
        currNodes.clear();
        currNodes.add(start);
    }

    public boolean advanced() {
        return advanced;
    }
}
