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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.manager.WagonConfigurationException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.util.FileUtils;

/**
 * Download/Acquire external Maven artifacts, copy to staging directory.
 *
 * @goal resolve
 * @category Maven Plugin
 * @ThreadSafe
 */
public class ResolveExternalDependencyMojo extends AbstractExternalDependencyMojo {

	/**
	 * @component
	 * @readonly
	 */
	protected ArchiverManager archiverManager;

	/**
	 * @component
	 * @required
	 * @readonly
	 */
	private WagonManager wagonManager;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		super.execute();
		getLog().info("Resolving external dependencies..");

		// get a set of all project artifacts
		// Set<Artifact> projectArtifacts = project.createArtifacts(
		// artifactFactory, null, null );

		Map<URL, File> cachedDownloads = new HashMap<URL, File>();

		// loop over and process all configured artifacts
		for (final ArtifactItem artifactItem : artifactItems) {
			getLog().info("Attempting to resolve external artifact: " + artifactItem.toString());

			// Create Maven artifact
			Artifact artifact = createArtifact(artifactItem);

			/*
			 * Determine if the artifact is already installed in an existing Maven repository
			 */
			boolean artifactResolved = resolveArtifactItem(artifact);

			/*
			 * Now that the file has been successfully downloaded and the
			 * checksum verification has passed (if required), lets copy
			 * the temporary file to the staging location
			 */
			final File artifactFile = getFullyQualifiedArtifactFilePath(artifactItem);

			// Only proceed with this artifact if it is not already installed or it is configured to be forced.
			if (!artifactResolved || (!artifactFile.exists() && (force || artifactItem.getForce()))) {

				if (artifactItem.getForce()) {
					getLog().debug(String.format("Artifact %s is flagged as a FORCED download", artifactItem.toString()));
				}

				// Download file from URL
				if (artifactItem.getDownloadUrl() != null) {
					URL downloadUrl;
					try {
						downloadUrl = new URL(artifactItem.getDownloadUrl());
					} catch (MalformedURLException e1) {
						throw new MojoExecutionException("Could not interpret URL " + artifactItem.getDownloadUrl(), e1);
					}

					final File tempDownloadFile;

					if (cachedDownloads.containsKey(downloadUrl)) {
						tempDownloadFile = cachedDownloads.get(downloadUrl);
						getLog().info(String.format("Artifact %s already downloaded from URL",artifactItem.getDownloadUrl()));
						getLog().debug("Using cached download: " + tempDownloadFile.getAbsolutePath());
					} else {
						// create a temporary download file
						try {
							tempDownloadFile = File.createTempFile(artifactItem.getLocalFile(), "." + getExtension(downloadUrl));
						} catch (IOException e) {
							throw new MojoExecutionException("Could not create temporary file", e);
						}

						getLog().info(
							String.format("Downloading artifact %s from URL %s", artifactItem.toString(),
							artifactItem.getDownloadUrl()
						));
						getLog().debug("Downloading artifact to temporary file: " + tempDownloadFile.getAbsolutePath());

						// vharseko@openam.org.ru
						String endPointUrl = downloadUrl.getProtocol() + "://" + downloadUrl.getAuthority();
						Repository repository = new Repository("additonal-configs", endPointUrl);
						Wagon wagon;
						try {
							wagon = wagonManager.getWagon(repository);
						} catch (WagonConfigurationException | UnsupportedProtocolException e) {
							throw new MojoExecutionException(String.format(
								"Could not initialize protocol \"%s\": %s",
								downloadUrl.getProtocol(),
								e.getMessage()
							), e);
						}
						if (getLog().isDebugEnabled()) {
							Debug debug = new Debug();
							wagon.addSessionListener(debug);
							wagon.addTransferListener(debug);
						}
						wagon.setTimeout(artifactItem.getTimeout());

						ProxyInfo proxyInfo = null;
						try {
							Settings settings = getSettingsBuilder().build(getSettingsBuildingRequest()).getEffectiveSettings();

							if (settings != null && settings.getActiveProxy() != null) {
								Proxy settingsProxy = settings.getActiveProxy();
								proxyInfo = new ProxyInfo();
								proxyInfo.setHost(settingsProxy.getHost());
								proxyInfo.setType(settingsProxy.getProtocol());
								proxyInfo.setPort(settingsProxy.getPort());
								proxyInfo.setNonProxyHosts(settingsProxy.getNonProxyHosts());
								proxyInfo.setUserName(settingsProxy.getUsername());
								proxyInfo.setPassword(settingsProxy.getPassword());
							}
						} catch (SettingsBuildingException e) {
							getLog().warn("Could not read Maven settings, skipping proxy configuration: " + e.getMessage());
							getLog().debug(e);
						}

						try {
						if (proxyInfo != null) {
							wagon.connect(repository, wagonManager.getAuthenticationInfo(repository.getId()), proxyInfo);
						} else {
							wagon.connect(repository, wagonManager.getAuthenticationInfo(repository.getId()));
						}

						wagon.get(downloadUrl.getPath().substring(1), tempDownloadFile);
						} catch (ConnectionException | TransferFailedException | ResourceDoesNotExistException | AuthorizationException | AuthenticationException e) {
							throw new MojoExecutionException(
								"Failed to download artifact " + artifactItem.toString() + ": " + e.getMessage(), e
							);
						}

						getLog().debug("Caching temporary file for later: " + tempDownloadFile);
						cachedDownloads.put(downloadUrl, tempDownloadFile);
					}

					/*
					 * Verify file checksum (if a checksum was defined).
					 * MojoFailureException exception will be thrown if
					 * verification fails.
					 *
					 * Note: In theory, there might be conflicting checksums
					 * configured for the same artifact; checksum
					 * verification may thus be done several times for a
					 * cached download.
					 */
					verifyArtifactItemChecksum(artifactItem, tempDownloadFile);

					/*
					 * If this artifact is not configured to extract a file,
					 * then simply copy the downloaded file to the target
					 * artifact file.
					 */

					if (!artifactItem.hasExtractFile()) {
						getLog().info(
							"Copying downloaded artifact file to staging path: " + artifactFile.getAbsolutePath()
						);
						try {
							FileUtils.copyFile(tempDownloadFile, artifactFile);
						} catch (IOException e) {
							throw new MojoExecutionException(
								"Failed to copy artifact " + artifactItem.toString() + " to staging path \"" +
								artifactFile.getAbsolutePath() + "\": " + e.getMessage(),
								e
							);
						}
					}

					/*
					 * If this artifact is configured to extract a file,
					 * then extract the file from the downloaded compressed
					 * file to the target artifact file
					 */
					else {
						getLog().info(
							"Extracting target file from downloaded compressed file: " + artifactItem.getExtractFile()
						);

						File tempOutputDir = FileUtils.createTempFile(tempDownloadFile.getName(), ".dir", null);
						tempOutputDir.mkdirs();
						File extractedFile = new File(tempOutputDir, artifactItem.getExtractFile());

						UnArchiver unarchiver;
						try {
							unarchiver = archiverManager.getUnArchiver(tempDownloadFile);
						} catch (NoSuchArchiverException e) {
							if (tempDownloadFile.getName().endsWith(".gz")) {
								try {
									unarchiver = archiverManager.getUnArchiver("gzip");
								} catch (NoSuchArchiverException e1) {
									throw new MojoExecutionException(
										"No gzip unarchiver available for: " + tempDownloadFile.getAbsolutePath(), e
									);
								}
								unarchiver.setDestFile(extractedFile);
							} else
								throw new MojoExecutionException(
									"No unarchiver available for archive type: " + tempDownloadFile.getAbsolutePath(), e
								);
						}

						// Ensure the path exists to write the file to
						File parentDirectory = artifactFile.getParentFile();
						if (parentDirectory != null && !parentDirectory.exists()) {
							artifactFile.getParentFile().mkdirs();
						}

						unarchiver.setSourceFile(tempDownloadFile);
						if (unarchiver.getDestFile() == null)
							unarchiver.setDestDirectory(tempOutputDir);
						unarchiver.extract();

						// If an archive entry was not found, then throw a Mojo exception
						if (extractedFile.isFile()) {
							try {
								FileUtils.copyFile(extractedFile, artifactFile);
							} catch (IOException e) {
								throw new MojoExecutionException(
									"Failed to copy \"" + extractedFile.getAbsolutePath() +
									"\" to \"" + artifactFile.getAbsolutePath() + "\": " +
									e.getMessage(), e
								);
							}
						} else if (extractedFile.isDirectory() && artifactItem.isRepack()) {
							Archiver archiver;
							try {
								archiver = archiverManager.getArchiver(artifactFile);
							} catch (NoSuchArchiverException e) {
								throw new MojoExecutionException(
									"No unarchiver available for archive type: " + artifactFile.getAbsolutePath(), e
								);
							}
							archiver.setDestFile(artifactFile);
							archiver.addFileSet(new DefaultFileSet(extractedFile));
							try {
								archiver.createArchive();
							} catch (ArchiverException | IOException e) {
								throw new MojoExecutionException(
									"Failed to create archive \"" + artifactFile.getAbsolutePath() + "\": " + e.getMessage(),
									e
								);
							}
						} else {
							// Checksum verification failed, throw error
							throw new MojoFailureException(
								"Could not find target artifact file to extract from downloaded resource:" + NEWLINE +
								"  groupId      : " + artifact.getGroupId() + NEWLINE +
								"  artifactId   : " + artifact.getArtifactId() + NEWLINE +
								"  version      : " + artifact.getVersion() + NEWLINE +
								"  extractFile  : " + artifactItem.getExtractFile() + NEWLINE +
								"  download URL : " + artifactItem.getDownloadUrl()
							);
						}

						getLog().info("Extracted target file to staging path: " + artifactFile.getAbsolutePath());
					}

					// update the artifact items local file property
					try {
						artifactItem.setLocalFile(artifactFile.getCanonicalPath());
					} catch (IOException e) {
						throw new MojoExecutionException(
							"Failed to get canonical path for \"" + artifactFile.getAbsolutePath() + "\": " + e.getMessage(),
							e
						);
					}

					getLog().info("External artifact downloaded and staged: " + artifactItem.toString());

					// if the acquired artifact listed in the project artifacts collection
					/*if(projectArtifacts.contains(artifact)) {
						getLog().info("FOUND ARTIFACT IN PROJECT: " + artifact.toString());
					}*/
				}
			} else {
				getLog().info(String.format(
					"External artifact %s resolved in existing repository; no download needed.",
					artifactItem.toString()
				));
			}
		}

		if (cachedDownloads.size() > 0) {
			getLog().info("Deleting temporary download files");
		}
		// We're done with the temporary files so lets delete them
		for (File tempDownloadFile : cachedDownloads.values()) {
			getLog().debug("Deleting file: " + tempDownloadFile.getAbsolutePath());
			tempDownloadFile.delete();
		}

		getLog().info("Finished resolving all external dependencies");
	}

	private String getExtension(URL downloadUrl) {
		String path = downloadUrl.getPath();
		if (path.endsWith(".tar.gz")) {
			return "tar.gz";
		}
		if (path.endsWith(".tar.bz2")) {
			return "tar.bz2";
		}
		return FileUtils.getExtension(path);
	}
}
