package io.miscellanea.vertx.example;

import io.miscellanea.vertx.example.KeyLoader.KeyType;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JwtRpVerticle extends AbstractVerticle {
  // Fields
  private static final Logger LOGGER = LoggerFactory.getLogger(JwtRpVerticle.class);
  public static final String DEFAULT_TIMEZONE = "Z";
  public static final String DEFAULT_ALGORITHM = "RS256";
  public static final String CLIENT_ID_FORM_FIELD = "client_id";
  public static final String CLIENT_SECRET_FORM_FIELD = "client_secret";
  public static final String CONTENT_TYPE_HEADER = "Content-Type";
  public static final String JWT_WRAPPER_ACCESS_TOKEN = "access_token";
  public static final String JWT_WRAPPER_TOKEN_TYPE = "token_type";
  public static final String JWT_WRAPPER_EXPIRES_IN = "expires_in";
  public static final String MIME_TYPE_JSON = "application/json";

  private int bindPort;
  private String idpAlgorithm;
  private String keyStorePath;
  private String keyStorePassword;
  private JwtAuthenticator authenticator;

  // Constructors
  public JwtRpVerticle() {}

  // Vert.x life-cycle management
  @Override
  public void start(Promise<Void> startPromise) {
    LOGGER.debug("Starting JWT Issuer verticle.");

    this.configure();

    // Load the signer key. This might take time so this bit must
    // not run on the event loop.
    vertx.executeBlocking(
        this::createJwtAuthenticator,
        asyncResult -> {
          if (asyncResult.succeeded()) {
            // Register HTTP routes
            var router = this.registerRoutes();

            // Configure the endpoint for TLS. This requires that we provide HTTP
            // options that identify the keystore and its password.
            var httpOpts =
                new HttpServerOptions()
                    .setSsl(true)
                    .setKeyStoreOptions(
                        new JksOptions()
                            .setPath(this.keyStorePath)
                            .setPassword(this.keyStorePassword));
            vertx
                .createHttpServer(httpOpts)
                .requestHandler(router)
                .listen(
                    this.bindPort,
                    result -> {
                      if (result.succeeded()) {
                        LOGGER.debug("HTTP server started successfully.");
                        startPromise.complete();
                      } else {
                        LOGGER.error(
                            "Unable to start HTTP server. Reason: {}", result.cause().getMessage());
                        startPromise.fail(result.cause());
                      }
                    });
          } else {
            // Abort verticle initialization.
            startPromise.fail(asyncResult.cause());
          }
        });
  }

  // Verticle initialization
  private void createJwtAuthenticator(Promise<Object> promise) {
    try {
      String publicKey = new KeyLoader(KeyType.Public, config()).loadKey();

      LOGGER.debug("Initialized authenticator.");
      var jwtAuth =
          JWTAuth.create(
              vertx,
              new JWTAuthOptions()
                  .addPubSecKey(
                      new PubSecKeyOptions()
                          .setAlgorithm(this.idpAlgorithm)
                          .setPublicKey(publicKey)));

      this.authenticator = new JwtAuthenticator(jwtAuth);
      LOGGER.debug("Authenticator successfully initialized.");

      promise.complete();
    } catch (Exception e) {
      LOGGER.error("Unable to initialize authenticator.", e);
      promise.fail(e);
    }
  }

  private Router registerRoutes() {
    // Create and initialize the router. This object directs web
    // requests to specific handlers based on URL pattern matching.
    var router = Router.router(vertx);

    // Add a body handler to all routes. If we forget to do this,
    // we won't be able to access the content of any POST methods!
    router.route("/api*").handler(BodyHandler.create());

    // Add the JWT authenticator as the next handler to all routes.
    router.route("/api*").handler(this.authenticator::authenticate);

    // Add handler to echo the user attributes back to the caller.
    router.get("/api/hello").handler(this::sayHello);

    return router;
  }

  private void configure() {
    this.bindPort = config().getInteger(ConfigProp.BIND_PORT);
    this.idpAlgorithm = config().getString(ConfigProp.IDP_ALGORITHM);

    // Initialize TLS configuration
    this.keyStorePath = config().getString(ConfigProp.KEY_STORE);
    if (this.keyStorePath == null) {
      throw new IdpException(
          "Required configuration element '"
              + ConfigProp.KEY_STORE
              + "' is missing; verticle will not deploy.");
    }

    this.keyStorePassword = config().getString(ConfigProp.KEY_STORE_PASSWORD);
    if (this.keyStorePassword == null) {
      throw new IdpException(
          "Required configuration element '"
              + ConfigProp.KEY_STORE_PASSWORD
              + "' is missing; verticle will not deploy.");
    }
  }

  // Path handlers
  private void sayHello(RoutingContext routingContext) {
    LOGGER.debug("Handling request to issue JWT token.");

    // Echo the authenticated principal back as a JSON object.
    routingContext
        .response()
        .setStatusCode(200)
        .putHeader("Content-Type", "application/json")
        .end(routingContext.user().principal().toString());
  }
}
