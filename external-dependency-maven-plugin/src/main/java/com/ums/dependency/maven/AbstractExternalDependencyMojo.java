/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 **/

package com.ums.dependency.maven;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.install.AbstractInstallMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.io.DefaultSettingsReader;
import org.apache.maven.settings.io.DefaultSettingsWriter;
import org.apache.maven.settings.validation.DefaultSettingsValidator;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.digest.Md5Digester;
import org.codehaus.plexus.digest.Sha1Digester;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Base class for all goals in this plugin.
 *
 * @category Maven Plugin
 * @ThreadSafe
 */
public abstract class AbstractExternalDependencyMojo extends AbstractInstallMojo {

	/**
	 * Used to look up Artifacts in the remote repository.
	 *
	 * @component
	 */
	protected RepositorySystem repositorySystem;

	/**
	 * Collection of ArtifactItems to work on. (ArtifactItem contains groupId,
	 * artifactId, version, type, classifier, location, destFile, markerFile and
	 * overwrite.) See "Usage" and "Javadoc" for details.
	 *
	 * @parameter
	 * @required
	 */
	protected ArrayList<ArtifactItem> artifactItems;

	/**
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	protected MavenProject project;

	/**
	 * @parameter default-value="${user.home}/.m2/settings.xml"
	 * @required
	 */
	protected String userSettings;

	/**
	 * @parameter default-value="${env.M2_HOME}/conf/settings.xml"
	 * @required
	 */
	protected String globalSettings;

	/**
	 * @parameter default-value="${localRepository}"
	 * @required
	 * @readonly
	 */
	protected ArtifactRepository localRepository;

	/**
	 * @parameter default-value="${project.build.directory}"
	 */
	protected String stagingDirectory;

	/**
	 * Forces a download, maven install, maven deploy
	 *
	 * @parameter default-value="false"
	 */
	protected Boolean force = false;

	/**
	 * If this property is set to true, then the downloaded file's checksum will
	 * not be verified using the Sonatype artifact query by checksum validation
	 * routine.
	 *
	 * @parameter default-value="false"
	 */
	protected Boolean skipChecksumVerification = false;

	protected final String NEWLINE = System.getProperty("line.separator");
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// Super class lack a default value and requires it to be set manually
		super.localRepository = this.localRepository;
	}

	/**
	 * Create Maven Artifact object from ArtifactItem configuration descriptor
	 *
	 * @param item
	 * @return Artifact
	 */
	protected Artifact createArtifact(ArtifactItem item) {
		Artifact artifact = null;

		// create Maven artifact with a classifier
		artifact = repositorySystem.createArtifactWithClassifier(item.getGroupId(), item.getArtifactId(),
			item.getVersion(), item.getPackaging(), item.getClassifier());

		return artifact;
	}

	/**
	 * Generates a (temporary) POM file from the plugin configuration. It's the
	 * responsibility of the caller to delete the generated file when no longer
	 * needed.
	 *
	 * @return The path to the generated POM file, never <code>null</code>.
	 * @throws MojoExecutionException
	 *             If the POM file could not be generated.
	 */
	protected File generatePomFile(ArtifactItem artifact) throws MojoExecutionException {
		Model model = generateModel(artifact);

		Writer writer = null;
		try {
			File pomFile = File.createTempFile(artifact.getGroupId() + "." + artifact.getArtifactId(), ".pom");

			writer = WriterFactory.newXmlWriter(pomFile);
			new MavenXpp3Writer().write(writer, model);

			return pomFile;
		} catch (IOException e) {
			throw new MojoExecutionException("Error writing temporary POM file: " + e.getMessage(), e);
		} finally {
			IOUtil.close(writer);
		}
	}

	/**
	 * Generates a minimal model from the user-supplied artifact information.
	 *
	 * @return The generated model, never <code>null</code>.
	 */
	protected Model generateModel(ArtifactItem artifact) {
		Model model = new Model();
		model.setModelVersion("4.0.0");
		model.setGroupId(artifact.getGroupId());
		model.setArtifactId(artifact.getArtifactId());
		model.setVersion(artifact.getVersion());
		model.setPackaging(artifact.getPackaging());

		return model;
	}

	/**
	 * Resolves the file path and returns a file object instance for an artifact
	 * item
	 *
	 * @return File object for artifact item
	 */
	protected File getFullyQualifiedArtifactFilePath(ArtifactItem artifactItem) {
		String artifactStagingDirectory = artifactItem.getStagingDirectory();
		if (artifactStagingDirectory == null || artifactStagingDirectory.isEmpty()) {
			artifactStagingDirectory = stagingDirectory;
		}
		return new File(artifactStagingDirectory, artifactItem.getLocalFile());
	}

	/**
	 * Verifies a checksum for the specified file.
	 *
	 * @param targetFile
	 *            The path to the file from which the checksum is verified, must
	 *            not be <code>null</code>.
	 * @param digester
	 *            The checksum algorithm to use, must not be <code>null</code>.
	 * @throws MojoExecutionException
	 *             If the checksum could not be installed.
	 */
	protected boolean verifyChecksum(File targetFile, Digester digester, String checksum) throws MojoExecutionException {
		getLog().debug("Calculating " + digester.getAlgorithm() + " checksum for " + targetFile);
		try {
			String calculatedChecksum = digester.calc(targetFile);
			getLog().debug("Generated checksum : " + calculatedChecksum);
			getLog().debug("Expected checksum  : " + checksum);
			return (calculatedChecksum.equals(checksum));
		} catch (DigesterException e) {
			throw new MojoExecutionException("Failed to calculate " + digester.getAlgorithm() + " checksum for "
				+ targetFile, e);
		}
	}

	/**
	 * Validate artifact configured checksum against specified file.
	 *
	 * @param artifactItem
	 *            to validate checksum against
	 * @param targetFile
	 *            to validate checksum against
	 * @throws MojoExecutionException
	 * @throws MojoFailureException
	 * @throws IOException
	 */
	protected void verifyArtifactItemChecksum(ArtifactItem artifactItem, File targetFile) throws MojoExecutionException, MojoFailureException {

		// If a checksum was specified, we must verify the checksum against the downloaded file
		if (artifactItem.hasChecksum()) {
			getLog().info(String.format(
				"Verifying checksum on downloaded file %s: %s",
				targetFile.getName(),
				artifactItem.getChecksum()
			));

			// Perform MD5 checksum verification
			getLog().info("Testing for MD5 checksum on artifact " + artifactItem.toString());
			if (!verifyChecksum(targetFile, new Md5Digester(), artifactItem.getChecksum())) {
				getLog().info("Verification failed on MD5 checksum for file: " + targetFile.getAbsolutePath());
				getLog().info("Testing for SHA1 checksum on artifact: " + artifactItem.toString());

				// Did not pass MD5 checksum verification, now test SHA1 checksum
				if (!verifyChecksum(targetFile, new Sha1Digester(), artifactItem.getChecksum())) {
					// Checksum verification failed, throw error
					throw new MojoFailureException(
						"Both MD5 and SHA1 checksum verification failed for: " + NEWLINE +
						"  groupId    : " + artifactItem.getGroupId() + NEWLINE +
						"  artifactId : " + artifactItem.getArtifactId() + NEWLINE +
						"  version    : " + artifactItem.getVersion() + NEWLINE +
						"  checksum   : " + artifactItem.getChecksum() + NEWLINE +
						"  file       : " + targetFile.getAbsolutePath()
					);
				} else {
					getLog().info("Verification passed on SHA1 checksum for artifact: " + artifactItem.toString());
				}
			} else {
				getLog().info("Verification passed on MD5 checksum for artifact: " + artifactItem.toString());
			}
		}
	}

	/**
	 * Validate artifact configured extracted file checksum against specified
	 * file.
	 *
	 * @param artifactItem
	 *            to validate checksum against
	 * @param targetFile
	 *            to validate checksum against
	 * @throws MojoExecutionException
	 * @throws MojoFailureException
	 * @throws IOException
	 */
	protected void verifyArtifactItemExtractFileChecksum(ArtifactItem artifactItem, File targetFile) throws MojoExecutionException, MojoFailureException {

		// If a checksum was specified, we must verify the checksum against the extracted file
		if (artifactItem.hasExtractFileChecksum()) {
			getLog().info(String.format(
				"Verifying checksum on extracted file %s: %s",
				targetFile.getName(),
				artifactItem.getExtractFileChecksum()
			));

			// Perform MD5 checksum verification
			getLog().info("Testing for MD5 checksum on artifact: " + artifactItem.toString());
			if (!verifyChecksum(targetFile, new Md5Digester(), artifactItem.getExtractFileChecksum())) {
				getLog().info("Verification failed on MD5 checksum for extracted file: " + targetFile.getAbsolutePath());
				getLog().info("Testing for SHA1 checksum on artifact: " + artifactItem.toString());

				// Did not pass MD5 checksum verification, now test SHA1 checksum
				if (!verifyChecksum(targetFile, new Sha1Digester(), artifactItem.getExtractFileChecksum())) {
					// checksum verification failed, throw error
					throw new MojoFailureException(
						"Both MD5 and SHA1 checksum verification failed for: " + NEWLINE +
						"  groupId        : " + artifactItem.getGroupId() + NEWLINE +
						"  artifactId     : " + artifactItem.getArtifactId() + NEWLINE +
						"  version        : " + artifactItem.getVersion() + NEWLINE +
						"  checksum       : " + artifactItem.getExtractFileChecksum() + NEWLINE +
						"  extracted file : " + targetFile.getAbsolutePath());
				} else {
					getLog().info("Verification passed on SHA1 checksum for artifact: " + artifactItem.toString());
				}
			} else {
				getLog().info("Verification passed on MD5 checksum for artifact: " + artifactItem.toString());
			}
		}
	}

	/**
	 * Gets the text content of a named attribute from a <code>Node</code>
	 * or <code>null</code> if it doesn't exist.
	 * @param node the node to search
	 * @return The text content of the node or <code>null</code>
	 */

	private String getAttribute(Node node, String attributeName) {
		Node attributeNode = node.getAttributes().getNamedItem(attributeName);
		if (attributeNode != null) {
			return attributeNode.getTextContent();
		}
		else return null;
	}

	/**
	 * Checks if a named attribute exists for a give <code>Node</code>.
	 * @param node the node to search
	 * @return The result
	 */
	private boolean hasAttribute(Node node, String attributeName) {
		return getAttribute(node, attributeName) != null;
	}

	/**
	 * Validate downloaded file artifact checksum does not match another
	 * artifact's checksum that already exists in the central Maven repository.
	 * Using the search.maven.org REST API to perform a checksum lookup.
	 *
	 * @since 0.1
	 *
	 * @param artifactItem
	 *            to validate checksum against
	 * @param targetFile
	 *            to validate checksum against
	 * @throws MojoExecutionException
	 * @throws MojoFailureException
	 * @throws IOException
	 */
	protected void verifyArtifactItemChecksumByCentralLookup(ArtifactItem artifactItem, File targetFile)
		throws MojoExecutionException, MojoFailureException {
		// Skip this artifact checksum verification?
		if (skipChecksumVerification == true || artifactItem.getSkipChecksumVerification() == true) {
			return;
		}

		boolean artifactMismatch = false;
		StringBuilder detectedArtifacts = new StringBuilder();

		// Calculate SHA1 checksum
		digester.calculate(targetFile);
		String sha1Checksum = digester.getSha1();
		getLog().debug("Performing Central Repository lookup on artifact SHA1 checksum: " + sha1Checksum);

		// perform REST query against Central Repository checksum lookup API
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			getLog().error("Could not create XML Document parser: " + e1.getMessage());
			getLog().info("Skipping Central Repository checksum verification");
			getLog().debug(e1);
			return;
		}
		Document document;
		try {
			document = builder.parse(String.format("http://search.maven.org/solrsearch/select?q=1:\"%s\"&rows=20&wt=xml", sha1Checksum));
		} catch (SAXException e) {
			getLog().error("Could not parse Central Repository response: " + e.getMessage());
			getLog().info("Skipping Central Repository checksum verification");
			getLog().debug(e);
			return;
		} catch (IOException e) {
			getLog().error("Could not contact Central Repository: " + e.getMessage());
			getLog().info("Skipping Central Repository checksum verification");
			getLog().debug(e);
			return;
		}
		NodeList artifactList = document.getElementsByTagName("doc");

		// were any results returned?
		if (artifactList != null && artifactList.getLength() > 0) {

			int nodeCount = 0;
			NodeList resultNodes = document.getElementsByTagName("result");
			for (int i = 0; i < resultNodes.getLength(); i++) {
				Node node = resultNodes.item(i);
				if (hasAttribute(node, "name") && hasAttribute(node, "numFound")) {
					if (getAttribute(node, "name").equalsIgnoreCase("response")) {
						try {
							nodeCount = Integer.valueOf(getAttribute(node, "numFound"));
							break;
						} catch (NumberFormatException e) {
							nodeCount = 0;
						}
					}
				}
			}
			// If we can't interpret the results, use the list length (although it might be truncated)
			if (nodeCount == 0) {
				nodeCount = artifactList.getLength();
			}

			getLog().info(
				nodeCount + " existing artifacts found in Central Repository checksum lookup. Verifying artifact GAV.");

			/*
			 * Iterate over all the query returned artifact definitions and
			 * attempt to determine if any of the returned artifact GAV do
			 * no match the GAV of the attempted install artifact.
			 */
			for (int index = 0; index < artifactList.getLength(); index++) {
				Node artifactNode = artifactList.item(index);
				if (artifactNode.hasChildNodes()) {
					NodeList children = artifactNode.getChildNodes();
					for (int loop = 0; loop < children.getLength(); loop++) {
						Node artifactProperty = children.item(loop);

						// We only inspect the strings with "name" attribute
						if (!artifactProperty.getNodeName().equals("str") || !hasAttribute(artifactProperty, "name")) {
							continue;
						}

						String propertyName = getAttribute(artifactProperty, "name");
						// Append returned artifact property names to an output message string
						detectedArtifacts.append(
							"       " + propertyName + " : " + artifactProperty.getTextContent() + NEWLINE
						);

						if (propertyName.equalsIgnoreCase("a")) {
							/*
							 * Attempt to validate the returned artifact's
							 * ArtifactId against the target install artifact
							 */
							if (!artifactProperty.getTextContent().equalsIgnoreCase(artifactItem.getArtifactId())) {
								getLog().error(
									"Artifact id found in Central Repository lookup does not match: " +
									artifactProperty.getTextContent() + " != " + artifactItem.getArtifactId());
								artifactMismatch = true;
							}
						} else if (propertyName.equalsIgnoreCase("g")) {
							/*
							 * Attempt to validate the returned artifact's
							 * GroupId against the target install artifact
							 */
							if (!artifactProperty.getTextContent().equalsIgnoreCase(artifactItem.getGroupId())) {
								getLog().error(
									"Artifact group id found in Central Repository lookup does not match: " +
									 artifactProperty.getTextContent() + " != " + artifactItem.getGroupId());
								artifactMismatch = true;
							}
						} else if (propertyName.equalsIgnoreCase("v")) {
							/*
							 * Attempt to validate the returned artifact's
							 * Version against the target install artifact
							 */
							if (!artifactProperty.getTextContent().equalsIgnoreCase(artifactItem.getVersion())) {
								getLog().error(
									"Artifact version found in Central Repository lookup does not match: " +
									artifactProperty.getTextContent() + " != " + artifactItem.getVersion());
								artifactMismatch = true;
							}
						}
					}
				}

				detectedArtifacts.append(NEWLINE);
			}
		} else {
			getLog().debug(
				"No existing artifacts found in Central Repository checksum lookup. Continue with artifact installation.");
		}

		// Was a mismatch detected?
		if (artifactMismatch == true) {
			// Checksum verification failed, throw error
			throw new MojoFailureException(
				"Central Repository artifact checksum verification failed on artifact defined in POM:" + NEWLINE + NEWLINE +
				"       groupId    : " + artifactItem.getGroupId() + NEWLINE +
				"       artifactId : " + artifactItem.getArtifactId() + NEWLINE +
				"       version    : " + artifactItem.getVersion() + NEWLINE +
				"       checksum   : " + sha1Checksum + NEWLINE +
				"       file       : " + targetFile.getAbsolutePath() + NEWLINE + NEWLINE +
				"The following artifact(s) were detected using the same checksum:" + NEWLINE +
				detectedArtifacts.toString() + NEWLINE +
				"Please verify that the GAV defined on the target artifact is correct." + NEWLINE
			);
		}
	}

	/**
	 * Generates a default settings builder.
	 * @return
	 * 			The SettingsBuilder instance
	 */
	public SettingsBuilder getSettingsBuilder() {
		DefaultSettingsBuilder settingsBuilder = new DefaultSettingsBuilder();
		settingsBuilder.setSettingsReader(new DefaultSettingsReader());
		settingsBuilder.setSettingsValidator(new DefaultSettingsValidator());
		settingsBuilder.setSettingsWriter(new DefaultSettingsWriter());
		return settingsBuilder;
	}

	/**
	 * Generates a settings builder request from parameters.
	 * @return
	 * 			The SettingsBuilderRequest instance
	 */
	public SettingsBuildingRequest getSettingsBuildingRequest() {
		DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
		if (userSettings != null) {
			request.setUserSettingsFile(new File(userSettings));
		}
		if (globalSettings != null) {
			request.setGlobalSettingsFile(new File(globalSettings));
		}
		return request;
	}
}
