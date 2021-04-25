package util;

import discord4j.discordjson.json.UserData;

public class mentions {
    public static String mentionUserString(UserData userData){
        return "<@" + userData.id() + ">";
    }
}
