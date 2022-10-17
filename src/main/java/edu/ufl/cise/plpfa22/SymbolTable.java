package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.ast.Declaration;

import java.util.*;

import static java.util.Collections.emptyList;

public class SymbolTable {
    private final Map<String, List<SymbolTableEntry>> table;
    private final LinkedList<String> scopeStack;
    private int currentNestLevel;

    public SymbolTable() {
        table = new HashMap<>();
        scopeStack = new LinkedList<>();
        currentNestLevel = -1;
    }

    public void enterScope() {
        final String id = Integer.toString(++currentNestLevel);
        scopeStack.push(id);
    }

    public void exitScope() {
        scopeStack.pop();
        currentNestLevel--;
    }

    public int insert(final String name, final Declaration declaration) {
        table.putIfAbsent(name, new ArrayList<>());
        final List<SymbolTableEntry> entries = table.get(name);
        final String scopeId = scopeStack.peek();
        if (entries.stream().anyMatch(e -> e.scopeId.equals(scopeId))) {
            return -1;
        }
        entries.add(new SymbolTableEntry(currentNestLevel, declaration, scopeId));
        return currentNestLevel;
    }

    public Declaration find(final String name) {
        for (String scopeId : scopeStack) {
            if (table.containsKey(name)) {
                final SymbolTableEntry found = table.get(name).stream().filter(e -> scopeId.equals(e.scopeId)).findFirst().orElse(null);
                if (found != null) {
                    return found.declaration;
                }
            }

        }
        return null;
    }

    private static class SymbolTableEntry {
        private final int nestingLevel;
        private final Declaration declaration;
        private final String scopeId;

        private SymbolTableEntry(int nestingLevel, Declaration declaration, String scopeId) {
            this.nestingLevel = nestingLevel;
            this.declaration = declaration;
            this.scopeId = scopeId;
        }
    }
}
