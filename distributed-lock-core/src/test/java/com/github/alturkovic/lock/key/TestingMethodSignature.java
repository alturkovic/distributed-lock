/*
 * MIT License
 *
 * Copyright (c) 2018 Alen Turkovic
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.alturkovic.lock.key;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.stream.Stream;
import lombok.Data;
import org.aspectj.lang.reflect.MethodSignature;

@Data
public class TestingMethodSignature implements MethodSignature {
  private final Method method;

  @Override
  public Class getReturnType() {
    return method.getReturnType();
  }

  @Override
  public Method getMethod() {
    return method;
  }

  @Override
  public Class[] getParameterTypes() {
    return method.getParameterTypes();
  }

  @Override
  public String[] getParameterNames() {
    return Stream.of(method.getParameters()).map(Parameter::getName).toArray(String[]::new);
  }

  @Override
  public Class[] getExceptionTypes() {
    return method.getExceptionTypes();
  }

  @Override
  public String toShortString() {
    return method.toString();
  }

  @Override
  public String toLongString() {
    return method.toString();
  }

  @Override
  public String getName() {
    return method.toString();
  }

  @Override
  public int getModifiers() {
    return method.getModifiers();
  }

  @Override
  public Class getDeclaringType() {
    return method.getDeclaringClass();
  }

  @Override
  public String getDeclaringTypeName() {
    return method.getDeclaringClass().getTypeName();
  }
}
