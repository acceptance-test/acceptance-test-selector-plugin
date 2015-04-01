package com.github.kentolsen;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

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
 * Opens and reads files, urls and process results.
 *
 * @author Kent Olsen
 */
public class BufferedReaderHelper {

  public static BufferedReader getBufferedReader(File suiteFileName) throws MojoExecutionException {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(suiteFileName));
    }
    catch (FileNotFoundException e) {
      ExceptionHelper.throwMojoExecutionException("Error reading suite", e);
    }
    return reader;
  }

  public static BufferedReader getBufferedReader(URL shaUrl) throws MojoExecutionException {
    BufferedReader reader = null;
    try {
      URLConnection urlConnection = shaUrl.openConnection();
      reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
    }
    catch (Exception e) {
      ExceptionHelper.throwMojoExecutionException("Error opening reader for shaUrl", e);
    }
    return reader;
  }

  public static BufferedReader getBufferedReader(String shellCommand, String workingDirectory) throws MojoExecutionException {
    BufferedReader reader = null;
    try {
      Process process = Runtime.getRuntime().exec(shellCommand, null, new File(workingDirectory));
      reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    }
    catch (Exception e) {
      ExceptionHelper.throwMojoExecutionException("Error opening reader for shell command", e);
    }
    return reader;
  }

  public static String readFromBuffer(BufferedReader reader) throws MojoExecutionException {
    StringBuilder stringBuilder = new StringBuilder();
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line);
        stringBuilder.append(AcceptanceTestSelector.LINE_SEPARATOR);
      }
    }
    catch (IOException e) {
      ExceptionHelper.throwMojoExecutionException("Error reading from buffer", e);
    }
    finally {
      try {
        reader.close();
      }
      catch (IOException e) {
        //ignore
      }
    }
    return stringBuilder.toString();
  }
}
