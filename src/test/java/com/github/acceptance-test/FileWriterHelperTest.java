package com.github.kentolsen;

import org.apache.maven.plugin.MojoExecutionException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

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
 * Tests for the FileWriterHelper class.
 *
 * @author Kent Olsen
 */
public class FileWriterHelperTest {

  @Mock
  private File file;
  @Mock
  private FileWriter writer;

  @BeforeMethod
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void construct() {
    Assert.assertNotNull(new FileWriterHelper());
  }

  @Test
  public void getFileWriter() throws Exception {
    File file = File.createTempFile("tmp", "txt");
    final FileWriter fileWriter = FileWriterHelper.getFileWriter(file);
    Assert.assertNotNull(fileWriter);
    fileWriter.close();
    Assert.assertTrue(file.delete());
  }

  @Test (expectedExceptions = MojoExecutionException.class)
  public void getFileWriter_exceptionCreatingWriter() throws Exception {
    File file = File.createTempFile("tmp", "txt");
    Assert.assertTrue(file.setReadOnly());
    //noinspection CaughtExceptionImmediatelyRethrown
    try {
      FileWriterHelper.getFileWriter(file);
    }
    catch (MojoExecutionException e) {
      throw e;
    }
    finally {
      Assert.assertTrue(file.delete());
    }
  }

  @Test
  public void writeToWriter() throws Exception {
    final String contents = "contents";
    FileWriterHelper.writeToWriter(writer, contents);
    verify(writer).write(eq(contents));
  }

  @Test
  public void writeToWriter_errorClosing() throws Exception {
    final String contents = "contents";
    doThrow(new IOException()).when(writer).close();
    FileWriterHelper.writeToWriter(writer, contents);
    verify(writer).write(eq(contents));
    verify(writer).close();
  }

  @Test (expectedExceptions = MojoExecutionException.class)
  public void writeToWriter_errorWriting() throws Exception {
    doThrow(new IOException()).when(writer).write(anyString());
    FileWriterHelper.writeToWriter(writer, "contents");
  }
}
