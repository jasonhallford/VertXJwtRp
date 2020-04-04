package io.miscellanea.vertx.example;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JwtAuthenticator {
  // Fields
  private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticator.class);

  private JWTAuth authenticator;

  // Constructors
  public JwtAuthenticator(JWTAuth authenticator) {
    assert authenticator != null : "authenticator must not be null.";
    this.authenticator = authenticator;
  }

  // Handler methods
  public void authenticate(RoutingContext routingContext) {
    var authorizeHeader = routingContext.request().getHeader("Authorization");
    LOGGER.debug("Authenticating request with Authorization = {}", authorizeHeader);

    if (authorizeHeader != null) {
      // We can only handle bearer tokens; let's make certain that's what we have.
      String[] components = authorizeHeader.split(" ");
      if (components.length > 1 && components[0].equalsIgnoreCase("bearer")) {
          var jwt = components[1];
          LOGGER.debug("Will authenticate using JWT {}",jwt);

          this.authenticator.authenticate(new JsonObject().put("jwt",jwt), result -> {
              if( result.succeeded()){
                  LOGGER.debug("Successfully authenticated request.");

                  // Add the user to the routing context and pass control to the
                  // next handler.
                  User user = result.result();
                  LOGGER.debug("User pricipal = {}", user.principal().toString());

                  routingContext.setUser(user);
                  routingContext.next();
              } else {
                  // Unable to authenticate; request is forbidden.
                  LOGGER.warn("Unable to authenticate request.",result.cause());
                  routingContext.response().setStatusCode(403).end();
              }
          });
      } else {
        routingContext.response().setStatusCode(400).end();
      }
    } else {
      LOGGER.info("Request does not contain Authorization header; returning HTTP status code 401.");
      routingContext.response().setStatusCode(401).end();
    }
  }
}
