package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.ast.Declaration;

import java.util.*;

public class SymbolTable <T> {
    private final Map<String, List<SymbolTableEntry>> table;
    private final LinkedList<String> scopeStack;
    private int currentNestLevel;
    //id needs to be different from currentNestLevel
    /**
     * e.g.
     * f() {
     *     CONST a = 5
     * }
     * g() {
     *     return a + 1
     * }
     *
     * variable "a" is not within the scope of g(), but nesting level of both f() and g() are same.
     * So if nesting level is used as scope id, then symbol table will wrongly be able to find "a" within the scope of g()
     */
    private int id;

    public SymbolTable() {
        table = new HashMap<>();
        scopeStack = new LinkedList<>();
        currentNestLevel = -1;
        id = 0;
    }

    public void enterScope() {
        final String id = Integer.toString(this.id++);
        ++currentNestLevel;
        scopeStack.push(id);
    }

    public void exitScope() {
        scopeStack.pop();
        currentNestLevel--;
    }

    public int insert(final String name, final T data) {
        table.putIfAbsent(name, new ArrayList<>());
        final List<SymbolTableEntry> entries = table.get(name);
        final String scopeId = scopeStack.peek();
        if (entries.stream().anyMatch(e -> e.scopeId.equals(scopeId))) {
            return -1;
        }
        entries.add(new SymbolTableEntry(data, scopeId));
        return currentNestLevel;
    }

    public T find(final String name) {
        for (String scopeId : scopeStack) {
            if (table.containsKey(name)) {
                final SymbolTableEntry found = table.get(name).stream().filter(e -> scopeId.equals(e.scopeId)).findFirst().orElse(null);
                if (found != null) {
                    return found.data;
                }
            }

        }
        return null;
    }

    private class SymbolTableEntry {
        private final T data;
        private final String scopeId;

        private SymbolTableEntry(T data, String scopeId) {
            this.data = data;
            this.scopeId = scopeId;
        }
    }
}
