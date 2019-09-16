package com.cloudentity.tools.vertx.conf.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.cloudentity.tools.vertx.conf.common.JsonUtils.*;

public class YamlUtils {

    private static final Logger log = LoggerFactory.getLogger(YamlUtils.class);

    /**
     * This method converts provided string with yaml format content to json and wraps by provided outputPath
     * @param yaml
     * @param outputPath
     * @return
     */
    public static JsonObject wrapYamlConfig(String yaml, String outputPath) {
        return wrapJsonConfig(yamlToJsonObject(yaml), outputPath);
    }

    private static JsonObject yamlToJsonObject(String yamlString) {
        String jsonString = null;
        try {
            ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
            Object obj = yamlReader.readValue(yamlString, Object.class);
            ObjectMapper jsonWriter = new ObjectMapper();
            jsonString = jsonWriter.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Error while parsing 'yaml': " + yamlString + " to 'json' format: " + e.getMessage());
            throw new IllegalArgumentException("Error while parsing 'yaml': " + yamlString + " to 'json' format");
        }
        return new JsonObject(jsonString);
    }
}
