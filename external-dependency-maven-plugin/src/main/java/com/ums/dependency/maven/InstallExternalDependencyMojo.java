/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 **/

package com.ums.dependency.maven;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Download/acquire and install external dependencies to local repository.
 *
 * @goal install
 * @phase generate-sources
 * @category Maven Plugin
 * @ThreadSafe
 */
public class InstallExternalDependencyMojo extends AbstractExternalDependencyMojo {

	public void execute() throws MojoExecutionException, MojoFailureException {
		super.execute();

		getLog().info("Installing external dependencies into local repository..");

		Map<URL, File> cachedDownloads = new HashMap<URL, File>();

		// Loop over and process all configured artifacts
		for (final ArtifactItem artifactItem : artifactItems) {
			getLog().debug("Attempting to install external artifact: " + artifactItem.toString());

			// Create Maven artifact
			Artifact artifact = createArtifact(artifactItem);

			// Determine if the artifact is already installed in the local Maven repository
			boolean artifactAlreadyInstalled = getLocalRepoFile(artifact).exists();

			// Determine if the artifact already is in the staging directory
			File artifactFile = getFullyQualifiedArtifactFilePath(artifactItem);

			/*
			 * Get the file if it doesn't exist in the staging
			 * directory, it is a snapshot (can have changed), or the artifact
			 * or the installation is forced.
			 */
			if (!artifactFile.exists() || artifact.isSnapshot() || force || artifactItem.getForce()) {
				if (getLog().isDebugEnabled()) {
					String reason;
					if (!artifactFile.exists()) {
						reason = "it's not in the staging directory";
					} else if (artifact.isSnapshot()) {
						reason = "it's a snapshot";
					} else {
						reason = "it is FORCED";
					}
					getLog().info(String.format(
						"Downloading artifact %s as " + reason,
						artifactItem.toString()
					));
				}

				downloadArtifact(artifactItem, artifact, artifactFile, cachedDownloads);

				verifyArtifact(artifactItem, artifactFile);
			} else if (artifactFile.exists()) {
				getLog().debug(String.format("Artifact %s is already in the staging directory, no download is needed", artifactItem.toString()));
			}

			getLog().debug(String.format("Resolving artifact %s for installation", artifactItem.toString()));

			/*
			 * Install the artifact if it's not installed, is a snapshot,
			 * or it or the installation is forced.
			 */
			if (artifactItem.getInstall() && (!artifactAlreadyInstalled || artifact.isSnapshot() || force || artifactItem.getForce())) {
				if (artifactAlreadyInstalled && artifact.isSnapshot()) {
					getLog().debug(String.format(
						"Reinstalling artifact %s into local repository because it's a snapshot ",
						artifactItem.toString()
					));
				} else if (artifactAlreadyInstalled) {
					getLog().debug(String.format(
						"Reinstalling artifact %s into local repository as it is FORCED",
						artifactItem.toString()
					));
				} else {
					getLog().debug(String.format(
						"Installing artifact %s into local repository \"%s\"", artifactItem.toString(), localRepository.getId()
					));
				}

				installArtifact(artifactItem, artifact, artifactFile);
			} else {
				if (!artifactItem.getInstall()) {
					getLog().info("Configured not to install artifact: " + artifactItem.toString());
				} else {
					getLog().debug(String.format(
						"Aritifact %s already exists in the local repository; no installation is needed",
						artifactItem.toString()
					));
				}
			}
		}

		if (cachedDownloads.size() > 0) {
			getLog().info("Deleting temporary download files");

			// We're done with the temporary files so lets delete them
			for (File tempDownloadFile : cachedDownloads.values()) {
				getLog().debug("Deleting file: " + tempDownloadFile.getAbsolutePath());
				tempDownloadFile.delete();
			}
		}

		getLog().info("Finished installing all external dependencies into local repository");
	}
}
