package com.universalmediaserver.external.archiver;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;

/**
 * The Class TarGZipArchiver.
 */
public class TarGZipArchiver extends TarArchiver {

	/**
	 * TarGZipArchiver constructor
	 *
	 * @throws ArchiverException ArchiverException
	 */
	public TarGZipArchiver() throws ArchiverException {
		this.setupCompressionMethod();
	}

	/**
	 * Setup compression method.
	 *
	 * @throws ArchiverException the archiver exception
	 */
	private void setupCompressionMethod() throws ArchiverException {

		TarCompressionMethod compression = TarCompressionMethod.gzip;
		this.setCompression(compression);
	}

}