package com.mygdx.server.entity;

import static com.mygdx.server.entity.WorldMap.TILES_HEIGHT;
import static com.mygdx.server.entity.WorldMap.TILES_WIDTH;

import com.badlogic.gdx.math.Vector2;
import com.mygdx.server.network.GameRegister;
import com.mygdx.server.ui.RogueFantasyServer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results;
import dev.dominion.ecs.api.Scheduler;

class Tile {
    Map<Integer, Entity> entities; // map of entities of this tile (excluding characters)
    Map<Integer, Component.Character> characters; // map of characters of this tile

    public Tile() {entities = new ConcurrentHashMap<>(); characters = new ConcurrentHashMap<>();}
}

public class EntityController {
    private Dominion dominion; // the domain/world containing all entities
    private Scheduler scheduler;
    private static EntityController instance = null;
    public Tile[][] entityWorldState;

    public static EntityController getInstance() {
        if(instance == null)
            instance = new EntityController();
        return instance;
    }

    private EntityController() {
        // create ecs world
        dominion  = Dominion.create();
        // initializes entities state array
        entityWorldState = new Tile[RogueFantasyServer.world.TILES_HEIGHT][RogueFantasyServer.world.TILES_WIDTH];
        for (int i = 0; i < RogueFantasyServer.world.TILES_HEIGHT; i++) {
            for (int j = 0; j < RogueFantasyServer.world.TILES_WIDTH; j++) {
                entityWorldState[i][j] = new Tile();
            }
        }

        // spawn one wolf for testing
//        Entity wolfPrefab = dominion.createEntity(
//                "Herr Wolfgang IV",
//                new Component.Tag(0, "Herr Wolfgang IV"),
//                new Component.Position(26, 4),
//                new Component.Velocity(0, 0),
//                new Component.Attributes(64f*RogueFantasyServer.world.getUnitScale(), 64f*RogueFantasyServer.world.getUnitScale(),
//                        100f*RogueFantasyServer.world.getUnitScale(), 50f,
//                        14f*RogueFantasyServer.world.getUnitScale()),
//                new Component.Spawn(0, new Component.Position(0, 0), 15)
//        ).setState(Component.AI.State.IDLE);
//        wolfPrefab.add(new Component.AI(wolfPrefab));

        // create systems
        scheduler = createSystems();
        scheduler.tickAtFixedRate(GameRegister.clientTickrate); // updates at the same speed client doe
    }

    public void dispose() {
        scheduler.shutDown();
        dominion.close();
    }

    public Map<Integer, Entity> getEntitiesAtTilePos(int i, int j) {
        synchronized (entityWorldState) {
            return entityWorldState[i][j].entities;
        }
    }

    public Vector2 placeCharacter(Component.Character character) {
        Vector2 tPos = RogueFantasyServer.world.toIsoTileCoordinates(new Vector2(character.position.x, character.position.y));

        // makes sure its within world state bounds
        if(tPos.x < 0) tPos.x = 0;
        if(tPos.y < 0) tPos.y = 0;
        if(tPos.x > TILES_WIDTH) tPos.x = TILES_WIDTH-1;
        if(tPos.y > TILES_HEIGHT) tPos.y = TILES_HEIGHT-1;

        EntityController.getInstance().entityWorldState[(int) tPos.x][(int) tPos.y].characters.putIfAbsent(character.tag.id, character);
        return tPos;
    }

    // creates all game systems
    // all the scheduled systems will be executed in sequence at each tick of the scheduler, in the order in which they were added
    // some systems will fork other subsystems to distribute tasks across multiple worker threads
    private Scheduler createSystems() {
        // creates the scheduler
        Scheduler scheduler = dominion.createScheduler();

        // adds a system to update AI
        scheduler.schedule(() -> {
            // finds AI entities to update
            dominion.findEntitiesWith(Component.AI.class).stream().forEach(r -> {
                Component.AI AI = r.comp();
                AI.act();
            });
        });

        // applies velocities to all entities with position and velocity components (not players)
        scheduler.schedule(() -> {
            //find entities
            dominion.findEntitiesWith(Component.Position.class, Component.Velocity.class, Component.Tag.class)
                    // stream the results
                    .stream().forEach(result -> {
                        Component.Position position = result.comp1();
                        Component.Velocity velocity = result.comp2();
                        Component.Tag tag = result.comp3();
                        position.x += velocity.x;
                        position.y += velocity.y;
                        position.lastVelocityX = velocity.x;
                        position.lastVelocityY = velocity.y;
                        position.updatePositionIn2dArray(result.entity()); // updates position in 2d entities state array
//                        System.out.printf("Entity %s moved with %s to %s\n",
//                                tag.name, velocity, position);
                    });
        });

        return scheduler;
    }

    /**
     * Get specific creature data and prepare it to be sent to the client
     * @param entity    the creature entity (this method is only for creatures)
     * @return the update message containing all the info necessary for the client regarding this creature
     **/
    public GameRegister.UpdateCreature getCreatureData(Entity entity) {
        GameRegister.UpdateCreature creatureUpdate = new GameRegister.UpdateCreature();
        creatureUpdate.creatureId = entity.get(Component.Tag.class).id;
        creatureUpdate.name = entity.get(Component.Tag.class).name;
        creatureUpdate.spawnId = entity.get(Component.Spawn.class).id;
        creatureUpdate.x = entity.get(Component.Position.class).x;
        creatureUpdate.y = entity.get(Component.Position.class).y;
        creatureUpdate.speed = entity.get(Component.Attributes.class).speed;
        creatureUpdate.attackSpeed = entity.get(Component.Attributes.class).attackSpeed;
        creatureUpdate.range = entity.get(Component.Attributes.class).range;
        creatureUpdate.lastVelocityX = entity.get(Component.Position.class).lastVelocityX;
        creatureUpdate.lastVelocityY = entity.get(Component.Position.class).lastVelocityY;
        creatureUpdate.state = entity.get(Component.AI.class).state.getName();
        creatureUpdate.timestamp = Instant.now().toEpochMilli();
        Component.Character target = entity.get(Component.AI.class).target;
        if (target != null)
            creatureUpdate.targetId = entity.get(Component.AI.class).target.tag.id;
        else
            creatureUpdate.targetId = -1;

        return creatureUpdate;
    }

    // prepares all creatures data to be sent to clients (should be avoided)
    public ArrayList<GameRegister.UpdateCreature> getCreaturesData() {
        // finds all creature entities
        Results<Results.With3<Component.Attributes, Component.Spawn, Component.AI>> entities =
                dominion.findEntitiesWith(Component.Attributes.class, Component.Spawn.class, Component.AI.class);
        // selects only the visible entities
        // iterator = entities.withState(Visibility.VISIBLE).iterator();
        // iterates through entities found adding them to array
        Iterator<Results.With3<Component.Attributes, Component.Spawn, Component.AI>> iterator = entities.iterator();
        ArrayList<GameRegister.UpdateCreature> creatureData = new ArrayList<>();
        while (iterator.hasNext()) {
            Results.With3<Component.Attributes, Component.Spawn, Component.AI> entity = iterator.next();
            GameRegister.UpdateCreature creatureUpdate = new GameRegister.UpdateCreature();
            creatureUpdate.creatureId = entity.entity().get(Component.Tag.class).id;
            creatureUpdate.name = entity.entity().get(Component.Tag.class).name;
            creatureUpdate.spawnId = entity.entity().get(Component.Spawn.class).id;
            creatureUpdate.x = entity.entity().get(Component.Position.class).x;
            creatureUpdate.y = entity.entity().get(Component.Position.class).y;
            creatureUpdate.speed = entity.entity().get(Component.Attributes.class).speed;
            creatureUpdate.attackSpeed = entity.entity().get(Component.Attributes.class).attackSpeed;
            creatureUpdate.range = entity.entity().get(Component.Attributes.class).range;
            creatureUpdate.lastVelocityX = entity.entity().get(Component.Position.class).lastVelocityX;
            creatureUpdate.lastVelocityY = entity.entity().get(Component.Position.class).lastVelocityY;
            creatureUpdate.state = entity.entity().get(Component.AI.class).state.getName();
            creatureUpdate.timestamp = Instant.now().toEpochMilli();
            Component.Character target = entity.entity().get(Component.AI.class).target;
            if(target != null)
                creatureUpdate.targetId = entity.entity().get(Component.AI.class).target.tag.id;
            else
                creatureUpdate.targetId = -1;
            creatureData.add(creatureUpdate);
        }
        return creatureData;
    }

    // creates an empty character entity to be filled with char information later
    public Entity createCharacter () {
        return dominion.createEntity(
                new Component.Character()
        );
    }

    // dispatch event to all AI event reactors
    public void dispatchEventToAll (Component.Event event) {
        // finds AI entities to dispatch event
        dominion.findEntitiesWith(Component.AI.class).stream().forEach(r -> {
            Component.AI AI = r.comp();
            AI.react(event);
        });
    }

    public void removeEntity (Entity entity) {
        dominion.deleteEntity(entity);
    }

}