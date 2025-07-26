package com.thizthizzydizzy.midilogio.connection;
import com.thizthizzydizzy.midilogio.MidiLogIO;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.BiFunction;
public class ConnectionManager{
    private static HashMap<String, BiFunction<UUID, String, Connection>> registeredConnectionTypes = new HashMap<>();
    static{
        register("HALF", HalfConnection::new);
        register("FULL", FullConnection::new);
    }
    public static void register(String type, BiFunction<UUID, String, Connection> builder){
        if(registeredConnectionTypes.containsKey(type))throw new IllegalArgumentException("Cannot register connection type "+type+"! (This type is already registered)");
        registeredConnectionTypes.put(type, builder);
    }
    public static Connection create(MidiLogIO io, UUID uuid, String definition){
        String[] definitionParts = definition.split(" ", 3);
        int speed = Integer.parseInt(definitionParts[0]);
        String type = definitionParts[1];
        String settings = definitionParts[2];

        var builder = registeredConnectionTypes.get(type);
        if(builder==null)throw new IllegalArgumentException("Unknown connection type: "+type);
        var conn = builder.apply(uuid, settings);
        conn.speed = speed;
        conn.io = io;
        return conn;
    }
}
