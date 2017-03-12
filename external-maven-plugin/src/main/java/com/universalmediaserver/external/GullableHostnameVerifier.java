package com.universalmediaserver.external;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * This is a {@link HostnameVerifier} that trusts any host.
 */
public class GullableHostnameVerifier implements HostnameVerifier {

	@Override
	public boolean verify(String hostname, SSLSession session) {
		return true;
	}

}
