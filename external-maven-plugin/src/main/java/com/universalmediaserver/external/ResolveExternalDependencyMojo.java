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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Download/Acquire external Maven artifacts, copy to staging directory.
 *
 * @goal resolve
 * @phase generate-sources
 * @category Maven Plugin
 * @ThreadSafe
 */
public class ResolveExternalDependencyMojo extends AbstractExternalDependencyMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		super.execute();
		getLog().info("Resolving external dependencies..");

		// Get a set of all project artifacts
		// Set<Artifact> projectArtifacts = project.createArtifacts(artifactFactory, null, null );

		Map<URL, File> cachedDownloads = new HashMap<URL, File>();

		// Loop over and process all configured artifacts
		for (final ArtifactItem artifactItem : artifactItems) {
			getLog().info("Attempting to resolve external artifact: " + artifactItem.toString());

			// Create Maven artifact
			Artifact artifact = createArtifact(artifactItem);

			/*
			 * Now that the file has been successfully downloaded and the
			 * checksum verification has passed (if required), lets copy
			 * the temporary file to the staging location
			 */
			final File artifactFile = getFullyQualifiedArtifactFilePath(artifactItem);

			/*
			 * Get the file if it doesn't exist in the staging
			 * directory, it is a snapshot (can have changed), or the artifact
			 * or the installation is forced.
			 */
			if (!artifactFile.exists() || artifact.isSnapshot() || force || artifactItem.getForce()) {

				if (artifactItem.getForce()) {
					getLog().debug(String.format("Artifact %s is flagged as a FORCED download", artifactItem.toString()));
				}

				downloadArtifact(artifactItem, artifact, artifactFile, cachedDownloads);

				getLog().info(String.format("Artifact %s downloaded and staged", artifactItem.toString()));

				verifyArtifact(artifactItem, artifactFile);
			} else {
				getLog().info(String.format(
					"External artifact %s already exists in staging directory; no download needed.",
					artifactItem.toString()
				));
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

		getLog().info("Finished resolving all external dependencies");
	}
}
