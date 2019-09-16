package examples;

import com.cloudentity.tools.vertx.bus.ServiceClientFactory;
import com.cloudentity.tools.vertx.bus.ServiceVerticle;
import com.cloudentity.tools.vertx.bus.VertxBus;
import com.cloudentity.tools.vertx.bus.VertxEndpoint;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class ServiceVerticleExamples {
  public interface Validator {
    @VertxEndpoint(address = "validator.validate")
    Future<Boolean> validate(String attribute, int maxLenght);
  }

  public class ValidatorVerticle extends ServiceVerticle implements Validator {
    @Override
    public Future<Boolean> validate(String attribute, int maxLenght) {
      return Future.succeededFuture(attribute.length() <= maxLenght);
    }
  }

  public void example1(Vertx vertx) {
    VertxBus.registerPayloadCodec(vertx.eventBus());
    Validator client = ServiceClientFactory.make(vertx.eventBus(), Validator.class);

    vertx.deployVerticle(new ValidatorVerticle(), result -> {
      if (result.succeeded()) {
        client.validate("aaa", 3).setHandler(validateResult -> {
          System.out.println("validate result = " + validateResult.result());
        });
      } else {
        System.out.println("Could not deploy ServiceVerticle");
      }
    });
  }

  public static void main(String[] args) throws InterruptedException {
    new ServiceVerticleExamples().example1(Vertx.vertx());
  }
}
