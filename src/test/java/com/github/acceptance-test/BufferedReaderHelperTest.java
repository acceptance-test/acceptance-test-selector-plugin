package com.github.kentolsen;

import org.apache.maven.plugin.MojoExecutionException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * Copyright (C) 2015  Kent Olsen
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

/**
 * Tests for the BufferedReaderHelper class.
 *
 * @author Kent Olsen
 */
public class BufferedReaderHelperTest {

  @Mock
  private File file;
  @Mock
  private BufferedReader reader;

  @BeforeMethod
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void construct() {
    Assert.assertNotNull(new BufferedReaderHelper());
  }

  @Test
  public void getBufferedReader_file() throws Exception {
    File file = File.createTempFile("tmp", "txt");
    final BufferedReader bufferedReader = BufferedReaderHelper.getBufferedReader(file);
    Assert.assertNotNull(bufferedReader);
    bufferedReader.close();
    Assert.assertTrue(file.delete());
  }

  @Test (expectedExceptions = MojoExecutionException.class)
  public void getBufferedReader_exceptionCreatingFileReader() throws Exception {
    File file = new File("non-existent");
    BufferedReaderHelper.getBufferedReader(file);
  }

  @Test
  public void getBufferedReader_url() throws Exception {
    File file = File.createTempFile("tmp", "txt");
    URL url = file.toURI().toURL();
    final BufferedReader bufferedReader = BufferedReaderHelper.getBufferedReader(url);
    Assert.assertNotNull(bufferedReader);
    bufferedReader.close();
    Assert.assertTrue(file.delete());
  }

  @Test (expectedExceptions = MojoExecutionException.class)
  public void getBufferedReader_exceptionCreatingUrlReader() throws Exception {
    URL url = new URL("file:///non-existent");
    BufferedReaderHelper.getBufferedReader(url);
  }

  @Test
  public void getBufferedReader_command() throws Exception {
    BufferedReader bufferedReader = BufferedReaderHelper.getBufferedReader("ls", ".");
    Assert.assertNotNull(bufferedReader);
    bufferedReader.close();
  }

  @Test (expectedExceptions = MojoExecutionException.class)
  public void getBufferedReader_exceptionCreatingCommandReader() throws Exception {
    BufferedReaderHelper.getBufferedReader("ls", "non-existent-directory");
  }

  @Test
  public void readFromBuffer() throws Exception {
    when(reader.readLine()).thenReturn("one").thenReturn("two").thenReturn("three").thenReturn(null);
    final String result = BufferedReaderHelper.readFromBuffer(reader);
    Assert.assertEquals(result, "one\ntwo\nthree\n");
  }

  @Test (expectedExceptions = MojoExecutionException.class)
  public void readFromBuffer_errorReading() throws Exception {
    when(reader.readLine()).thenReturn("one").thenReturn("two").thenThrow(new IOException());
    BufferedReaderHelper.readFromBuffer(reader);
  }

  @Test
  public void readFromBuffer_errorClosingReader() throws Exception {
    when(reader.readLine()).thenReturn("one").thenReturn("two").thenReturn("three").thenReturn(null);
    doThrow(new IOException()).when(reader).close();
    final String result = BufferedReaderHelper.readFromBuffer(reader);
    Assert.assertEquals(result, "one\ntwo\nthree\n");
    verify(reader).close();
  }
}
