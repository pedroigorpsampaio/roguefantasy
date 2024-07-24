package com.mygdx.game.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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

    // Check if Polygon intersects Rectangle
    public static boolean intersects(Polygon p, Rectangle r) {
        Polygon rPoly = new Polygon(new float[] { 0, 0, r.width, 0, r.width,
                r.height, 0, r.height });
        rPoly.setPosition(r.x, r.y);
        if(p == null || rPoly == null || p.getTransformedVertices() == null || rPoly.getTransformedVertices() == null) return false;
        if (Intersector.overlapConvexPolygons(rPoly, p))
            return true;
        return false;
    }

    // Check if Polygon intersects Circle
    public static boolean intersects(Polygon p, Circle c) {
        float[] vertices = p.getTransformedVertices();
        Vector2 center = new Vector2(c.x, c.y);
        float squareRadius = c.radius * c.radius;
        for (int i = 0; i < vertices.length; i += 2) {
            if (i == 0) {
                if (Intersector.intersectSegmentCircle(new Vector2(
                        vertices[vertices.length - 2],
                        vertices[vertices.length - 1]), new Vector2(
                        vertices[i], vertices[i + 1]), center, squareRadius))
                    return true;
            } else {
                if (Intersector.intersectSegmentCircle(new Vector2(
                        vertices[i - 2], vertices[i - 1]), new Vector2(
                        vertices[i], vertices[i + 1]), center, squareRadius))
                    return true;
            }
        }
        return false;
    }

    //directories within assets that you want a catalog of
//    static final String[] directories = {
//            "assets/sounds/weapons",
//            "assets/sounds/creatures"
//    };
    public static ArrayList<String> createDirCatalog(String dir) {
        String workingDir = System.getProperty("user.dir");
        ArrayList<String> dirFileNames = new ArrayList<>();

        //for (String dir : directories){
        File directory = new File(workingDir + "/" + dir);
//        File outputFile = new File(directory, "catalog.txt");
//        FileUtils.deleteQuietly(outputFile); //delete previous catalog
        File[] files = directory.listFiles();

        for (int i = 0; i < files.length; i++) {
            //FileUtils.write(outputFile, files[i].getName() + (i == files.length - 1 ? "" : "\n"), Charset.forName("UTF-8"), true);
            dirFileNames.add(files[i].getName());
        }
        //}

        return dirFileNames;
    }

    public static ArrayList<String> createDirCatalogAndroid(String dir) {
        FileHandle dirHandle = Gdx.files.internal(dir);
        ArrayList<String> dirFileNames = new ArrayList<>();
        for (FileHandle entry: dirHandle.list()) {
            dirFileNames.add(entry.name());
        }
        return dirFileNames;
    }
}
