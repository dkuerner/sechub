// SPDX-License-Identifier: MIT
package com.daimler.sechub.test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Simplifies some issues with eclipse versus gradle scan testing etc. (Gradle
 * does use always the root folder, Eclipse is using the current project as
 * relative root). Does also support convenient text file reading etc.
 * 
 * @author Albert Tregnaghi
 *
 */
@SechubTestComponent
public class TestFileSupport {

	private final File detectedGradleRoot;

	private String resourcePath;

	protected TestFileSupport(String projectTestResourcePath) {
		if (projectTestResourcePath == null) {
			this.resourcePath = "";
		} else {
			if (projectTestResourcePath.endsWith("/")) {
				throw new IllegalArgumentException("Testcase corrupt, path may not end with / please change");
			}
			this.resourcePath = projectTestResourcePath;
		}

		File userDir = new File(System.getProperty("user.dir"));
		File gradleFolder = new File(userDir, "gradle");
		if (!gradleFolder.exists()) {
			// eclipse call from an eclipse project - so we got a src folder here
			File srcFolder = new File(userDir, "src");
			assertNotNull("no sourcefolder found!", srcFolder);

			File projectFolder = srcFolder.getParentFile();
			assertNotNull("no projectfolder found for source folder:" + srcFolder.getAbsolutePath(), projectFolder);

			File rootProjectFolder = projectFolder.getParentFile();
			assertNotNull("no root project folder found for project:" + projectFolder.getAbsolutePath(),
					rootProjectFolder);
			gradleFolder = new File(rootProjectFolder, "gradle");
		}
		if (gradleFolder.exists()) {
			detectedGradleRoot = gradleFolder.getParentFile();
		} else {
			throw new IllegalStateException("Testcase szenario corrupt, cannot determine gradle root folder!");
		}
	}

	public InputStream getInputStreamTo(String resourcePath) {
		File file = createFileFromResourcePath(resourcePath);
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Create method does test file exists - and so this MUST exist here!",e);
		}
	}
	
	public File getRootFolder() {
		return detectedGradleRoot;
	}
	/**
	 * Loads a test file from defined project test resource path + given path, will
	 * use \n as line break
	 * 
	 * @param pathFromRoot
	 * @return text file
	 * @throws IllegalArgumentException
	 *             when file cannot be found (runtime exception to reduce bloating
	 *             stuff)
	 */
	public String loadTestFile(String path) {
		return loadTestFile(path, "\n");
	}

	/**
	 * Loads a test file from defined project test resource path + given path - does
	 * also asserts that file exists
	 * 
	 * @param path
	 *            - relative path, may not start with /
	 * @return text file
	 * @throws IllegalArgumentException
	 *             when file cannot be found (runtime exception to reduce bloating
	 *             stuff)
	 */
	public String loadTestFile(String path, String lineBreak) {
		return loadTestFileFromRoot(createPathFromRoot(path), lineBreak);
	}

	private String createPathFromRoot(String path) {
		return resourcePath + "/" + path;
	}

	/**
	 * Loads a test file from root path, will use \n as line break
	 * 
	 * @param pathFromRoot
	 * @return text file
	 * @throws IllegalStateException
	 *             when file cannot be found (runtime exception to reduce bloating
	 *             stuff)
	 */
	public String loadTestFileFromRoot(String pathFromRoot) {
		return loadTestFileFromRoot(pathFromRoot, "\n");
	}

	/**
	 * Loads a test file from root path - does also asserts that file exists
	 * 
	 * @param pathFromRoot
	 * @return text file
	 * @throws IllegalStateException
	 *             when file cannot be found (runtime exception to reduce bloating
	 *             stuff)
	 */
	public String loadTestFileFromRoot(String pathFromRoot, String lineBreak) {
		if (lineBreak == null) {
			throw new IllegalArgumentException("Testcase corrupt: Line break may not be null!");
		}
		File file = createFileFromRoot(pathFromRoot);

		return loadTextFile(file, lineBreak);
	}

	
	/**
	 * 
	 * @param file
	 * @param lineBreak
	 * @return
	 */
	public String loadTextFile(File file, String lineBreak) {
		StringBuilder sb = new StringBuilder();

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"))) {
			String line = null;

			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append(lineBreak);
			}
			return sb.toString();
		} catch (Exception e) {
			throw new IllegalStateException("Testcase corrupt: Cannot read test file " + file.getAbsolutePath(), e);
		}
	}

	/**
	 * Creates a file inside project - does also asserts that file exists
	 * 
	 * @param pathFromRoot
	 * @return file
	 * @throws IllegalStateException
	 *             when file cannot be found (runtime exception to reduce bloating
	 *             stuff)
	 */
	public File createFileFromRoot(String pathFromRoot) {
		return assertFile(new File(detectedGradleRoot, pathFromRoot));
	}

	/**
	 * Creates a file based resource path given at constructor- does also asserts
	 * that file exists. So if you are using 'src/test/resources' in constructor
	 * call as resource path you must be aware that you have no access to normal
	 * resources... In this case use the {@link #createFileFromRoot(String)} method
	 * instead
	 * 
	 * @param pathInProject
	 * @return file
	 * @throws IllegalStateException
	 *             when file cannot be found (runtime exception to reduce bloating
	 *             stuff)
	 */
	public File createFileFromResourcePath(String pathInProject) {
		return assertFile(new File(createFileFromRoot(resourcePath), pathInProject));
	}

	private File assertFile(File file) {
		if (file == null) {
			throw new IllegalArgumentException("file is null!");
		}
		if (!file.exists()) {
			throw new IllegalStateException("Testcase corrupt: Test file does not exist:" + file.getAbsolutePath());
		}
		return file;
	}
}
