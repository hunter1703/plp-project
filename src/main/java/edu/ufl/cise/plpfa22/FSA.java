package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.IToken.Kind;

import java.util.*;
import java.util.stream.Collectors;

public class FSA {
    private final FSANode start;
    private Set<FSANode> currNodes = new HashSet<>();
    //will be true if advance() resulted into at least one state transition
    private boolean advanced = false;

    public FSA(FSANode start) {
        this.start = start;
        reset();
    }

    //returns list of types of token recognized
    public Set<String> advance(final char test) {
        advanced = false;
        currNodes = currNodes.stream().flatMap(curr -> curr.getNextNodes(test).stream()).collect(Collectors.toSet());
        advanced = !currNodes.isEmpty();

        currNodes.addAll(getEpsilonReachableNodes(currNodes));
        return currNodes.stream().filter(FSANode::isAccepting).map(FSANode::getKind).collect(Collectors.toSet());
    }

    private static Set<FSANode> getEpsilonReachableNodes(final Set<FSANode> rootNodes) {
        //add nodes with epsilon transitions
        final Queue<FSANode> queue = new LinkedList<>(rootNodes);
        final Set<FSANode> epsilonReachableNodes = new HashSet<>();
        while (!queue.isEmpty()) {
            final FSANode curr = queue.poll();

            if (epsilonReachableNodes.contains(curr)) {
                continue;
            }
            epsilonReachableNodes.add(curr);
            queue.addAll(curr.getNextNodes(null));
        }
        return epsilonReachableNodes;
    }

    public void reset() {
        currNodes.clear();
        currNodes.addAll(getEpsilonReachableNodes(Collections.singleton(start)));
    }

    public boolean advanced() {
        return advanced;
    }
}
