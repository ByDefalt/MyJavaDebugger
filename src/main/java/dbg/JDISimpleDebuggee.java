package dbg;

import java.util.ArrayList;
import java.util.List;

public class JDISimpleDebuggee {

    public static class Machin{
        public String truc;
    }

    public static void main(String[] args) {
        String description = "Simple power printer";
        System.out.println(description + " -- starting");
        int x = 40;
        List<Machin> machins = new ArrayList<>();
        machins.add(new Machin());
        x = 12;
         x = x + 5;
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