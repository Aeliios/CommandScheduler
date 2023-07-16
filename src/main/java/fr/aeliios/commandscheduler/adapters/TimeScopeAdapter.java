package fr.aeliios.commandscheduler.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import fr.aeliios.commandscheduler.data.TimeScope;
import fr.aeliios.commandscheduler.enums.Day;
import fr.aeliios.commandscheduler.enums.Periodicity;

import java.lang.reflect.Type;

public class TimeScopeAdapter implements JsonSerializer<TimeScope>, JsonDeserializer<TimeScope> {

    @Override
    public JsonElement serialize(TimeScope timeScope, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(timeScope.toString());
    }

    @Override
    public TimeScope deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        String[] split = json.getAsString().split(" : ");
        Periodicity periodicity = Periodicity.valueOf(split[0]);
        Day day = split.length == 2 ? Day.valueOf(split[1]) : null;
        return new TimeScope(periodicity, day);
    }
}
