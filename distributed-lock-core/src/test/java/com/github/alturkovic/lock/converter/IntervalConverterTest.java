package com.github.alturkovic.lock.converter;

import com.github.alturkovic.lock.Interval;
import org.aspectj.lang.JoinPoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DirtiesContext
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class IntervalConverterTest {

    @Autowired
    private IntervalConverter intervalConverter;

    @Mock
    private JoinPoint joinPoint;

    @Test
    @Interval(value = 10, unit = TimeUnit.MILLISECONDS)
    public void shouldResolveStatic() throws NoSuchMethodException {
        final long millis = intervalConverter.toMillis(this.getClass().getMethod("shouldResolveStatic").getAnnotation(Interval.class), null, null);
        assertThat(millis).isEqualTo(10);
    }

    @Test
    @Interval(property = "example", unit = TimeUnit.MILLISECONDS)
    public void shouldResolveProperty() throws NoSuchMethodException {
        System.setProperty("example", "10");
        final long millis = intervalConverter.toMillis(this.getClass().getMethod("shouldResolveProperty").getAnnotation(Interval.class), null, null);
        assertThat(millis).isEqualTo(10);
    }

    @Test
    @Interval(expression = "#p0", unit = TimeUnit.MILLISECONDS)
    public void shouldResolveExpression() throws NoSuchMethodException {
        when(joinPoint.getArgs()).thenReturn(new Object[]{10});
        when(joinPoint.getTarget()).thenReturn(this);

        final long millis = intervalConverter.toMillis(this.getClass().getMethod("shouldResolveExpression").getAnnotation(Interval.class), "p", joinPoint);
        assertThat(millis).isEqualTo(10);
    }

    @SpringBootApplication
    @ComponentScan("com.github.alturkovic.lock.converter")
    static class TestApp {}
}