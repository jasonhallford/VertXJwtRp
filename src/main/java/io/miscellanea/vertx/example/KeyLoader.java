package io.miscellanea.vertx.example;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A class to load keys and certificates serialized in PEM format.
 *
 * @author Jason Hallford
 */
public class KeyLoader {
  public enum KeyType {
    Public,
    Private
  }

  // Fields
  private static final Logger LOGGER = LoggerFactory.getLogger(KeyLoader.class);
  private static final String PRIVATE_KEY = "private";
  private static final String PUBLIC_KEY = "public";

  private JsonObject config;
  private KeyType ofType;
  private String key;

  // Constructors
  public KeyLoader(KeyType ofType, JsonObject config) {
    assert config != null : "config must not be null!";
    this.config = config;
    this.ofType = ofType;

    LOGGER.debug("Key loader initialized to read {} key.", ofType);
  }

  // Methods

  public String loadKey() {
    LOGGER.debug("Loading key.");

    JsonObject keyConfig = this.initializeKeyConfig();

    String pathToKey = null;
    switch (this.ofType) {
      case Private:
        if (keyConfig.containsKey(PRIVATE_KEY)
            && keyConfig.getString(PRIVATE_KEY) != null) {
          pathToKey = keyConfig.getString(PRIVATE_KEY);
        } else {
          throw new IdpException("Provided configuration does not contain a private key entry.");
        }
        break;
      case Public:
        if (keyConfig.containsKey(PUBLIC_KEY)
            && keyConfig.getString(PUBLIC_KEY) != null) {
          pathToKey = keyConfig.getString(PUBLIC_KEY);
        } else {
          throw new IdpException("Provided configuration does not contain a public key entry.");
        }
        break;
    }

    try {
      List<String> contents;
      if (pathToKey.startsWith("classpath:")) {
        contents = FileUtils.readTextFileFromClasspath(pathToKey.substring("classpath:".length()));
      } else {
        contents = Files.readAllLines(Path.of(pathToKey));
      }
      this.key = FileUtils.formatPemFileForVertx(contents);
    } catch (IOException e) {
      throw new IdpException("Unable to read " + ofType + " key from file " + pathToKey + "!", e);
    }

    LOGGER.debug("Successfully loaded {} key {}", ofType, this.key);

    return this.key;
  }

  // Private methods
  private JsonObject initializeKeyConfig() {
    JsonObject keyConfig = config.getJsonObject(ConfigProp.KEYS);
    if (config.containsKey(ConfigProp.CONFIG_FILE)) {
      var path = Path.of(config.getString(ConfigProp.CONFIG_FILE));
      LOGGER.debug("Reading key configuration from file at '{}'.", path.toString());

      keyConfig = this.readKeyConfigFromFile(path);
    }

    LOGGER.debug("Key config = {}", keyConfig.toString());
    return keyConfig;
  }

  private JsonObject readKeyConfigFromFile(Path path) throws IdpException {
    JsonObject config;

    try {
      List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
      var joined = String.join(" ", lines);

      LOGGER.debug("Key config from file = {}", joined);
      config = (JsonObject) Json.decodeValue(joined);
    } catch (Exception e) {
      throw new IdpException(
          "Unable to read key configuration from file " + path.toString() + ".", e);
    }

    return config;
  }
}
