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

package com.universalmediaserver.external;

import java.io.File;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Install external dependencies in staging directory to local repository.
 *
 * @goal localinstall
 * @phase generate-sources
 * @threadSafe
 */
public class LocalInstallExternalDependencyMojo extends AbstractExternalDependencyMojo {

	public void execute() throws MojoExecutionException, MojoFailureException {
		super.execute();

		getLog().info("Installing external dependencies into local repository..");

		// Loop over and process all configured artifacts
		for (final ArtifactItem artifactItem : artifactItems) {
			getLog().info(String.format("Resolving artifact %s for installation", artifactItem.toString()));

			// Create Maven artifact
			Artifact artifact = createArtifact(artifactItem);

			// Determine if the artifact is already installed in the local Maven repository
			boolean artifactAlreadyInstalled = getLocalRepoFile(artifact).exists();

			// Only proceed with this artifact if it is not already installed or it is configured to be forced.
			if (!artifactAlreadyInstalled || artifact.isSnapshot() || force || artifactItem.getForce()) {
				if (artifactItem.getForce()) {
					getLog().debug(String.format("Artifact %s is flagged as a FORCED install", artifactItem.toString()));
				}

				// Ensure the artifact file is located in the staging directory
				File stagedArtifactFile = getFullyQualifiedArtifactFilePath(artifactItem);
				if (stagedArtifactFile.exists()) {
					// Install Maven artifact to local repository
					if (artifactItem.getInstall()) {
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

						installArtifact(artifactItem, artifact, stagedArtifactFile);
					} else {
						getLog().info("Configured not to install artifact: " + artifactItem.toString());
					}
				} else {
					// Throw error because we were unable to install the external dependency
					throw new MojoFailureException(
						"Unable to install external dependency \"" + artifactItem.getArtifactId() +
						"\"; file not found in staging path: " + stagedArtifactFile.getAbsolutePath() + NEWLINE +
						"Make sure \"resolve\" goal has been executed before \"install\"."
					);
				}
			} else {
				getLog().info(String.format(
					"Aritifact %s already exists in the local repository; no installation is needed",
					artifactItem.toString()
				));
			}
		}

		getLog().info("Finished installing all external dependencies into local repository");
	}
}
