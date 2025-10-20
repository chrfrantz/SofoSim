package org.sofosim.util.test;

import org.nzdis.micro.random.MersenneTwister;
import org.sofosim.util.RandomHelper;
import java.util.ArrayList;

public class RandomHelperTest {


    public static void main(String[] args) {
        Supertype s = new Supertype();
        Subtype1 s10 = new Subtype1();
        Subtype2 s20 = new Subtype2();
        Subtype1 s11 = new Subtype1();
        Subtype2 s21 = new Subtype2();
        Subtype1 s12 = new Subtype1();
        Subtype2 s22 = new Subtype2();
        Subtype1 s13 = new Subtype1();
        Subtype2 s23 = new Subtype2();
        Subtype1 s14 = new Subtype1();
        Subtype2 s24 = new Subtype2();
        Subtype1 s15 = new Subtype1();
        Subtype2 s25 = new Subtype2();

        ArrayList list = new ArrayList<>();

        list.add(s10);
        list.add(s11);
        list.add(s21);
        list.add(s21);
        list.add(s22);
        list.add(s13);
        list.add(s14);
        list.add(s23);
        list.add(s24);

        System.out.println("Source elements: " + list);

        RandomHelper.setRNG(new MersenneTwister(2532524L));

        // Should return 5 Subtype2 instances
        ArrayList result = RandomHelper.getRandomElements(5, list, null, Subtype2.class, true, false);

        System.out.println("Result: " + result);

        // Should return null (not enough instances)
        result = RandomHelper.getRandomElements(7, list, null, Subtype2.class, true, false);

        System.out.println("Result: " + result);


    }

}
