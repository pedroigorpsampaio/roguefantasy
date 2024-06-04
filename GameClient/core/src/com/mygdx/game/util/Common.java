package com.mygdx.game.util;

import java.util.ArrayList;

public class Common {

    public final static long MB = 1024 * 1024;
    public final static Runtime runtime = Runtime.getRuntime();
    static public boolean clientPrediction = true; // apply client prediction
    static public boolean serverReconciliation = true; // apply server reconciliation
    static public boolean entityInterpolation = true; // apply entity interpolation
    static public boolean lagCompensation = false; // apply lag compensation

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

    public static long getRamUsage() {
        return (runtime.totalMemory() - runtime.freeMemory()) / MB;
    }
}
