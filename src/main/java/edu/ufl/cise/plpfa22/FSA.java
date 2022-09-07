package edu.ufl.cise.plpfa22;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class FSA {
    private final FSANode start;

    public FSA(FSANode start) {
        this.start = start;
    }

    public boolean isAccepted(final String test) {
        final int len = test.length();
        final Queue<Tuple<FSANode, Integer>> queue = new LinkedList<>();
        final char[] charArray = test.toCharArray();

        queue.add(new Tuple<>(start, 0));

        while (!queue.isEmpty()) {
            final Tuple<FSANode, Integer> front = queue.poll();
            final FSANode currNode = front.getFirst();
            final int currIndex = front.getSecond();

            if (currIndex >= len) {
                if (currNode.isAccepting()) {
                    return true;
                }
                continue;
            }
            final char currChar = charArray[currIndex];

            for (final FSANode next : currNode.getNextNodes(currChar)) {
                queue.add(new Tuple<>(next, currIndex + 1));
            }
        }

        return false;
    }
}
