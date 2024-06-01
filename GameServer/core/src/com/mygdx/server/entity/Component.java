package com.mygdx.server.entity;

import com.mygdx.server.network.GameRegister;


public class Component {
    public static class Character {
        public String name;
        public String token;
        public int id, role_level;
        public float x, y;
        public float speed;

        /**
         * Prepares character data to be sent to clients with
         * only data that is safe to send and it is of clients interest
         * @return  the Register character class containing the data safe to send
         */
        public GameRegister.Character toSendToClient() {
            GameRegister.Character charData = new GameRegister.Character();
            charData.role_level = this.role_level; charData.id = this.id; charData.speed = this.speed;
            charData.x = this.x; charData.y = this.y; charData.name = this.name;
            return charData;
        }
    }
}
