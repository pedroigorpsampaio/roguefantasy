package com.mygdx.game.util;

import com.badlogic.gdx.Gdx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

/**
 * Generates random names based on the statistical weight of letter sequences
 * in a collection of sample names
 * SOURCE: http://www.siliconcommandergames.com/MarkovNameGenerator.htm
 *
 * @since 0.9
 */
public class NameGenerator
{
    //private members
    private LinkedHashMap<String, List<Character>> _chains = new LinkedHashMap<String, List<Character>>();
    private List<String> _samples = new ArrayList<String>();
    private List<String> _used = new ArrayList<String>();
    private Random _rnd;
    private int _order;
    private int _minLength;

    //constructor
    public NameGenerator(String filename, int order, int minLength)
    {
        ArrayList<String> sampleNames = readNamesFromFile(filename);

        _rnd = new Random();

        //fix parameter values
        if (order < 1)
            order = 1;
        if (minLength < 1)
            minLength = 1;

        _order = order;
        _minLength = minLength;

        //split comma delimited lines
        for (String line : sampleNames)
        {
            String[] tokens = line.split(",");
            for (String word : tokens)
            {
                String upper = word.trim().toUpperCase();
                if (upper.length() < order + 1)
                    continue;
                _samples.add(upper);
            }
        }

        //Build chains
        for (int i = 0; i < _samples.size(); i++)
        {
            String word = _samples.get(i);
            for (int letter = 0; letter < (word.length()) - order; letter++)
            {
                String token = word.substring(letter, letter+order);
                List<Character> entry = null;
                if (_chains.containsKey(token))
                    entry = _chains.get(token);
                else
                {
                    entry = new ArrayList<Character>();
                    _chains.put(token, entry);
                }
                entry.add(word.charAt(letter + order));
            }
        }
    }

    private ArrayList<String> readNamesFromFile(String filename) {
        // the file containing the information to be loaded
        InputStream colFile = Gdx.files.internal(filename).read();
        // buffered reader to read collider file
        BufferedReader colReader = new BufferedReader(new InputStreamReader(colFile));
        // iterates through lines and columns to get collider mask
        String line;
        // array list of name samples
        ArrayList<String> nameSamples = new ArrayList<String>();
        try {
            while ((line = colReader.readLine()) != null) {
                nameSamples.add(line);
            }
        } catch (IOException e) {
            System.err.println("Could not read file: names/" + filename + ".name");
            e.printStackTrace();
        }

        // return samples of names read from file
        return nameSamples;
    }

    //Get the next random name
    public String nextName()
    {
        //get a random token somewhere in middle of sample word
        String s = "";
        do
        {
            int n = _rnd.nextInt(_samples.size());
            int nameLength = _samples.get(n).length();
            int lowerLimit = randInt(0, _samples.get(n).length() - _order);
            s = _samples.get(n).substring(lowerLimit, lowerLimit + _order);
            while (s.length() < nameLength)
            {
                String token = s.substring(s.length() - _order, s.length());
                char c = getLetter(token);
                if (c != '?')
                    s += getLetter(token);
                else
                    break;
            }

            if (s.contains(" "))
            {
                String[] tokens = s.split(" ");
                s = "";
                for (int t = 0; t < tokens.length; t++)
                {
                    if (tokens[t] == "")
                        continue;
                    if (tokens[t].length() == 1)
                        tokens[t] = tokens[t].toUpperCase();
                    else
                        tokens[t] = tokens[t].substring(0, 0) + tokens[t].substring(0).toLowerCase();
                    if (s != "")
                        s += " ";
                    s += tokens[t];
                }
            }
            else
                s = s.substring(0, 0) + s.substring(0).toLowerCase();
        }
        while (_used.contains(s) || s.length() < _minLength);

        if (s.contains(" ")) {
            String[] tokens = s.split(" ");
            s = "";

            for (int t = 0; t < tokens.length; t++)
            {
                s += tokens[t].substring(0, 1).toUpperCase() + tokens[t].substring(1) + " ";
            }
        }
        else
            s = s.substring(0, 1).toUpperCase() + s.substring(1);

        _used.add(s);

        return s;
    }

    //Reset the used names
    public void reset()
    {
        _used.clear();
    }

    //Get a random letter from the chain
    private char getLetter(String token)
    {
        if (!_chains.containsKey(token))
            return '?';
        List<Character> letters = _chains.get(token);
        int n = _rnd.nextInt(letters.size());
        return letters.get(n);
    }

    /**
     * Returns a pseudo-random number between min and max, inclusive.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimum value
     * @param max Maximum value.  Must be greater than min.
     * @return Integer between min and max, inclusive.
     * @see java.util.Random#nextInt(int)
     */
    public int randInt(int min, int max) {

        // NOTE: This will (intentionally) not run as written so that folks
        // copy-pasting have to think about how to initialize their
        // Random instance.  Initialization of the Random instance is outside
        // the main scope of the question, but some decent options are to have
        // a field that is initialized once and then re-used as needed or to
        // use ThreadLocalRandom (if using at least Java 1.7).

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = _rnd.nextInt((max - min) + 1) + min;

        return randomNum;
    }
}
