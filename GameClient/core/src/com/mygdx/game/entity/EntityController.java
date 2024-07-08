package com.mygdx.game.entity;

import static com.mygdx.game.entity.Entity.rectOffsetLeft;
import static com.mygdx.game.entity.Entity.rectOffsetRight;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Timer;
import com.mygdx.game.network.GameClient;
import com.mygdx.game.ui.GameScreen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class that deals with world entities
 */
public class EntityController {
    private static EntityController instance = null;

    public ConcurrentSkipListMap<Integer, Entity> entities; // list (thread-safe) containing entities to be drawn
    private int lastUid;
    public CopyOnWriteArrayList<Integer> lastAoIEntities;  // list of last entities UiDs received in last state

    public static EntityController getInstance() {
        if(instance == null)
            instance = new EntityController();
        return instance;
    }

    private EntityController() {
        //entities = new ConcurrentSkipListSet<>(new EntityIsoSorter());
        entities = new ConcurrentSkipListMap<>();
        lastAoIEntities = new CopyOnWriteArrayList<>();
        lastUid = 0; // starts unique ids at 0
    }

    float alpha = 1f;

    /**
     * Iterates through visible list of entities calling their render methods
     * @param batch The sprite batch to render the entities. Begin must be called before
     */
    public void renderEntities(SpriteBatch batch) {
        if(GameClient.getInstance().getClientCharacter() == null) return; // client player not loaded

        boolean isPlayerOfuscated = false;
        boolean foundHoverEntity = false;

        // creates a sorted list version of entities and iterates it to draw in correct order
        synchronized (entities) {
            for (Map.Entry<Integer, Entity> entry : entriesSortedByValues(entities)) {
                Entity e = entry.getValue();

                if(GameClient.getInstance().getClientCharacter() == null) break;

                boolean obfuscateHit = false;

                if(e.isObfuscator) {
                    // check if its a entity that can obfuscate player and change transparency accordingly
                    Vector2 drawPos = GameClient.getInstance().getClientCharacter().drawPos;
                    Vector2 drawPosLeft = new Vector2(drawPos.x - rectOffsetLeft * 0.75f, drawPos.y);
                    Vector2 drawPosRight = new Vector2(drawPos.x + rectOffsetRight * 0.75f, drawPos.y);
                    obfuscateHit = e.rayCast(drawPos) || e.rayCast(drawPosLeft) || e.rayCast(drawPosRight);

                    if (obfuscateHit) {
                        Color c = batch.getColor();
                        batch.setColor(c.r, c.g, c.b, alpha); //set alpha interpolated
                        isPlayerOfuscated = true;
                    }
                }

                if(e.contextId != GameClient.getInstance().getClientCharacter().id) { // only considers other entities and not client character
                    if(e.isInteractive && !GameScreen.onStageActor) {// Only acts on world if it did not hit any UI actor
                        boolean mouseHit = e.rayCast(GameScreen.unprojectedMouse);
                        if(mouseHit) { // hover on an entity
                            foundHoverEntity = true;
                            WorldMap.hoverEntity = e;
                        }
                    }
                }

                // if entity is target of client player, render selected target UI
                if(GameClient.getInstance().getClientCharacter().target != null &&
                        GameClient.getInstance().getClientCharacter().target.uId == e.uId)
                    e.renderTargetUI(batch);
                e.render(batch);

                if(obfuscateHit) {
                    Color c = batch.getColor();
                    batch.setColor(c.r, c.g, c.b, 1f);//set alpha back to 1
                    alpha -= 1f * Gdx.graphics.getDeltaTime();
                    if(alpha <= 0.15f)
                        alpha = 0.15f;
                }
            }
        }
        if(!isPlayerOfuscated)
            alpha = 1.0f;
        if(!foundHoverEntity)
            WorldMap.hoverEntity = null;
    }

    /**
     * Given current client target, searches for next one based on entity proximity
     * *This method ignores client entity as you cannot target yourself
     * @param currentTarget the current target of client
     * @param reverse   if it should search in reverse
     * @return  the next entity found
     */
    public Entity getNextTargetEntity(Entity currentTarget, boolean reverse) {
        boolean surpassedCurrTarget = false;
        Entity lastEntity = null;
        synchronized (entities) {
            SortedSet<Map.Entry<Integer, Entity>> sortedEntities = entitiesByClientProximity(entities);
            for (Map.Entry<Integer, Entity> entry : sortedEntities) {
                Entity e = entry.getValue();

                if(e.uId == GameClient.getInstance().getClientUid()) // ignores client entity
                    continue;

                if(e.isTargetAble) {
                    if(!reverse) {
                        if (currentTarget == null || surpassedCurrTarget)
                            return e;
                        else if (e.uId == currentTarget.uId)
                            surpassedCurrTarget = true;
                    } else {
                        if (currentTarget == null) {
                            return e;
                        }

                        if (e.uId == currentTarget.uId) {
                            if(lastEntity != null)
                                return lastEntity;
                        }
                        lastEntity = e;
                    }
                }
            }
            if(!reverse) {
                // interactive entity not found after current target, look before
                for (Map.Entry<Integer, Entity> entry : sortedEntities) {
                    Entity e = entry.getValue();

                    if (e.uId == GameClient.getInstance().getClientUid()) // ignores client entity
                        continue;

                    if (currentTarget == null) {
                        return e;
                    }
                    // if found a interactive entity before return it or
                    if (e.isTargetAble || e.uId == currentTarget.uId) {  // if it has reach again the current target, return it as there are no other targets nearby
                        return e;
                    }
                }
            } else {
                Entity e = null;
                for (Map.Entry<Integer, Entity> entry : sortedEntities) { // gets last entity (in reverse, hasn't found looking backwards until the beginning)
                    if(entry.getValue().isTargetAble) { // get last interactive entity
                        e = entry.getValue();
                    }
                }
                if (currentTarget == null) {
                    return e;
                }
                if (e != null && e.uId != GameClient.getInstance().getClientUid()
                        && e.uId != currentTarget.uId) // if its not client and not current target, we found a new one
                    return e;
            }
        }
        return null; // returns null if no interactive entity was found
    }

    /**
     * Gets the closest interactive entity of clients entity, if any
     * @return  the closest interactive entity from player entity
     */
    public Entity getNextTargetEntity() {
        synchronized (entities) {
            SortedSet<Map.Entry<Integer, Entity>> sortedEntities = entitiesByClientProximity(entities);
            for (Map.Entry<Integer, Entity> entry : sortedEntities) {
                Entity e = entry.getValue();

                if(e.uId == GameClient.getInstance().getClientUid()) // ignores client entity
                    continue;

                boolean selectable;
                if(Gdx.app.getType() == Application.ApplicationType.Desktop) // discard non target-able entities from closest targeting with space
                    selectable = e.isTargetAble;
                else // on android include interactive entities to help interactions with UI interaction with closest entities button
                    selectable = e.isInteractive;

                if(selectable) {
                    return e;
                }
            }
        }
        return null; // returns null if no interactive entity was found
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
                if(entity.uId == GameClient.getInstance().getClientUid()) continue; // no point in removing client entity from drawing list of entities
                if(!lastAoIEntities.contains(entity.uId)) { // remove entity from lists if has not been in last AoI state update
                    if(!entity.fadeOut)
                        entity.fadeOut(5f); // fades out and let it handle the removing when alpha is low enough
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

    static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entitiesByClientProximity(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
                new Comparator<Map.Entry<K,V>>() {
                    @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                        Entity e1Entity = (Entity) e1.getValue();
                        Entity e2Entity = (Entity) e2.getValue();

                        float e1Dist = e1Entity.finalDrawPos.dst(GameClient.getInstance().getClientCharacter().finalDrawPos);
                        float e2Dist = e2Entity.finalDrawPos.dst(GameClient.getInstance().getClientCharacter().finalDrawPos);
                        int res = e1Dist > e2Dist ? 1 : -1;
                        return res != 0 ? res : 1; // Special fix to preserve items with equal values
                    }
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }
}
