/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.flume.sink.jdbc;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Map;

import org.apache.flume.Event;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class TestParameter {

  private PreparedStatement statement;
  private Event event;
  private Map<String, String> headers;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    statement = mock(PreparedStatement.class);
    event = mock(Event.class);
    headers = mock(Map.class);
    when(event.getHeaders()).thenReturn(headers);
  }

  @Test
  public void testByteArrayBodyParameter() throws Exception {
    final Parameter p = Parameter.newParameter(1, "body", "bytearray", null);
    assertThat(p, notNullValue());
    final byte[] bytes = new byte[] { 1, 2, 3 };
    when(event.getBody()).thenReturn(bytes);
    p.setValue(statement, event);
    verify(statement).setBytes(1, bytes);
  }

  @Test
  public void testDoubleHeaderParameter() throws Exception {
    final Parameter p = Parameter.newParameter(1, "header.foo", "double", null);
    assertThat(p, notNullValue());
    when(headers.get("foo")).thenReturn("123.4");
    p.setValue(statement, event);
    verify(statement).setDouble(1, 123.4);
  }

  @Test
  public void testFloatHeaderParameter() throws Exception {
    final Parameter p = Parameter.newParameter(1, "header.foo", "float", null);
    assertThat(p, notNullValue());
    when(headers.get("foo")).thenReturn("1.2");
    p.setValue(statement, event);
    verify(statement).setFloat(1, 1.2f);
  }

  @Test
  public void testStringBodyParameterNoParameter() throws Exception {
    // Just check to make sure this is a valid config.
    Parameter.newParameter(1, "body", "string", null);
  }

  @Test
  public void testStringBodyParameterBlankParameter() throws Exception {
    // Just check to make sure this is a valid config.
    Parameter.newParameter(1, "body", "string", "");
  }

  @Test
  public void testStringBodyParameterValidString() throws Exception {
    final Parameter p = Parameter.newParameter(1, "body", "string", "UTF-8");
    assertThat(p, notNullValue());
    when(event.getBody()).thenReturn("mystring".getBytes());
    p.setValue(statement, event);
    verify(statement).setString(1, "mystring");
  }

  @Test
  public void testStringBodyParameterEmpty() throws Exception {
    final Parameter p = Parameter.newParameter(1, "body", "string", "UTF-8");
    assertThat(p, notNullValue());
    when(event.getBody()).thenReturn(new byte[] {});
    p.setValue(statement, event);
    verify(statement).setString(1, "");
  }

  @Ignore
  // The String constructor doesn't seem to throw this on invalid UTF-8 strings.
  // Maybe this is a feature?
  @Test(expected = UnsupportedEncodingException.class)
  public void testStringBodyParameterInvalidString() throws Exception {
    final Parameter p = Parameter.newParameter(1, "body", "string", "UTF-8");
    assertThat(p, notNullValue());
    // Invalid UTF-8 sequence from:
    // http://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-test.txt
    when(event.getBody()).thenReturn(
        new byte[] { (byte) 0xfe, (byte) 0xfe, (byte) 0xff, (byte) 0xff });
    p.setValue(statement, event);
  }

  @Test
  public void testStringHeaderParameterNotNull() throws Exception {
    final Parameter p = Parameter.newParameter(1, "header.foo", "string", null);
    assertThat(p, notNullValue());
    when(headers.get("foo")).thenReturn("mystring");
    p.setValue(statement, event);
    verify(statement).setString(1, "mystring");
  }

  @Test
  public void testStringHeaderParameterNull() throws Exception {
    final Parameter p = Parameter.newParameter(1, "header.foo", "string", null);
    assertThat(p, notNullValue());
    when(headers.get("foo")).thenReturn(null);
    p.setValue(statement, event);
    verify(statement).setNull(1, Types.VARCHAR);
  }

  @Test
  public void testLongHeaderParameterNotNull() throws Exception {
    final Parameter p = Parameter.newParameter(1, "header.foo", "long", null);
    assertThat(p, notNullValue());
    when(headers.get("foo")).thenReturn("1234");
    p.setValue(statement, event);
    verify(statement).setLong(1, 1234);
  }

  @Test
  public void testIntHeaderParameter() throws Exception {
    final Parameter p = Parameter.newParameter(1, "header.foo", "int", null);
    assertThat(p, notNullValue());
  }

  @Test
  public void testLongHeaderParameterNull() throws Exception {
    final Parameter p = Parameter.newParameter(1, "header.foo", "long", null);
    assertThat(p, notNullValue());
    when(headers.get("foo")).thenReturn(null);
    p.setValue(statement, event);
    verify(statement).setNull(1, Types.BIGINT);
  }

  @Test(expected = NumberFormatException.class)
  public void testLongHeaderParameterInvalid() throws Exception {
    final Parameter p = Parameter.newParameter(1, "header.foo", "long", null);
    assertThat(p, notNullValue());
    when(headers.get("foo")).thenReturn("notalong");
    p.setValue(statement, event);
  }

  @Test
  public void testDateHeaderNoTimeZone() throws Exception {
    Parameter.newParameter(1, "header.foo", "date", "yyyy-MM-dd HH:mm:ss");
  }

  @Test
  public void testDateHeaderParameter() throws Exception {
    final Parameter p = Parameter.newParameter(1, "header.foo", "date",
        "yyyy-MM-dd HH:mm:ss#GMT");
    assertThat(p, notNullValue());
    when(headers.get("foo")).thenReturn("2013-11-26 06:43:50");
    p.setValue(statement, event);
    final ArgumentCaptor<Timestamp> captor = ArgumentCaptor
        .forClass(Timestamp.class);
    verify(statement).setTimestamp(eq(1), captor.capture());
    assertEquals(1385448230000L, captor.getValue().getTime());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidItem() throws Exception {
    Parameter.newParameter(1, "foo", "long", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidType() throws Exception {
    Parameter.newParameter(1, "body", "foo", null);
  }

  @Test
  public void testCustomParameterCreation() throws Exception {
    final Parameter p = Parameter
        .newParameter(1, "custom",
            "org.apache.flume.sink.jdbc.TestParameter$TestCustomParameter",
            "UTF-8");
    assertThat(p, notNullValue());
    assertTrue(p instanceof TestCustomParameter);
  }

  public static class TestCustomParameter extends CustomParameter {
    public TestCustomParameter(final int id) {
      super(id);
    }

    @Override
    public void setValue(PreparedStatement ps, Event e) throws Exception {
    }
  }

}
