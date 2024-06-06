package com.mygdx.server.util;

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
}
