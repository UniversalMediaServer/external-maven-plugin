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
import org.codehaus.plexus.util.StringUtils;

/**
 * ArtifactItem represents information specified in the plugin configuration
 * section for each artifact.
 *
 * Please note:
 * This class is not at Mojo and variables are not parsed as such. The
 * mojo annotations are therefore without any effect and defaults must
 * be initialized like in any Java class. Setters are called after
 * initialization overwriting the defaults.
 *
 * The annotations is kept to improve readability though
 */
public class ArtifactItem {

	/**
	 * Group Id of Artifact.
	 *
	 * @parameter
	 * @required
	 */
	private String groupId;

	/**
	 * Name of Artifact.
	 *
	 * @parameter
	 * @required
	 */
	private String artifactId;

	/**
	 * Version of Artifact.
	 *
	 * @parameter
	 */
	private String version = null;

	/**
	 * Classifier for Artifact (tests, sources, etc).
	 *
	 * @parameter
	 */
	private String classifier;

	/**
	 * Local file to download artifact to. Location file to install artifact
	 * from.
	 *
	 * @parameter default-value="{artifactId}-{version}.{packaging}"
	 */
	private String localFile = "{artifactId}-{version}-{classifier}.{packaging}";

	/**
	 * Folder to download the artifact to.
	 *
	 * @parameter
	 */
	private String stagingDirectory;

	/**
	 * URL to download the artifact from.
	 *
	 * @parameter
	 */
	private String downloadUrl;

	/**
	 * Timeout in milliseconds allowed for artifact download
	 *
	 * @parameter
	 */
	private Integer timeout;

	/**
	 * Packaging type of the artifact to be installed.
	 *
	 * @parameter default-value="jar"
	 */
	private String packaging = "jar";

	/**
	 * Installs the artifact into the local maven repository.
	 *
	 * @parameter default-value="true"
	 */
	private boolean install = true;

	/**
	 * Deploys the artifact to a remote maven repository.
	 *
	 * @parameter default-value="true"
	 */
	private boolean deploy = true;

	/**
	 * Forces a download, maven install, maven deploy.
	 *
	 * @parameter default-value="false"
	 */
	private boolean force = false;

	/**
	 * Location of an existing POM file to be installed alongside the main
	 * artifact, given by the {@link #file} parameter.
	 *
	 * @parameter
	 */
	private File pomFile;

	/**
	 * Generate a minimal POM for the artifact if none is supplied via the
	 * parameter {@link #pomFile}. Defaults to <code>true</code> if there is no
	 * existing POM in the local repository yet.
	 *
	 * @parameter default-value="true"
	 */
	private boolean generatePom = true;

	/**
	 * Flag whether to create checksums (MD5, SHA-1) or not.
	 *
	 * @parameter
	 */
	private Boolean createChecksum = null;

	/**
	 * If this property is set to true, the downloaded file's checksum will be
	 * verified with a query against Maven Central Repository to make sure the
	 * artifact isn't already there.
	 *
	 * @parameter default-value="false"
	 */
	private boolean centralChecksumVerification = false;

	/**
	 * Checksum for Artifact.
	 *
	 * @parameter
	 */
	private String checksum;

	/**
	 * File name to extract from downloaded ZIP file.
	 *
	 * @parameter
	 */
	private String extractFile;

	/**
	 * File checksum from file that was extracted from downloaded ZIP file.
	 *
	 * @parameter
	 */
	private String extractFileChecksum;

	/**
	 * In case you need to repack a directory as a new artifact
	 *
	 * @parameter default-value="false"
	 */
	private boolean repack = false;

	/**
	 * default constructor.
	 */
	public ArtifactItem() {
		// default constructor
	}

	/**
	 * alternate constructor.
	 *
	 * @param artifact
	 *            Artifact
	 */
	public ArtifactItem(final Artifact artifact) {
		this.setArtifactId(artifact.getArtifactId());
		this.setClassifier(artifact.getClassifier());
		this.setGroupId(artifact.getGroupId());
		this.setPackaging(artifact.getType());
		this.setVersion(artifact.getVersion());
	}

	/**
	 * filter empty strings.
	 *
	 * @param in
	 *            input string to test
	 * @return if string was empty as null is returned
	 */
	private String filterEmptyString(final String in) {
		if (in == null || in.equals("")) {
			return null;
		} else {
			return in;
		}
	}

	/**
	 * @return Returns the artifactId.
	 */
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * The artifactId to set.
	 *
	 * @param artifact
	 *            item to set
	 */
	public void setArtifactId(final String artifact) {
		this.artifactId = filterEmptyString(artifact);
	}

	/**
	 * @return Returns the groupId.
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * @param groupId
	 *            The groupId to set.
	 */
	public void setGroupId(final String groupId) {
		this.groupId = filterEmptyString(groupId);
	}

	/**
	 * @return Returns the type.
	 */
	public String getType() {
		return getPackaging();
	}

	/**
	 * @return Returns the version.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @param version
	 *            The version to set.
	 */
	public void setVersion(final String version) {
		this.version = filterEmptyString(version);
	}

	/**
	 * @return Classifier.
	 */
	public String getClassifier() {
		return classifier;
	}

	/**
	 * @param classifier
	 *            Classifier.
	 */
	public void setClassifier(final String classifier) {
		this.classifier = filterEmptyString(classifier);
	}

	/**
	 * returns a string representations of the artifact item.
	 *
	 * @return result string
	 */
	public String toString() {
		if (this.classifier == null) {
			return groupId + ":" + artifactId + ":" + StringUtils.defaultString(version, "?") + ":" + packaging;
		} else {
			return groupId + ":" + artifactId + ":" + classifier + ":" + StringUtils.defaultString(version, "?") + ":"
				+ packaging;
		}
	}

	/**
	 * @return Returns the location.
	 */
	public String getLocalFile() {
		return replaceTokens(localFile);
	}

	/**
	 * @param localFile
	 *            The localFile to set.
	 */
	public void setLocalFile(final String localFile) {
		this.localFile = filterEmptyString(localFile);
	}

	/**
	 * @return Returns the stagingDirectory.
	 */
	public String getStagingDirectory() {
		return replaceTokens(stagingDirectory);
	}

	/**
	 * @param stagingDirectory
	 *            The stagingDirectory to set.
	 */
	public void setStagingDirectory(final String stagingDirectory) {
		this.stagingDirectory = filterEmptyString(stagingDirectory);
	}

	/**
	 * @return Returns the source URL to download the artifact.
	 */
	public String getDownloadUrl() {
		return replaceTokens(downloadUrl);
	}

	/**
	 * @param downloadUrl
	 *            Set the URL to download the artifact from.
	 */
	public void setDownloadUrl(final String downloadUrl) {
		this.downloadUrl = filterEmptyString(downloadUrl);
	}

	/**
	 * @return Returns the timeout in millis allowed for artifact download.
	 */
	public int getTimeout() {
		return timeout == null || timeout <= 0 ? 5000 : timeout;
	}

	/**
	 * @param timeout
	 *            Set the timeout in millis allowed for artifact download.
	 */
	public void setTimeout(final Integer timeout) {
		this.timeout = timeout;
	}

	/**
	 * @return Packaging.
	 */
	public String getPackaging() {
		return packaging;
	}

	/**
	 * @param packaging
	 *            Packaging.
	 */
	public void setPackaging(final String packaging) {
		this.packaging = filterEmptyString(packaging);
	}

	/**
	 * @return Force.
	 */
	public boolean getForce() {
		return force;
	}

	/**
	 * @param force
	 *            Force.
	 */
	public void setForce(final boolean force) {
		this.force = force;
	}

	/**
	 * @return Install.
	 */
	public boolean getInstall() {
		return install;
	}

	/**
	 * @param install
	 *            Install.
	 */
	public void setInstall(final boolean install) {
		this.install = install;
	}

	/**
	 * @return Deploy.
	 */
	public boolean getDeploy() {
		return deploy;
	}

	/**
	 * @param deploy
	 *            Deploy.
	 */
	public void setDeploy(final boolean deploy) {
		this.deploy = deploy;
	}

	/**
	 * @return PomFile.
	 */
	public File getPomFile() {
		return pomFile;
	}

	/**
	 * @param pomFile
	 *            PomFile.
	 */
	public void setPomFile(final File pomFile) {
		this.pomFile = pomFile;
	}

	/**
	 * @return GeneratePom.
	 */
	public boolean getGeneratePom() {
		return generatePom;
	}

	/**
	 * @param generatePom
	 *            GeneratePom.
	 */
	public void setGeneratePom(final boolean generatePom) {
		this.generatePom = generatePom;
	}

	/**
	 * @return CreateChecksum.
	 */
	public Boolean getCreateChecksum() {
		return createChecksum;
	}

	/**
	 * @param createChecksum
	 *            CreateChecksum.
	 */
	public void setCreateChecksum(final boolean createChecksum) {
		this.createChecksum = createChecksum;
	}

	/**
	 * @return Checksum.
	 */
	public String getChecksum() {
		return checksum;
	}

	/**
	 * @return true is a checksum was defined.
	 */
	public boolean hasChecksum() {
		return checksum != null && !checksum.isEmpty();
	}

	/**
	 * @return true if a checksum was defined for an extracted file.
	 */
	public boolean hasExtractFileChecksum() {
		return hasChecksum() && extractFileChecksum != null && !extractFileChecksum.isEmpty();
	}

	/**
	 * @return Extracted File Checksum.
	 */
	public String getExtractFileChecksum() {
		return extractFileChecksum;
	}

	public void setExtractFileChecksum(final String extractFileChecksum) {
		this.extractFileChecksum = filterEmptyString(extractFileChecksum);
	}

	/**
	 * @param checksum
	 *            Checksum
	 */
	public void setChecksum(final String checksum) {
		this.checksum = filterEmptyString(checksum);
	}

	/**
	 * @return centralChecksumVerification.
	 */
	public boolean getCentralChecksumVerification() {
		return centralChecksumVerification;
	}

	/**
	 * @param centralChecksumVerification
	 *            centralChecksumVerification.
	 */
	public void setCentralChecksumVerification(final boolean centralChecksumVerification) {
		this.centralChecksumVerification = centralChecksumVerification;
	}

	/**
	 * @return ExtractFile.
	 */
	public String getExtractFile() {
		return replaceTokens(extractFile);
	}

	/**
	 * @return true if an extractFile was defined.
	 */
	public boolean hasExtractFile() {
		return extractFile != null && !extractFile.isEmpty();
	}

	/**
	 * @param extractFile
	 *            ExtractFile
	 */
	public void setExtractFile(final String extractFile) {
		this.extractFile = filterEmptyString(extractFile);
	}

	/**
	 * replace parameterized tokens in string.
	 *
	 * @param source
	 *            source string to replace tokens in
	 * @return parameterized string
	 */
	private String replaceTokens(final String source) {
		String target = source;
		if (target == null) {
			return null;
		}

		if (target.isEmpty()) {
			return target;
		}

		// replace all tokens
		if (getGroupId() != null) {
			target = target.replace("{groupId}", getGroupId());
		}

		if (getArtifactId() != null) {
			target = target.replace("{artifactId}", getArtifactId());
		}

		if (getVersion() != null) {
			target = target.replace("{version}", getVersion());
		}

		if (getVersion() != null) {
			target = target.replace("{_version}", getVersion().replace(".", "_"));
		}

		if (getPackaging() != null) {
			target = target.replace("{packaging}", getPackaging());
		}

		if (getClassifier() != null) {
			target = target.replace("{classifier}", getClassifier());
		} else {
			target = target.replace("-{classifier}", "");
		}

		if (getType() != null) {
			target = target.replace("{type}", getType());
		}

		return target;
	}

	public boolean isRepack() {
		return repack;
	}

	public void setRepack(boolean repack) {
		this.repack = repack;
	}

}
