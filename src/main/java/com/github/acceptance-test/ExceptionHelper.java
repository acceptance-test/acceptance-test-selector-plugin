package com.github.kentolsen;

import org.apache.maven.plugin.MojoExecutionException;

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
 * Manages throwing exceptions that have messages containing the plugin prefix.
 *
 * @author Kent Olsen
 */
public class ExceptionHelper {

  public static void throwMojoExecutionException(String message) throws MojoExecutionException {
    throw new MojoExecutionException(AcceptanceTestSelector.ACCEPTANCE_TEST_SELECTOR_PLUGIN_MESSAGE_PREFIX + message);
  }

  public static void throwMojoExecutionException(String message, Exception e) throws MojoExecutionException {
    throw new MojoExecutionException(AcceptanceTestSelector.ACCEPTANCE_TEST_SELECTOR_PLUGIN_MESSAGE_PREFIX + message, e);
  }
}
