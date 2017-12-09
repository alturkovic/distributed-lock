package com.github.alturkovic.lock.converter;

import com.github.alturkovic.lock.Interval;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "locked.interval=10")
public class IntervalConverterTest {

  @Autowired
  private IntervalConverter intervalConverter;

  @Test
  @Interval(value = "10", unit = TimeUnit.MILLISECONDS)
  public void shouldResolveStatic() throws NoSuchMethodException {
    final long millis = intervalConverter.toMillis(this.getClass().getMethod("shouldResolveStatic").getAnnotation(Interval.class));
    assertThat(millis).isEqualTo(10);
  }

  @Test
  @Interval(value = "${locked.interval}", unit = TimeUnit.MILLISECONDS)
  public void shouldResolveProperty() throws NoSuchMethodException {
    final long millis = intervalConverter.toMillis(this.getClass().getMethod("shouldResolveProperty").getAnnotation(Interval.class));
    assertThat(millis).isEqualTo(10);
  }

  @SpringBootApplication
  @ComponentScan("com.github.alturkovic.lock.converter")
  static class TestApp {}

  @Bean
  StandardBeanExpressionResolver resolver() {
    return new StandardBeanExpressionResolver();
  }
}