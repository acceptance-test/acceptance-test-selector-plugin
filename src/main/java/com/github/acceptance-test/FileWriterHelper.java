package com.github.kentolsen;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
 * Writes to files with a FileWriter.
 *
 * @author Kent Olsen
 */
public class FileWriterHelper {

  public static FileWriter getFileWriter(File suiteFileName) throws MojoExecutionException {
    FileWriter writer = null;
    try {
      writer = new FileWriter(suiteFileName);
    }
    catch (IOException e) {
      ExceptionHelper.throwMojoExecutionException("Error opening file writer", e);
    }
    return writer;
  }

  public static void writeToWriter(FileWriter writer, String suiteContents) throws MojoExecutionException {
    try {
      writer.write(suiteContents);
    }
    catch (IOException e) {
      ExceptionHelper.throwMojoExecutionException("Error writing to file", e);
    }
    finally {
      try {
        writer.close();
      }
      catch (IOException e) {
        // ignore
      }
    }
  }
}
