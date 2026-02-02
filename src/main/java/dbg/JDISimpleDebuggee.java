package dbg;

import java.util.ArrayList;
import java.util.List;

public class JDISimpleDebuggee {

    public class Machin{
        public String truc;
    }

    public static void main(String[] args) {
        String description = "Simple power printer";
        System.out.println(description + " -- starting");
        int x = 40;
        List<Machin> machins = new ArrayList<>();
        machins.add(new JDISimpleDebuggee().new Machin());
        int power = 2;
        printPower(x, power);
    }

    public static double power(int x, int power) {
        double powerX = Math.pow(x, power);
        return powerX;
    }

    public static void printPower(int x, int power) {
        double powerX = power(x, power);
        System.out.println(powerX);
    }
}