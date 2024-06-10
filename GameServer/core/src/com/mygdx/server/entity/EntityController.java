package com.mygdx.server.entity;

import com.mygdx.server.network.GameRegister;

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
        dominion.createEntity(
                "Herr Wolfgang IV",
                new Component.Tag(0, "Herr Wolfgang IV"),
                new Component.Position(222, 222),
                new Component.Velocity(0, 0),
                new Component.Attributes(100f),
                new Component.Spawn(0, new Component.Position(222, 222), 15),
                new Component.StateMachine(),
                new Component.AI()
        ).setState(Component.StateMachine.State.IDLE);

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
            dominion.findEntitiesWith(Component.Spawn.class, Component.AI.class, Component.Attributes.class,
                    Component.Velocity.class, Component.Position.class).stream().forEach(r -> {
                Component.Spawn spawn = r.comp1();
                Component.AI AI = r.comp2();
                Component.Attributes attributes = r.comp3();
                Component.Velocity velocity = r.comp4();
                Component.Position position = r.comp5();

                if(AI.target == null) { // move aimlessly
                    if(position.x <= spawn.position.x && position.y <= spawn.position.y) {
                        velocity.x = 1 * attributes.speed * GameRegister.clientTickrate();
                        velocity.y = 0;
                    }
                    if (position.x >= spawn.position.x + 100 && position.y <= spawn.position.y) {
                        velocity.x = 0;
                        velocity.y = 1 * attributes.speed * GameRegister.clientTickrate();
                    }
                    if(position.x >= spawn.position.x + 100 && position.y >= spawn.position.y + 100) {
                        velocity.x = -1 * attributes.speed * GameRegister.clientTickrate();
                        velocity.y = 0;
                    }
                    if(position.x <= spawn.position.x && position.y >= spawn.position.y + 100){
                        velocity.x = 0;
                        velocity.y = -1 * attributes.speed * GameRegister.clientTickrate();
                    }

                }
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
                        System.out.printf("Entity %s moved with %s to %s\n",
                                tag.name, velocity, position);
                    });
        });

        return scheduler;
    }

    // prepares entities data to be sent to clients
    public ArrayList<GameRegister.UpdateCreature> getCreaturesData() {
        // finds all creature entities
        Results<Results.With3<Component.Attributes, Component.Spawn, Component.StateMachine>> entities =
                dominion.findEntitiesWith(Component.Attributes.class, Component.Spawn.class, Component.StateMachine.class);
        // selects only the visible entities
        // iterator = entities.withState(Visibility.VISIBLE).iterator();
        // iterates through entities found adding them to array
        Iterator<Results.With3<Component.Attributes, Component.Spawn, Component.StateMachine>> iterator = entities.iterator();
        ArrayList<GameRegister.UpdateCreature> creatureData = new ArrayList<>();
        while (iterator.hasNext()) {
            Results.With3<Component.Attributes, Component.Spawn, Component.StateMachine> entity = iterator.next();
            GameRegister.UpdateCreature creatureUpdate = new GameRegister.UpdateCreature();
            creatureUpdate.creatureId = entity.entity().get(Component.Tag.class).id;
            creatureUpdate.name = entity.entity().get(Component.Tag.class).name;
            creatureUpdate.spawnId = entity.entity().get(Component.Spawn.class).id;
            creatureUpdate.x = entity.entity().get(Component.Position.class).x;
            creatureUpdate.y = entity.entity().get(Component.Position.class).y;
            creatureUpdate.speed = entity.entity().get(Component.Attributes.class).speed;
            creatureUpdate.lastVelocityX = entity.entity().get(Component.Position.class).lastVelocityX;
            creatureUpdate.lastVelocityY = entity.entity().get(Component.Position.class).lastVelocityY;
            creatureData.add(creatureUpdate);
        }
        return creatureData;
    }

    // creates an empty character entity to be filled with char information later
    public Entity createCharacter () {
        return dominion.createEntity(
                new Component.Character(),
                new Component.StateMachine()
        ).setState(Component.StateMachine.State.IDLE);
    }

    public void removeEntity (Entity entity) {
        dominion.deleteEntity(entity);
    }
}