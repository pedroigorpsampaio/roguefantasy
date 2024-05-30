package com.mygdx.game.util;

import java.util.ArrayList;

public class Common {
    public static long calculateAverage(ArrayList<Long> list) {
        long sum = 0;
        if(!list.isEmpty()) {
            for (Long val : list) {
                sum += val;
            }
            return sum / list.size();
        }
        return sum;
    }
}
