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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;

/**
 * Install external dependencies to local repository.
 *
 * @goal install
 * @phase generate-sources
 * @category Maven Plugin
 * @ThreadSafe
 */
public class InstallExternalDependencyMojo extends AbstractExternalDependencyMojo {

	/**
	 * @component
	 */
	protected ArtifactInstaller installer;

	/**
	 * Flag whether to create checksums (MD5, SHA-1) or not.
	 *
	 * @parameter property="createChecksum" default-value="true"
	 */
	protected boolean createChecksum;

	public void execute() throws MojoExecutionException, MojoFailureException {
		super.execute();

		getLog().info("Installing external dependencies into local repository..");

		// Loop over and process all configured artifacts
		for (ArtifactItem artifactItem : artifactItems) {
			getLog().info(String.format("Resolving artifact %s for installation", artifactItem.toString()));

			// Create Maven artifact
			Artifact artifact = createArtifact(artifactItem);

			// Determine if the artifact is already installed in the local Maven repository
			Boolean artifactAlreadyInstalled = getLocalRepoFile(artifact).exists();

			// Only proceed with this artifact if it is not already installed or it is configured to be forced.
			if (!artifactAlreadyInstalled || force || artifactItem.getForce()) {
				if (artifactItem.getForce()) {
					getLog().debug(String.format("Artifact %s is flagged as a FORCED install", artifactItem.toString()));
				}

				// Ensure the artifact file is located in the staging directory
				File stagedArtifactFile = getFullyQualifiedArtifactFilePath(artifactItem);
				if (stagedArtifactFile.exists()) {
					if (artifactItem.hasExtractFile()) {
						/*
						 * If this artifact is configured to extract a file,
						 * then the checksum verification will need to take
						 * place if there is a separate extract file checksum
						 * property defined
						 */
						if (artifactItem.hasExtractFileChecksum()) {
							/*
							 * Verify extracted file checksum (if an extract
							 * file checksum was defined). MojoFailureException
							 * exception will be thrown if verification fails
							 */
							verifyArtifactItemExtractFileChecksum(artifactItem, stagedArtifactFile);
						}
					} else {
						/*
						 * If this is not an extracted file, then verify the
						 * downloaded file using the regular checksum property
						 *
						 * Verify file checksum (if a checksum was defined).
						 * MojoFailureException exception will be thrown if
						 * verification fails
						 */
						verifyArtifactItemChecksum(artifactItem, stagedArtifactFile);
					}

					/*
					 * perform search.maven.org REST query to ensure that this
					 * artifacts checksum is not resolved to an existing
					 * artifact already hosted in another Maven repository
					 */
					verifyArtifactItemChecksumByCentralLookup(artifactItem, stagedArtifactFile);

					// Install Maven artifact to local repository
					if (artifact != null && artifactItem.getInstall()) {
						// Create Maven artifact POM file
						File generatedPomFile = null;

						// Don't generate a POM file for POM artifacts
						if (!"pom".equals(artifactItem.getPackaging())) {
							if (artifactItem.getPomFile() != null) {
								/*
								 * If a POM file was provided for the artifact
								 * item, then use that POM file instead of
								 * generating a new one
								 */
								ProjectArtifactMetadata pomMetadata = new ProjectArtifactMetadata(artifact,
									artifactItem.getPomFile());
								getLog().debug("Installing defined POM file: " + artifactItem.getPomFile());
								artifact.addMetadata(pomMetadata);
							} else {
								// Dynamically create a new POM file for this artifact
								generatedPomFile = generatePomFile(artifactItem);
								ProjectArtifactMetadata pomMetadata = new ProjectArtifactMetadata(artifact, generatedPomFile);

								if (artifactItem.getGeneratePom() == true) {
									getLog().debug(
										"installing generated POM file: " + generatedPomFile.getAbsolutePath());
									artifact.addMetadata(pomMetadata);
								}
							}
						}

						getLog().info("Installing artifact into local repository: " + localRepository.getId());

						// Install artifact to local repository
						try {
							installer.install(stagedArtifactFile, artifact, localRepository);
						} catch (ArtifactInstallationException e) {
							throw new MojoExecutionException(
								"Could not install artifact " + artifact.toString() + " to local repository: " + e.getMessage(), e
							);
						}

						// Install checksum files to local repository
						if (artifactItem.getCreateChecksum() != null) {
							super.createChecksum = artifactItem.getCreateChecksum().equalsIgnoreCase("true");
						} else {
							super.createChecksum = this.createChecksum;
						}
						installChecksums(artifact, super.createChecksum);
					} else {
						getLog().debug("Configured to not install artifact: " + artifactItem.toString());
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
