package com.github.kentolsen;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.*;

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
 * Maven plugin to select the set of acceptance tests to run based on git repository
 * changes since the last successful build.  It queries the Jenkins host
 * using <code>shaUrl</code> to get the git SHA used for the latest successful
 * build.  It then queries git at <code>sourceRoot</code> to get the list of
 * files that have changed since the last successful build.  It then uses
 * <code>suiteMappings</code>, a map containing source path to acceptance test
 * class mappings, to determine which acceptance tests to run. It stores the
 * list of acceptance tests to run as classes in the testng suite file
 * named <code>suiteFileName</code>.  As it writes the testng suite file it removes
 * any <packages>...</packages> section and adds a <classes>...</classes> section
 * with the classes to run.
 *
 * @author Kent Olsen
 */
@Mojo(name = "generateSuite", defaultPhase = LifecyclePhase.TEST_COMPILE)
public class AcceptanceTestSelector extends AbstractMojo {

  /**
   * Prefix that the plugin applies to all log messages.
   */
  public static final String ACCEPTANCE_TEST_SELECTOR_PLUGIN_MESSAGE_PREFIX = "acceptance-test-selector-plugin: ";
  /**
   * The system-defined line separator.
   */
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");
  /**
   * Suite mapping key to use for a fallback test when no other matching tests are found.
   */
  public static final String FALLBACK_CODE_PATH = "_fallback_code_path_";

  public static final String LAST_SUCCESSFUL_REVISION = "LAST_SUCCESSFUL_REVISION";
  private static final String SHA_TAG = "<SHA1>";
  private static final String PACKAGE_TAG_REGEX = "<packages>(?s).*</packages>";
  private static final String SIMPLE_SUITE_CONTENTS = "<suite name=\"Acceptance Test Suite\">\n"
    + "<test name=\"Selected Acceptance\" preserve-order=\"false\">\n"
    + "</test>\n"
    + "</suite>\n";
  private static final String TEST_END_TAG = "</test>";
  private static final String TEST_CLASS_SEPARATOR = ";";

  /**
   * The name of the acceptance test suite file that will be used by
   * testng to run the acceptance tests.  This is the full file name path.
   */
  @Parameter
  private File suiteFileName;

  public void setSuiteFileName(File suiteFileName) {
    this.suiteFileName = suiteFileName;
  }

  /**
   * The Jenkins url to query to get the git SHA of the last successful build.
   * It is expected that the result will be in the form of a SHA
   * (as in adbf23f466db4d9c1b7e0652103ff9bce422c97a), or a tag containing a
   * SHA (as in <SHA1>adbf23f466db4d9c1b7e0652103ff9bce422c97a</SHA1>).
   */
  @Parameter
  private URL shaUrl;

  public void setShaUrl(URL shaUrl) {
    this.shaUrl = shaUrl;
  }

  /**
   * The root of the git repository containing the source code being tested.
   */
  @Parameter
  private String sourceRoot;

  public void setSourceRoot(String sourceRoot) {
    this.sourceRoot = sourceRoot;
  }

  /**
   * Mappings from source code to corresponding acceptance tests.  For example:
   *  <suiteMappings>
   *    <property>
   *      <name>com/example/samplepackage</name>
   *      <value>com.example.acceptanceTestSuites.SampleSuite</value>
   *    </property>
   *    <property>
   *      <name>com/example/samplepackage2</name>
   *      <value>com.example.acceptanceTestSuites.SampleSuite2;com.example.acceptanceTestSuites.SampleSuite3</value>
   *    </property>
   *  </suiteMappings>
   *
   * In the example, any code changed in com/example/samplepackage will cause tests to be
   * run in the com.example.acceptanceTestSuites.SampleSuite class.  It is
   * expected that <name>...</name> will be the path to the source starting from the repository root.
   * It is expected that <value>...</value> will be the full class name of the testng class.  If multiple
   * test classes should run based on a source path, they should be separated by a semi-colon <code>;</code>
   * as in the 2nd property listed above.
   */
  @SuppressWarnings ( "MismatchedQueryAndUpdateOfCollection" )
  @Parameter
  private Properties suiteMappings;

  public void setSuiteMappings(Properties suiteMappings) {
    this.suiteMappings = suiteMappings;
  }

  /**
   * The name of the package file that is used to determine which integration suites to run.  This file name
	 * should contain one line for each separate component that contains the version of that component that
	 * is being used/tested in the product.  As different versions of a separate component are used, the line
	 * for that component should change to get the matching version.  This is a common approach used in
	 * JavaScript projects with a package.json file.
   */
  @Parameter
  private String packageFileName;

  public void setPackageFileName(String packageFileName) {
    this.packageFileName = packageFileName;
  }

  /**
   * Mappings from source code to corresponding acceptance tests that integrate a new
   * version of a separate component based on <code>packageFileName</code>.  For example:
   *  <suiteMappings>
   *    <property>
   *      <name>separate-component</name>
   *      <value>com.example.acceptanceTestSuites.SeparateComponentSuite</value>
   *    </property>
   *    <property>
   *      <name>separate-component2</name>
   *      <value>com.example.acceptanceTestSuites.SeparateComponent2Suite;com.example.acceptanceTestSuites.SeparateComponent2Suite2</value>
   *    </property>
   *  </suiteMappings>
   *
   * In the example, a change to a component version in <code>packageFileName</code> for separate-component will cause tests to be
   * run in the com.example.acceptanceTestSuites.SeparateComponentSuite class.  It is
   * expected that <name>...</name> will be the path to the source starting from the repository root.
   * It is expected that <value>...</value> will be the full class name of the testng class.  If multiple
   * test classes should run based on a source path, they should be separated by a semi-colon <code>;</code>
   * as in the 2nd property listed above.
   */
  @SuppressWarnings ( "MismatchedQueryAndUpdateOfCollection" )
  @Parameter
  private Properties integrationSuiteMappings;

  public void setIntegrationSuiteMappings(Properties integrationSuiteMappings) {
    this.integrationSuiteMappings = integrationSuiteMappings;
  }

  /**
   * Whether to skip the selection process.  If the process is skipped, the suiteFileName will not
   * be altered.  This basically bypasses the functionality of the plugin.  It is useful for  scenarios
   * where it is desired to run all tests rather than filtering them.
   */
  @Parameter
  private boolean skipFilter;

  public void setSkipFilter(boolean skipFilter) {
    this.skipFilter = skipFilter;
  }

  /**
   * Run the maven plugin.
   *
   * @throws MojoExecutionException
   */
  public void execute() throws MojoExecutionException {
    String sha = getShaFromEnvironment();
    if (skipFilter) {
      logInfoMessage("Bypassing acceptance test selection.");
      return;
    }
    if (suiteFileName == null) {
      ExceptionHelper.throwMojoExecutionException("suiteFileName MUST be set");
    }
    if (shaUrl == null && (sha == null || sha.isEmpty())) {
      ExceptionHelper.throwMojoExecutionException("shaUrl or LAST_SUCCESSFUL_REVISION environment variable/property must be set");
    }
    if (StringUtils.isEmpty(sourceRoot)) {
      ExceptionHelper.throwMojoExecutionException("sourceRoot MUST be set");
    }
    if (suiteMappings == null || suiteMappings.isEmpty()) {
      ExceptionHelper.throwMojoExecutionException("suiteMappings MUST be set");
    }
    if (StringUtils.isEmpty(packageFileName)) {
      ExceptionHelper.throwMojoExecutionException("packageFileName MUST be set");
    }
    if (integrationSuiteMappings == null || integrationSuiteMappings.isEmpty()) {
      ExceptionHelper.throwMojoExecutionException("integrationSuiteMappings MUST be set");
    }

    String suiteContents = suiteFileName.exists()
      ? getSuiteFileContents(getSuiteFileNameReader())
      : getSimpleSuiteContents();
    logInfoMessage("Running " + suiteFileName);

    if (sha == null || sha.isEmpty()) {
      sha = readSha(getShaUrlReader());
    }
    logInfoMessage("SHA is " + sha);

    List<String> changedFiles = getNamesOfChangedFiles(getGitCommandReader("git diff --name-only " + sha + " HEAD"));
    logInfoMessage("Changed file list begin:");
    for (String changedFile : changedFiles) {
      logInfoMessage("Changed file item: " + changedFile);
    }
    logInfoMessage("Changed file list end.");

    logInfoMessage("Package file name is " + packageFileName);

    String changedPins = BufferedReaderHelper.readFromBuffer(getGitCommandReader("git diff " + sha + " HEAD " + packageFileName + " | grep \"^\\+ \""));
    logInfoMessage("Changed Pins Begin:");
    logInfoMessage(changedPins);
    logInfoMessage("Changed Pins End.");

    Set<String> suites = determineSuitesToRun(changedFiles, changedPins);
    logInfoMessage("Suites to run begin:");
    for (String suite : suites) {
      logInfoMessage("Suite to run item: " + suite);
    }
    logInfoMessage("Suites to run end.");

    String classes = generateSuiteClassesSection(suites);
//    logInfoMessage("Classes generated from suites to run:\n" + classes);

    suiteContents = setClassesInSuiteContents(suiteContents, classes);

    FileWriterHelper.writeToWriter(getSuiteFileWriter(), suiteContents);
  }

  private String getShaFromEnvironment() {
    String sha = System.getProperty(LAST_SUCCESSFUL_REVISION);
    if (sha == null || sha.isEmpty()) {
      sha = System.getenv(LAST_SUCCESSFUL_REVISION);
    }
    return sha;
  }

  protected BufferedReader getSuiteFileNameReader() throws MojoExecutionException {
    return BufferedReaderHelper.getBufferedReader(suiteFileName);
  }

  protected BufferedReader getShaUrlReader() throws MojoExecutionException {
    return BufferedReaderHelper.getBufferedReader(shaUrl);
  }

  protected BufferedReader getGitCommandReader(String gitCommand) throws MojoExecutionException {
    return BufferedReaderHelper.getBufferedReader(gitCommand, sourceRoot);
  }

  protected FileWriter getSuiteFileWriter() throws MojoExecutionException {
    return FileWriterHelper.getFileWriter(suiteFileName);
  }

  private void logInfoMessage(String message) {
    getLog().info(ACCEPTANCE_TEST_SELECTOR_PLUGIN_MESSAGE_PREFIX + message);
  }

  private String setClassesInSuiteContents(String suiteContents, String classes) {
    if (suiteContents.contains(TEST_END_TAG)) {
      return suiteContents.replaceFirst(TEST_END_TAG, classes + "\n" + TEST_END_TAG);
    }
    else {
      return suiteContents;
    }
  }

  private String generateSuiteClassesSection(Set<String> suites) {
    StringBuilder classesSection = new StringBuilder("    <classes>\n");
    for (String suite : suites) {
      classesSection.append("      <class name=\"");
      classesSection.append(suite);
      classesSection.append("\"/>\n");
    }
    classesSection.append("    </classes>\n");

    return classesSection.toString();
  }

  private Set<String> determineSuitesToRun(List<String> changedFiles, String changedPins) {
    Set<String> suiteNames = new HashSet<String>();

    Set<String> suiteKeys = suiteMappings.stringPropertyNames();
    if (changedFiles != null) {
      for (String changedFile : changedFiles) {
        for (String suiteKey : suiteKeys) {
          if (changedFile.startsWith(suiteKey)) {
            if (!StringUtils.isEmpty(suiteMappings.getProperty(suiteKey))) {
              Collections.addAll(suiteNames, suiteMappings.getProperty(suiteKey).split(TEST_CLASS_SEPARATOR));
            }
            break;
          }
        }
      }
    }
    if (!StringUtils.isEmpty(changedPins)) {
      Set<String> integrationSuiteKeys = integrationSuiteMappings.stringPropertyNames();
      for (String integrationSuiteKey : integrationSuiteKeys) {
        if (changedPins.contains(integrationSuiteKey)) {
          if (!StringUtils.isEmpty(integrationSuiteMappings.getProperty(integrationSuiteKey))) {
            Collections.addAll(suiteNames, integrationSuiteMappings.getProperty(integrationSuiteKey).split(TEST_CLASS_SEPARATOR));
          }
        }
      }
    }
    if (suiteNames.isEmpty() && suiteKeys.contains(FALLBACK_CODE_PATH)) {
      suiteNames.add(suiteMappings.getProperty(FALLBACK_CODE_PATH));
    }
    return suiteNames;
  }

  private List<String> getNamesOfChangedFiles(BufferedReader reader) throws MojoExecutionException {
    String shellCommandResponse = BufferedReaderHelper.readFromBuffer(reader);

    List<String> fileNames = new ArrayList<String>();
    Collections.addAll(fileNames, shellCommandResponse.split(LINE_SEPARATOR));
    return fileNames;
  }

  private String readSha(BufferedReader reader) throws MojoExecutionException {
    String sha = BufferedReaderHelper.readFromBuffer(reader);

    if (StringUtils.isEmpty(sha)) {
      ExceptionHelper.throwMojoExecutionException("SHA not found in response from " + shaUrl);
    }
    if (sha.startsWith(SHA_TAG)) {
      sha = sha.substring(SHA_TAG.length(), sha.length() - SHA_TAG.length() - 1 - LINE_SEPARATOR.length());
    }
    else {
      sha = sha.substring(0, sha.length() - LINE_SEPARATOR.length());
    }
    return sha;
  }

  private String getSuiteFileContents(BufferedReader reader) throws MojoExecutionException {
    return BufferedReaderHelper.readFromBuffer(reader).replaceAll(PACKAGE_TAG_REGEX, "");
  }

  private String getSimpleSuiteContents() {
    logInfoMessage("Suite file name doesn't exist, using generated simple suite.");
    return SIMPLE_SUITE_CONTENTS;
  }
}
