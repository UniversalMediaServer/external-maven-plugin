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
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;

/**
 * Deploy external dependencies to distribution management defined repository.
 *
 * @goal deploy
 * @phase deploy
 * @category Maven Plugin
 * @ThreadSafe
 */
public class DeployExternalDependencyMojo extends AbstractExternalDependencyMojo {

	/**
	 * @component
	 */
	private ArtifactDeployer artifactDeployer;

	public void execute() throws MojoExecutionException, MojoFailureException {
		super.execute();

		getLog().info("Starting to deploy external dependencies to distribution repository");

		// Loop over and process all configured artifacts
		for (ArtifactItem artifactItem : artifactItems) {
			getLog().info("Resolving artifact in locale repository for deployment: " + artifactItem.toString());

			// Create Maven artifact
			Artifact artifact = createArtifact(artifactItem);

			// Determine if the artifact is already installed in the local Maven repository
			File installedArtifactFile = getLocalRepoFile(artifact);

			// Only proceed with this artifact if it is installed in the local repository.
			if (installedArtifactFile.exists()) {
				if (!resolveArtifactItem(artifact)) {
					throw new MojoExecutionException(
						"Could not resolve artifact " + artifact.toString() + NEWLINE +
						"Make sure \"resolve\" and \"install\" goals has been executed first"
					);
				}

				// Deploy to distribution Maven repository
				if (artifactItem.getDeploy()) {

					ArtifactRepository repo = getDeploymentRepository();

					String protocol = repo.getProtocol();

					if (protocol.equalsIgnoreCase("scp")) {
						File sshFolder = new File(System.getProperty("user.home"), ".ssh");

						if (!sshFolder.exists()) {
							sshFolder.mkdirs();
						}
					}

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
							artifact.addMetadata(pomMetadata);
						} else {
							// Dynamically create a new POM file for this artifact
							generatedPomFile = generatePomFile(artifactItem);
							ProjectArtifactMetadata pomMetadata = new ProjectArtifactMetadata(artifact, generatedPomFile);

							if (artifactItem.getGeneratePom() == true) {
								artifact.addMetadata(pomMetadata);
							}
						}

					}

					// Deploy now
					getLog().info("Deploying artifact to distribution repository: " + artifactItem.toString());
					try {
						artifactDeployer.deploy(artifact.getFile(), artifact, repo, localRepository);
					} catch (ArtifactDeploymentException e) {
						throw new MojoExecutionException("Deployment of external dependency failed with: " + e.getMessage(), e);
					}
				} else {
					getLog().debug("Configured to not deploy artifact: " + artifactItem.toString());
				}
			} else {
				// Throw exception because we were unable to find the installed external dependency
				throw new MojoExecutionException(
					"Unable to find external dependency \"" + artifactItem.getArtifactId() +
					"\"; file not found in local repository: " + installedArtifactFile.getAbsolutePath()
				);
			}
		}

		getLog().info("Finished deploying external dependencies to distribution repository");
	}

	/**
	 * Gets the repository defined in project POM's distribution management
	 * section
	 *
	 * @return
	 * 			Deployment repository defined in distribution management
	 *
	 * @throws MojoExecutionException
	 */
	private ArtifactRepository getDeploymentRepository() throws MojoExecutionException {
		ArtifactRepository repo = null;

		if (repo == null) {
			repo = project.getDistributionManagementArtifactRepository();
		}

		if (repo == null) {
			throw new MojoExecutionException(
				"Deployment failed: Repository element was not specified in the POM inside distributionManagement element"
			);
		}

		return repo;
	}
}
