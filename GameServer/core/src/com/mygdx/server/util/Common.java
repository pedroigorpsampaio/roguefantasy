package com.mygdx.server.util;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.server.ui.RogueFantasyServer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;

public class Common {

    // Get list of songs present in music directory and return it as a String list
    public static ArrayList<String> getListOfSongs() {
        URI uri = null;
        ArrayList<String> songs = new ArrayList<>();
        try {
            uri = RogueFantasyServer.class.getResource("/music").toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        Path myPath;
        FileSystem fileSystem = null;
        if (uri.getScheme().equals("jar")) {
            fileSystem = null;
            try {
                fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            myPath = fileSystem.getPath("/resources");
            try {
                fileSystem.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            myPath = Paths.get(uri);
        }
        Stream<Path> walk = null;
        try {
            walk = Files.walk(myPath, 1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Iterator<Path> it = walk.iterator(); it.hasNext(); ) {
            Path file = it.next().getFileName();
            if (file.toString().endsWith(".mp3")) {
                songs.add(String.valueOf(file));
            }

        }
        walk.close();

        return songs;
    }

    // Check if Polygon intersects Rectangle
    public static boolean intersects(Polygon p, Rectangle r) {
        Polygon rPoly = new Polygon(new float[] { 0, 0, r.width, 0, r.width,
                r.height, 0, r.height });
        rPoly.setPosition(r.x, r.y);
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
}
