package io.openshift.booster;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(Arquillian.class)
public class OpenShiftIT {

  @RouteURL("${app.name}")
  @AwaitRoute
  private URL route;

  @Before
  public void setup() {
    RestAssured.baseURI = route.toString();
  }

  @Test
  public void testThatWeRecover() throws MalformedURLException {
    await().atMost(5, TimeUnit.MINUTES).catchUncaughtExceptions().until(() -> {
      try {
        get("/api/greeting").then().body("content", equalTo("Hello, World!"));
        return true;
      } catch (Exception e) {
        return false;
      }
    });


    // Stop the service
    get("/api/stop").then().statusCode(200);

    AtomicInteger counter = new AtomicInteger();
    long begin = System.currentTimeMillis();
    await().atMost(5, TimeUnit.MINUTES).until(() -> {
      counter.incrementAndGet();
      Response response = get("/api/greeting");
      return response.getStatusCode() == 200;
    });

    // We recovered !
    long end = System.currentTimeMillis();
    System.out.println("Recovering failures in " + (end - begin) + " ms");
    System.out.println("Counter: " + counter.get());

  }

}
