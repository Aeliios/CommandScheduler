package fr.aeliios.commandscheduler.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import fr.aeliios.commandscheduler.enums.TimeScope;

import java.lang.reflect.Type;

public class TimeScopeAdapter implements JsonSerializer<TimeScope>, JsonDeserializer<TimeScope> {

    @Override
    public JsonElement serialize(TimeScope timeScope, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(timeScope.name());
    }

    @Override
    public TimeScope deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        return TimeScope.valueOf(String.valueOf(json.getAsString()));
    }
}
