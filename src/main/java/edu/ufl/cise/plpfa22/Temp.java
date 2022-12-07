package edu.ufl.cise.plpfa22;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public class Temp {
    public int a = 30;
    public int x;

    public static void main(String[] args) {
        Temp temp = new Temp();
        temp.run();
    }

    public void run() {
        x = a;
        System.out.println(x);
    }
}

class Test {
    public static void main(String[] args) throws IOException {
        System.out.println(CodeGenUtils.bytecodeToString(Temp.class.getResourceAsStream("Temp.class").readAllBytes()));
//        new ClassReader(Temp.class.getResourceAsStream("Temp.class").readAllBytes())
//                .accept(new TraceClassVisitor(null, new ASMifier(), new PrintWriter(System.out)), 0);
    }
}
