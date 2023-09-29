/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;

import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.security.canl.SSLContextCreator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

/**
 * Low level utility methods useful for secured Jetty connectors
 */
class JettyConnectorUtils
{
	static SslContextFactory.Server createJettyContextFactory(X509CertChainValidator validator,
			X509Credential credential, Logger log) throws NoSuchAlgorithmException, 
			NoSuchProviderException, KeyManagementException
	{
		SslContextFactory.Server ret = new SslContextFactory.Server();
		String protocol = "TLS"; 
		ret.setSslContext(SSLContextCreator.createSSLContext(credential, validator, protocol, 
				"Jetty HTTP Server", log, ServerHostnameCheckingMode.NONE));
		return ret;
	}
	
	
	static void reloadCredential(SslContextFactory.Server contextFactory, X509Credential newCredential, 
			X509CertChainValidator validator, Logger log) throws Exception 
	{
		String protocol = "TLS";
		contextFactory.setSslContext(SSLContextCreator.createSSLContext(newCredential, validator, protocol, 
				"Jetty HTTP Server", log, ServerHostnameCheckingMode.NONE));
		contextFactory.reload(scf->{});	
	}

	static void logConnection(Socket socket, Logger log) 
	{
		InetSocketAddress peer = (InetSocketAddress)socket.getRemoteSocketAddress();
		if(log.isDebugEnabled() && peer != null && peer.getAddress() != null)
		{
			String hostAddress = peer.getAddress().getHostAddress();
			log.debug("Connection attempt from {}", hostAddress);
			if (socket instanceof SSLSocket)
			{
				SSLSocket ssl = (SSLSocket) socket;
				ssl.addHandshakeCompletedListener(hce ->  
				{
					try 
					{
						X509Certificate[] peerCert = CertificateUtils.convertToX509Chain(
								hce.getPeerCertificates());
						String msg = X500NameUtils.getReadableForm(peerCert[0].getSubjectX500Principal());
						log.debug("SSL connection with {}, connected from {} was established.",
								msg, hostAddress);
					} catch (SSLPeerUnverifiedException spe) 
					{
						log.debug("An identity of the peer connecting from {} was not established on TLS layer",
								hostAddress);
					}
				});
			}
		}
	}
}
