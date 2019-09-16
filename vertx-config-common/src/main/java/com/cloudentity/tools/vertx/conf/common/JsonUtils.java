package com.cloudentity.tools.vertx.conf.common;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtils {
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

    /**
     * This method wraps provided json by provided outputPath.
     * Example: for config -> {"x": "y"} and outputPath some.output.path it will create {"some": {"output": {"path": {"x": "y"}}}}
     */
    public static JsonObject wrapJsonConfig(Object json, String outputPath) {
        io.vavr.collection.List<String> steps = io.vavr.collection.List.of(outputPath.split("\\."));
        JsonObject root = new JsonObject();
        wrap(steps, root, json);
        return root;
    }

    private static JsonObject wrap(io.vavr.collection.List<String> steps, JsonObject entry, Object json) {
        String key = steps.head();
        if (steps.tail().isEmpty()) return entry.put(key, json);
        else {
            JsonObject nextLevel = new JsonObject();
            entry.put(key, nextLevel);
            return wrap(steps.tail(), nextLevel, json);
        }
    }
}
