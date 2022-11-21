package org.klojang.invoke;

import org.junit.Test;
import org.klojang.util.InvokeException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class PrivateTest {

  @Test
  public void wrap00() {
    Throwable exception = new IOException("Something went wrong");
    BeanReader<Person> reader = new BeanReader<>(Person.class);
    Getter getter = reader.getIncludedGetters().get("firstName");
    InvokeException ie = Private.wrap(exception, reader, getter);
    assertEquals(
        "Error while reading BeanReader.firstName: java.io.IOException: Something went wrong",
        ie.getMessage());
  }

}
