package com.ums.dependency.maven.archiver;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;

public class TarGZipArchiver extends TarArchiver
{
    public TarGZipArchiver() throws ArchiverException
    {
        this.setupCompressionMethod();
    }

    private void setupCompressionMethod() throws ArchiverException
    {

        TarCompressionMethod compression = TarCompressionMethod.gzip;
        this.setCompression(compression);
    }

}