package com.github.kentolsen;

import org.apache.maven.plugin.MojoExecutionException;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.mockito.Matchers.argThat;
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
 * Tests for the AcceptanceTestSelector class.
 *
 * @author Kent Olsen
 */
public class AcceptanceTestSelectorTest {

  @Mock
  private BufferedReader suiteFileNameReader;
  @Mock
  private BufferedReader shaUrlReader;
  @Mock
  private BufferedReader gitCommandReader;
  @Mock
  private FileWriter suiteFileWriter;

  private MyAcceptanceTestSelector sut;

  @BeforeMethod
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    sut = new MyAcceptanceTestSelector(suiteFileNameReader, shaUrlReader, gitCommandReader, suiteFileWriter);
  }

  @Test
  public void execute_skipFilter() throws MojoExecutionException, IOException {
    sut.setSkipFilter(true);
    sut.execute();
    final FileWriter writer = sut.getSuiteFileWriter();
    verify(writer, never()).write(anyString());
  }

  @Test (expectedExceptions = MojoExecutionException.class)
  public void execute_nullSuiteFileName() throws Exception {
    sut.setSuiteFileName(null);
    sut.execute();
  }

  @Test (expectedExceptions = MojoExecutionException.class)
  public void execute_nullShaUrl() throws Exception {
    System.setProperty(AcceptanceTestSelector.LAST_SUCCESSFUL_REVISION, "");
    sut.setSuiteFileName(new File("test"));
    sut.setShaUrl(null);
    sut.execute();
  }

  @Test (expectedExceptions = MojoExecutionException.class)
  public void execute_nullSourceRoot() throws Exception {
    sut.setSuiteFileName(new File("test"));
    sut.setShaUrl(new URL("file:///test"));
    sut.setSourceRoot(null);
    sut.execute();
  }

  @Test (expectedExceptions = MojoExecutionException.class)
  public void execute_nullSuiteMappings() throws Exception {
    sut.setSuiteFileName(new File("test"));
    sut.setShaUrl(new URL("file:///test"));
    sut.setSourceRoot("/home/foo/example");
    sut.setSuiteMappings(null);
    sut.execute();
  }

  @Test (expectedExceptions = MojoExecutionException.class)
  public void execute_emptySuiteMappings() throws Exception {
    sut.setSuiteFileName(new File("test"));
    sut.setShaUrl(new URL("file:///test"));
    sut.setSourceRoot("/home/foo/example");
    sut.setSuiteMappings(new Properties());
    sut.execute();
  }

  @Test (expectedExceptions = MojoExecutionException.class)
  public void execute_missingPackageFileName() throws Exception {
    sut.setSuiteFileName(new File("test"));
    sut.setShaUrl(new URL("file:///test"));
    sut.setSourceRoot("/home/foo/example");
    final Properties suiteMappings = new Properties();
    suiteMappings.setProperty("foo", "bar");
    sut.setSuiteMappings(suiteMappings);
    sut.setPackageFileName(null);
    final Properties integrationSuiteMappings = new Properties();
    integrationSuiteMappings.setProperty("fruit", "apple");
    sut.setIntegrationSuiteMappings(integrationSuiteMappings);
    sut.execute();
  }

  @Test (expectedExceptions = MojoExecutionException.class)
  public void execute_nullIntegrationSuiteMappings() throws Exception {
    sut.setSuiteFileName(new File("test"));
    sut.setShaUrl(new URL("file:///test"));
    sut.setSourceRoot("/home/foo/example");
    final Properties suiteMappings = new Properties();
    suiteMappings.setProperty("foo", "bar");
    sut.setSuiteMappings(suiteMappings);
    sut.setPackageFileName("package.json");
    sut.setIntegrationSuiteMappings(null);
    sut.execute();
  }

  @Test (expectedExceptions = MojoExecutionException.class)
  public void execute_emptyIntegrationSuiteMappings() throws Exception {
    sut.setSuiteFileName(new File("test"));
    sut.setShaUrl(new URL("file:///test"));
    sut.setSourceRoot("/home/foo/example");
    final Properties suiteMappings = new Properties();
    suiteMappings.setProperty("foo", "bar");
    sut.setSuiteMappings(suiteMappings);
    sut.setPackageFileName("package.json");
    sut.setIntegrationSuiteMappings(new Properties());
    sut.execute();
  }

  @Test
  public void execute_noSuiteFile() throws Exception {

    sut.setShaUrl(new URL("http://localhost:8080/example-jenkins/blah"));
    sut.setSourceRoot("/home/foo/example");
    final File suiteFileName = mock(File.class);
    when(suiteFileName.exists()).thenReturn(false);
    sut.setSuiteFileName(suiteFileName);
    sut.setSuiteMappings(createSuiteMappings());
    sut.setPackageFileName("package.json");
    sut.setIntegrationSuiteMappings(createIntegrationSuiteMappings());

    when(shaUrlReader.readLine())
      .thenReturn("1234567890")
      .thenReturn(null);
    when(gitCommandReader.readLine())
      .thenReturn("src/main/java/com/example/package1/path2/Class2.java")
      .thenReturn("src/main/java/com/example/package1/path4/Class4.java")
      .thenReturn("src/main/java/com/example/package1/path4/Class17.java")
      .thenReturn(null);

    sut.execute();

    List<String> classes = new ArrayList<String>();
    classes.add("<classes>");
    classes.add("Path2Test");
    classes.add("Path4Test");
    classes.add("</classes>");
    verify(suiteFileWriter).write(argThat(new StringListMatcher(classes)));
  }

  @Test
  public void execute_existingSuiteFileName() throws Exception {

    sut.setShaUrl(new URL("http://localhost:8080/example-jenkins/blah"));
    sut.setSourceRoot("/home/foo/example");
    final File suiteFileName = mock(File.class);
    when(suiteFileName.exists()).thenReturn(true);
    sut.setSuiteFileName(suiteFileName);
    final Properties mappings = createSuiteMappings();
    mappings.setProperty("src/main/java/com/example/package1/path2", ""); // verify empty value handling
    sut.setSuiteMappings(mappings);
    sut.setPackageFileName("package.json");
    sut.setIntegrationSuiteMappings(createIntegrationSuiteMappings());

    when(shaUrlReader.readLine())
      .thenReturn("1234567890")
      .thenReturn(null);
    when(suiteFileNameReader.readLine())
      .thenReturn("<suite>")
      .thenReturn("<test>")
      .thenReturn("</test>")
      .thenReturn("</suite>")
      .thenReturn(null);
    when(gitCommandReader.readLine())
      .thenReturn("src/main/java/com/example/package1/path2/Class2.java")
      .thenReturn("src/main/java/com/example/package1/path4/Class4.java")
      .thenReturn(null)
      .thenReturn("+ tree-descendancy")
      .thenReturn(null);

    sut.execute();

    List<String> classes = new ArrayList<String>();
    classes.add("<classes>");
    classes.add("Path4Test");
    classes.add("DescendancyIntegrationTest");
    verify(suiteFileWriter).write(argThat(new StringListMatcher(classes)));
    classes = new ArrayList<String>();
    classes.add("Path2Test");
    verify(suiteFileWriter, never()).write(argThat(new StringListMatcher(classes)));
  }

  @Test
  public void execute_shaWrappedInTag() throws Exception {

    sut.setShaUrl(new URL("http://localhost:8080/example-jenkins/blah"));
    sut.setSourceRoot("/home/foo/example");
    final File suiteFileName = mock(File.class);
    when(suiteFileName.exists()).thenReturn(true);
    sut.setSuiteFileName(suiteFileName);
    sut.setSuiteMappings(createSuiteMappings());
    sut.setPackageFileName("package.json");
    sut.setIntegrationSuiteMappings(createIntegrationSuiteMappings());

    when(shaUrlReader.readLine())
      .thenReturn("<SHA1>1234567890</SHA1>")
      .thenReturn(null);
    when(suiteFileNameReader.readLine())
      .thenReturn("<suite>")
      .thenReturn("</suite>")
      .thenReturn(null);
    when(gitCommandReader.readLine())
      .thenReturn("src/main/java/com/example/package1/path2/Class2.java")
      .thenReturn("src/main/java/com/example/package1/path4/Class4.java")
      .thenReturn(null)
      .thenReturn("+ fanchart\n+ tree-port-pedigree")
      .thenReturn(null);

    sut.execute();

    List<String> classes = new ArrayList<String>();
    classes.add("<classes>");
    classes.add("Path2Test");
    classes.add("Path4Test");
    classes.add("PortraitPedigreeIntegrationTest");
    classes.add("FanChartIntegrationTest");
    verify(suiteFileWriter, never()).write(argThat(new StringListMatcher(classes)));
  }

  @Test
  public void execute_fallbackBecauseNoMatchedChanges() throws Exception {

    sut.setShaUrl(new URL("http://localhost:8080/example-jenkins/blah"));
    sut.setSourceRoot("/home/foo/example");
    final File suiteFileName = mock(File.class);
    when(suiteFileName.exists()).thenReturn(true);
    sut.setSuiteFileName(suiteFileName);
    sut.setSuiteMappings(createSuiteMappings());
    sut.setPackageFileName("package.json");
    sut.setIntegrationSuiteMappings(createIntegrationSuiteMappings());

    System.setProperty(AcceptanceTestSelector.LAST_SUCCESSFUL_REVISION, "1234567890");
    when(suiteFileNameReader.readLine())
      .thenReturn("<suite>")
      .thenReturn("</suite>")
      .thenReturn(null);
    when(gitCommandReader.readLine())
      .thenReturn("src/main/java/com/example/package1/path5/Class5.java")
      .thenReturn("src/main/java/com/example/package1/path7/Class7.java")
      .thenReturn(null)
      .thenReturn(null);

    sut.execute();

    List<String> classes = new ArrayList<String>();
    classes.add("<classes>");
    classes.add("FallbackTest");
    verify(suiteFileWriter, never()).write(argThat(new StringListMatcher(classes)));
  }

  @Test (expectedExceptions = MojoExecutionException.class)
  public void execute_emptySha() throws Exception {

    sut.setShaUrl(new URL("http://localhost:8080/example-jenkins/blah"));
    sut.setSourceRoot("/home/foo/example");
    final File suiteFileName = mock(File.class);
    when(suiteFileName.exists()).thenReturn(true);
    sut.setSuiteFileName(suiteFileName);
    sut.setSuiteMappings(createSuiteMappings());

    when(shaUrlReader.readLine())
      .thenReturn(null);
    when(suiteFileNameReader.readLine())
      .thenReturn("<suite>")
      .thenReturn("</suite>")
      .thenReturn(null);
    when(gitCommandReader.readLine())
      .thenReturn("src/main/java/com/example/package1/path2/Class2.java")
      .thenReturn("src/main/java/com/example/package1/path4/Class4.java")
      .thenReturn(null);

    sut.execute();
  }

  @Test
  public void getSuiteFileNameReader() throws Exception {
    File file = File.createTempFile("tmp", "txt");
    AcceptanceTestSelector selector = new AcceptanceTestSelector();
    selector.setSuiteFileName(file);
    final BufferedReader bufferedReader = selector.getSuiteFileNameReader();
    Assert.assertNotNull(bufferedReader);
    bufferedReader.close();
    Assert.assertTrue(file.delete());
  }

  @Test
  public void getShaUrlReader() throws Exception {
    File file = File.createTempFile("tmp", "txt");
    URL url = file.toURI().toURL();
    AcceptanceTestSelector selector = new AcceptanceTestSelector();
    selector.setShaUrl(url);
    final BufferedReader bufferedReader = selector.getShaUrlReader();
    Assert.assertNotNull(bufferedReader);
    bufferedReader.close();
    Assert.assertTrue(file.delete());
  }

  @Test
  public void getGitCommandReader() throws Exception {
    AcceptanceTestSelector selector = new AcceptanceTestSelector();
    selector.setSourceRoot(".");
    BufferedReader bufferedReader = selector.getGitCommandReader("ls");
    Assert.assertNotNull(bufferedReader);
    bufferedReader.close();
  }

  @Test
  public void getSuiteFileWriter() throws Exception {
    AcceptanceTestSelector selector = new AcceptanceTestSelector();
    File file = File.createTempFile("tmp", "txt");
    selector.setSuiteFileName(file);
    final FileWriter fileWriter = selector.getSuiteFileWriter();
    Assert.assertNotNull(fileWriter);
    fileWriter.close();
    Assert.assertTrue(file.delete());
  }

  private Properties createSuiteMappings() {
    Properties suiteMappings = new Properties();
    suiteMappings.setProperty("src/main/java/com/example/package1/path1", "Path1Test");
    suiteMappings.setProperty("src/main/java/com/example/package1/path2", "Path2Test");
    suiteMappings.setProperty("src/main/java/com/example/package1/path3", "Path3Test");
    suiteMappings.setProperty("src/main/java/com/example/package1/path4", "Path4Test");
    suiteMappings.setProperty(AcceptanceTestSelector.FALLBACK_CODE_PATH, "FallbackTest");
    return suiteMappings;
  }

  private Properties createIntegrationSuiteMappings() {
    final Properties integrationSuiteMappings = new Properties();
    integrationSuiteMappings.setProperty("fanchart", "FanChartIntegrationTest");
    integrationSuiteMappings.setProperty("tree-port-pedigree", "PortraitPedigreeIntegrationTest");
    integrationSuiteMappings.setProperty("tree-descendancy", "DescendancyIntegrationTest");
    return integrationSuiteMappings;
  }

  private class MyAcceptanceTestSelector extends AcceptanceTestSelector {

    private final BufferedReader suiteFileNameReader;
    private final BufferedReader shaUrlReader;
    private final BufferedReader gitCommandReader;
    private final FileWriter suiteFileWriter;

    MyAcceptanceTestSelector(BufferedReader suiteFileNameReader, BufferedReader shaUrlReader, BufferedReader gitCommandReader, FileWriter suiteFileWriter) {
      this.suiteFileNameReader = suiteFileNameReader;
      this.shaUrlReader = shaUrlReader;
      this.gitCommandReader = gitCommandReader;
      this.suiteFileWriter = suiteFileWriter;
    }

    @Override
    protected BufferedReader getSuiteFileNameReader() throws MojoExecutionException {
      return suiteFileNameReader;
    }

    @Override
    protected BufferedReader getShaUrlReader() throws MojoExecutionException {
      return shaUrlReader;
    }

    @Override
    protected BufferedReader getGitCommandReader(String sha) throws MojoExecutionException {
      return gitCommandReader;
    }

    @Override
    protected FileWriter getSuiteFileWriter() throws MojoExecutionException {
      return suiteFileWriter;
    }
  }

  private class StringListMatcher extends BaseMatcher<String> {

    private List<String> matchingStrings;

    StringListMatcher(List<String> matchingStrings) {
      this.matchingStrings = matchingStrings;
    }

    public boolean matches(Object o) {
      boolean returnValue = true;
      String strings = (String) o;
      for (String matchingString : matchingStrings) {
        if (!strings.contains(matchingString)) {
          returnValue = false;
          break;
        }
      }
      return returnValue;
    }

    public void describeTo(Description description) {
    }
  }
}
