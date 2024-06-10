package com.mygdx.server.entity;

import com.mygdx.server.network.GameRegister;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results;
import dev.dominion.ecs.api.Scheduler;

public class EntityController {
    private Dominion dominion; // the domain/world containing all entities
    private Scheduler scheduler;
    private static EntityController instance = null;

    public static EntityController getInstance() {
        if(instance == null)
            instance = new EntityController();
        return instance;
    }

    private EntityController() {
        // create ecs world
        dominion  = Dominion.create();

        // spawn one wolf for testing
        Entity wolfPrefab = dominion.createEntity(
                "Herr Wolfgang IV",
                new Component.Tag(0, "Herr Wolfgang IV"),
                new Component.Position(222, 222),
                new Component.Velocity(0, 0),
                new Component.Attributes(60f, 68f, 100f, 50f, 20f),
                new Component.Spawn(0, new Component.Position(222, 222), 15)
        ).setState(Component.AI.State.IDLE);
        wolfPrefab.add(new Component.AI(wolfPrefab));

        // create systems
        scheduler = createSystems();
        scheduler.tickAtFixedRate(GameRegister.clientTickrate); // updates at the same speed client doe

    }

    public void dispose() {
        scheduler.shutDown();
        dominion.close();
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

        // applies velocities to all entities with position and velocity components
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
//                        System.out.printf("Entity %s moved with %s to %s\n",
//                                tag.name, velocity, position);
                    });
        });

        return scheduler;
    }

    // prepares entities data to be sent to clients
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
            Entity target = entity.entity().get(Component.AI.class).target;
            if(target != null)
                creatureUpdate.targetId = entity.entity().get(Component.AI.class).target.get(Component.Character.class).tag.id;
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