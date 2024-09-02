package com.mygdx.server.db;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mygdx.server.entity.Component;

import org.bson.Document;
import org.bson.conversions.Bson;
import static com.mongodb.client.model.Filters.eq;

import java.io.File;
import java.util.ArrayList;

/**
 * Class that deals with mongodb stored data including character game data
 *
 * IMPORTANT: Add server host ip to whitelist to be able to access data
 */
public class MongoDb {
    public static class MongoDBConnection {
        private static final String CONNECTION_STRING = "mongodb+srv://pedroigorpsampaio:11zIGd4PkALvWfhz@cluster0.f5gzh.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
        private static MongoClient mongoGameClient = null;

        public static MongoClient getMongoGameClient() {
            if(mongoGameClient == null) {
                ServerApi serverApi = ServerApi.builder()
                        .version(ServerApiVersion.V1)
                        .build();
                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(CONNECTION_STRING))
                        .serverApi(serverApi)
                        .build();

                mongoGameClient = MongoClients.create(settings);
                // Create a new client and connect to the server
//                try (MongoClient inst = MongoClients.create(settings)) {
//                    mongoGameClient = inst;
//                    try {
//                        // Send a ping to confirm a successful connection
//                        MongoDatabase database = mongoGameClient.getDatabase("RFGameData");
//                        database.runCommand(new Document("ping", 1));
//                        System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
//                    } catch (MongoException e) {
//                        e.printStackTrace();
//                    }
//                }
            }

            return mongoGameClient;
        }
    }

    public static int findCharacterIdByName(String name) {
        MongoClient mClient = MongoDBConnection.getMongoGameClient();
        MongoDatabase database = mClient.getDatabase("RFGameData");
        MongoCollection<Document> charCollection = database.getCollection("characters");

        Document doc = charCollection.find(eq("name", name))
                .first();


        if (doc == null)
            return -1;

        return doc.getInteger("id");
    }

    public static String findCharacterNameById(int id) {
        MongoClient mClient = MongoDBConnection.getMongoGameClient();
        MongoDatabase database = mClient.getDatabase("RFGameData");
        MongoCollection<Document> charCollection = database.getCollection("characters");

        Document doc = charCollection.find(eq("id", id))
                .first();


        if (doc == null)
            return null;

        return doc.getString("name");
    }

    public static ArrayList<Integer> getContactsFromId(int id) {
        MongoClient mClient = MongoDBConnection.getMongoGameClient();
        MongoDatabase database = mClient.getDatabase("RFGameData");
        MongoCollection<Document> charCollection = database.getCollection("characters");

        Document doc = charCollection.find(eq("id", id))
                .first();

        if (doc == null)
            return null;

        return (ArrayList<Integer>) doc.getList("contacts", Integer.class);
    }

    public static Component.Character loadCharacter (Component.Character character) {
        MongoClient mClient = MongoDBConnection.getMongoGameClient();
        MongoDatabase database = mClient.getDatabase("RFGameData");
        MongoCollection<Document> charCollection = database.getCollection("characters");
        Document doc = charCollection.find(eq("id", character.tag.id))
                .first();

        // Prints a message if there are no result documents, or prints the result document as JSON
        if (doc == null) {
            System.out.println("No results found. Saving character in mongodb characters collection");
            saveCharacter(character); // save character if its not in collection yet
            return character;
        }

        /**
         * Character found in mongodb - load stored data
         */
        character.tag.name = doc.getString("name");
        character.role_level = doc.getInteger("role_level");
        character.position.mapId = doc.getInteger("map_id");
        character.position.floor = doc.getInteger("floor");
        character.position.x = doc.getDouble("pos_x").floatValue();
        character.position.y = doc.getDouble("pos_y").floatValue();
        character.attr.maxHealth = doc.getDouble("max_health").floatValue();
        character.attr.health = doc.getDouble("health").floatValue();
        character.attr.speed = doc.getDouble("speed").floatValue();
        character.attr.width = doc.getDouble("width").floatValue();
        character.attr.height = doc.getDouble("height").floatValue();
        character.attr.attackSpeed = doc.getDouble("attack_speed").floatValue();
        character.attr.attack = doc.getDouble("attack").floatValue();
        character.attr.defense = doc.getDouble("defense").floatValue();
        character.contacts = (ArrayList<Integer>) doc.getList("contacts", Integer.class);
        character.ignoreList = (ArrayList<Integer>) doc.getList("ignore_list", Integer.class);

//        character.tag.id = input.readInt();
//        character.role_level = input.readInt();
//        character.position.mapId = input.readInt();
//        character.position.floor = input.readInt();
//        character.position.x = input.readFloat();
//        character.position.y = input.readFloat();
//        character.attr.maxHealth = input.readFloat();
//        character.attr.health = input.readFloat();
//        character.attr.speed = input.readFloat();
//        character.attr.width = input.readFloat();
//        character.attr.height = input.readFloat();
//        character.attr.attackSpeed = input.readFloat();
//        character.attr.attack = input.readFloat();
//        character.attr.defense = input.readFloat();

        return character;
    }

    public static boolean saveCharacter(Component.Character character) {
        MongoClient mClient = MongoDBConnection.getMongoGameClient();
        MongoDatabase database = mClient.getDatabase("RFGameData");
        MongoCollection<Document> charCollection = database.getCollection("characters");

        Bson filter = Filters.eq("id", character.tag.id);

        Bson update =  new Document("$set",
                new Document()
                        .append("id", character.tag.id)
                        .append("name", character.tag.name)
                        .append("role_level", character.role_level)
                        .append("map_id", character.position.mapId)
                        .append("floor", character.position.floor)
                        .append("pos_x", character.position.x)
                        .append("pos_y", character.position.y)
                        .append("max_health", character.attr.maxHealth)
                        .append("health", character.attr.health)
                        .append("speed", character.attr.speed)
                        .append("width", character.attr.width)
                        .append("height", character.attr.height)
                        .append("attack_speed", character.attr.attackSpeed)
                        .append("attack", character.attr.attack)
                        .append("defense", character.attr.defense)
                        .append("contacts", character.contacts)
                        .append("ignore_list", character.ignoreList));
        UpdateOptions options = new UpdateOptions().upsert(true);

        charCollection.updateOne(filter, update, options);

        return true;

        //File file = new File("characters", character.tag.name.toLowerCase());

//        output.writeInt(character.tag.id);
//        output.writeInt(character.role_level);
//        output.writeInt(character.position.mapId);
//        output.writeInt(character.position.floor);
//        output.writeFloat(character.position.x);
//        output.writeFloat(character.position.y);
//        output.writeFloat(character.attr.maxHealth);
//        output.writeFloat(character.attr.health);
//        output.writeFloat(character.attr.speed);
//        output.writeFloat(character.attr.width);
//        output.writeFloat(character.attr.height);
//        output.writeFloat(character.attr.attackSpeed);
//        output.writeFloat(character.attr.attack);
//        output.writeFloat(character.attr.defense);
    }
}
