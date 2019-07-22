// SPDX-License-Identifier: MIT
package com.daimler.sechub.integrationtest.api;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daimler.sechub.integrationtest.internal.IntegrationTestFileSupport;
import com.daimler.sechub.integrationtest.internal.SecHubClientExecutor;
import com.daimler.sechub.integrationtest.internal.SecHubClientExecutor.ExecutionResult;
import com.daimler.sechub.test.TestUtil;

/**
 * This test class tries to use the former build sechub execution
 *
 * @author Albert Tregnaghi
 *
 */
public class WithSecHubClient {
	private static final Logger LOG = LoggerFactory.getLogger(WithSecHubClient.class);

	private AsUser asUser;
	private Path outputFolder;

	private boolean stopOnYellow;

	WithSecHubClient(AsUser asUser) {
		this.asUser = asUser;
		try {
			this.outputFolder = Files.createTempDirectory("with-sechub-client-");
			this.outputFolder.toFile().deleteOnExit();
		} catch (IOException e) {
			throw new IllegalStateException("Can NOT create temp directory for tests!", e);
		}
	}

	public WithSecHubClient enableStopOnYellow() {
		this.stopOnYellow = true;
		return this;
	}

	public class AssertAsyncResult {
		private UUID jobUUID;
		public File configFile;

		public UUID getJobUUID() {
			return jobUUID;
		}

		private AssertAsyncResult() {

		}

		public AssertAsyncResult assertJobTriggered() {
			/* having a job uuid means it was done */
			assertNotNull(jobUUID);
			return this;
		}

		/**
		 * Asserts file was uploaded for project
		 *
		 * @param project
		 * @param sha256checksum
		 * @return
		 */
		public AssertZipFileUpload assertFileUploadedAsZip(TestProject project) {
			File file = assertFile(project);
			return new AssertZipFileUpload(file);
		}

		public AssertAsyncResult assertFileUploaded(TestProject project) {
			assertFile(project);
			return this;
		}

		private File assertFile(TestProject project) {
			/* the filename at upload is currently always sourcecode.zip! */
			File file = TestAPI.getFileUploaded(project, jobUUID, "sourcecode.zip");
			if (file == null) {
				fail("NO file upload for " + jobUUID + " in project +" + project);
			}

			LOG.info("Uploaded file for job {} was re-downloaded to {}", jobUUID, file);

			return file;
		}
	}

	public class AssertZipFileUpload {
		private File downloadedFile;
		Path unzipTo;

		private AssertZipFileUpload(File file) {
			if (file == null) {
				throw new IllegalArgumentException("Zip file may not be null");
			}
			if (!file.exists()) {
				throw new IllegalArgumentException("Zip file does not exist:" + file);
			}
			this.downloadedFile = file;

			try {
				unzipTo = Files.createTempDirectory("sechub-assertzip");
				if (TestUtil.isDeletingTempFiles()) {
					unzipTo.toFile().deleteOnExit();
				}
				/* unzip */
				TestUtil.unzip(downloadedFile, unzipTo);
				LOG.info("Unzipped re-downloaded zipfile {} to {}", downloadedFile, unzipTo);

			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

		}

		public AssertZipFileUpload zipContains(String pathToFile) {
			File f = new File(unzipTo.toFile(), pathToFile);
			if (!f.exists()) {
				fail("File does not exist:" + pathToFile + "\n - looked at :" + f.getAbsolutePath());
			}
			return this;
		}

		public AssertZipFileUpload zipNotContains(String pathToFile) {
			File f = new File(unzipTo.toFile(), pathToFile);
			if (f.exists()) {
				fail("File does exist:" + pathToFile + "\n - looked at :" + f.getAbsolutePath());
			}
			return this;
		}

	}

	public AssertAsyncResult startAsynchronScanFor(TestProject project, String jsonConfigfile) {
		File file = IntegrationTestFileSupport.getTestfileSupport().createFileFromResourcePath(jsonConfigfile);
		SecHubClientExecutor executor = new SecHubClientExecutor();
		List<String> list = buildCommand(project, false);
		list.add("scanAsync");
		ExecutionResult result = doExecute(file, executor, list);
		if (result.getExitCode() != 0) {
			fail("Not exit code 0 but:" + result.getExitCode());
		}
		AssertAsyncResult asynchResult = new AssertAsyncResult();
		asynchResult.jobUUID = UUID.fromString(result.getLastOutputLine());
		asynchResult.configFile = file;
		return asynchResult;
	}

	/**
	 * Starts a synchronous scan for given project.
	 *
	 * @param project
	 * @param jsonConfigfile name of the config file which shall be used. Its
	 *                       automatically resolved from test file support.
	 * @return
	 */
	public ExecutionResult startSynchronScanFor(TestProject project, String jsonConfigfile) {
		File file = IntegrationTestFileSupport.getTestfileSupport().createFileFromResourcePath(jsonConfigfile);
		SecHubClientExecutor executor = new SecHubClientExecutor();

		List<String> list = buildCommand(project, true);
		list.add("scan");

		return doExecute(file, executor, list);
	}

	private ExecutionResult doExecute(File file, SecHubClientExecutor executor, List<String> list) {
		return executor.execute(file, asUser.user, list.toArray(new String[list.size()]));
	}

	private List<String> buildCommand(TestProject project, boolean withWait0) {
		List<String> list = new ArrayList<>();
		list.add("-server");
		list.add(asUser.getServerURL());
		list.add("-project");
		list.add(project.getProjectId());
		list.add("-output");
		list.add(outputFolder.toFile().getAbsolutePath());
		if (withWait0) {
			list.add("-wait");
			list.add("0");
		}
		if (stopOnYellow) {
			list.add("-stop-on-yellow");
		}
		return list;
	}
}