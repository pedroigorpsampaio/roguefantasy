package com.mygdx.game.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.network.GameClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Class that deals with world entities
 */
public class EntityController {
    private static EntityController instance = null;

    public ConcurrentSkipListMap<Integer, Entity> entities; // list (thread-safe) containing entities to be drawn
    private int lastUid;
    public ArrayList<Integer> lastAoIEntities;  // list of last entities UiDs received in last state

    public static EntityController getInstance() {
        if(instance == null)
            instance = new EntityController();
        return instance;
    }

    private EntityController() {
        //entities = new ConcurrentSkipListSet<>(new EntityIsoSorter());
        entities = new ConcurrentSkipListMap<>();
        lastAoIEntities = new ArrayList<>();
        lastUid = 0; // starts unique ids at 0
    }

    /**
     * Iterates through visible list of entities calling their render methods
     * @param batch The sprite batch to render the entities. Begin must be called before
     */
    public void renderEntities(SpriteBatch batch) {
        // creates a sorted list version of entities and iterates it to draw in correct order
        synchronized (entities) {
            for (Map.Entry<Integer, Entity> entry : entriesSortedByValues(entities)) {
                //System.out.println(entry.getKey()+":"+entry.getValue());
                Entity e = entry.getValue();
//                if (WorldMap.isWithinPlayerAOI(e.drawPos) ||
//                        e.uId == GameClient.getInstance().getClientUid()) // render if entity is within player AoI or if its client render anyway
                    e.render(batch);
//                else {
//                    entities.remove(e.uId); // if not visible, remove from ordered list of visible entities
//                    GameClient.getInstance().removeEntity(e.uId); // remove from list of entities
//                    //System.out.println(e.uId+":"+e.entityName);
//                }
            }
        }
        //System.out.println("\n\n");
//        // creating an iterator
//        Iterator<Map.Entry<Integer, Entity>> it = entities.entrySet().iterator();
//
//        while (it.hasNext()) {
//            Entity e = it.next().getValue();
//            if(isInScreen(e.drawPos)) // render if entity is visible in screen
//                e.render(batch);
//            else
//                it.remove(); // if not visible, remove from ordered list of visible entities
//        }
    }

    /**
     * Searches for entity by its unique entity ID to remove from list of drawn entities
     * @param id    the unique entity id attributed to each entity on its creation
     */
    public void removeEntity(int id) {
//        Iterator<Entity> it = entities.iterator();
//        System.out.println(entities.size() + " before");
//        while (it.hasNext()) {
//            Entity e = it.next();
//            if(e.uId == id) {
//                System.out.println(entities.remove(e));
//
//                return;
//            }
//        }
        entities.remove(id);
    }

    // generates unique ids for entities to be drawn in world
    public int generateUid() {
        int uid = 0;
        synchronized (entities) {
            uid = entities.size();
            while(entities.containsKey(uid)) {
                uid--;
            }
        }
        //System.out.println("key: " + uid);
        return uid;
    }

    // based on last state received remove entities out of this player AoI
    public void removeOutOfAoIEntities() {
        synchronized (entities) {
            Iterator<Map.Entry<Integer, Entity>> itr = entities.entrySet().iterator();
            while (itr.hasNext()) {
                Entity entity = itr.next().getValue();
                if(entity.uId == GameClient.getInstance().getClientUid()) continue; // no point in removing client from drawing list of entities
                if(!lastAoIEntities.contains(entity.uId)) { // remove entity from lists if has not been in last AoI state update
                    itr.remove(); // remove from list of entities to draw
                    GameClient.getInstance().removeEntity(entity.uId); // remove from data list of entities
                }
            }
        }
    }

    /**
     * Sorter that sorts based on isometric world
     */
    class EntityIsoSorter implements Comparator<Entity> {

        @Override
        public int compare(Entity e1, Entity e2) {
            Vector3 e1Iso = WorldMap.translateScreenToIso(e1.drawPos);
            Vector3 e2Iso = WorldMap.translateScreenToIso(e2.drawPos);
            float e1Depth = e1Iso.x + e1Iso.y;
            float e2Depth = e2Iso.x + e2Iso.y;
            if(e1Depth <= e2Depth)
                return -1;
            else
                return 1;
            //return 0;
        }
    }

    static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
                new Comparator<Map.Entry<K,V>>() {
                    @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                        int res = e1.getValue().compareTo(e2.getValue());
                        return res != 0 ? res : 1; // Special fix to preserve items with equal values
                    }
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }
}
