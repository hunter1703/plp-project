package edu.ufl.cise.plpfa22;

import java.util.Arrays;

public class Temp {
    public class Temp2 {
        public class Temp3 {
            public void run() {
                System.out.println(Temp3.class.getNestHost());
                System.out.println(Temp3.class.getEnclosingClass());
                System.out.println(Arrays.asList(Temp3.class.getNestMembers()));

                System.out.println(Temp2.class.getNestHost());
                System.out.println(Arrays.asList(Temp2.class.getNestMembers()));

                System.out.println(Temp.class.getNestHost());
                System.out.println(Arrays.asList(Temp.class.getNestMembers()));
            }
        }

        public class Temp4 {
            public void run() {
                System.out.println(Temp3.class.getNestHost());
                System.out.println(Temp3.class.getEnclosingClass());
                System.out.println(Arrays.asList(Temp3.class.getNestMembers()));

                System.out.println(Temp2.class.getNestHost());
                System.out.println(Arrays.asList(Temp2.class.getNestMembers()));

                System.out.println(Temp.class.getNestHost());
                System.out.println(Arrays.asList(Temp.class.getNestMembers()));
            }
        }

        public void run() {
            final Temp3 temp3 = new Temp3();
            temp3.run();
        }
    }

    public static void main(String[] args) {
        final Temp temp = new Temp();
        temp.run();
    }

    public void run() {
        final Temp2 temp2 = new Temp2();
        temp2.run();
    }
}
