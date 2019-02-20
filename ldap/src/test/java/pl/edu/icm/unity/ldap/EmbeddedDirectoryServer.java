/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.schema.Schema;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.emi.security.authn.x509.impl.SocketFactoryCreator;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;

/**
 * Helper starting embedded directory server
 * 
 * @author K. Benedyczak
 */
public class EmbeddedDirectoryServer
{
	private InMemoryDirectoryServer ds;


	public InMemoryDirectoryServer startEmbeddedServer() throws Exception
	{
		InMemoryDirectoryServerConfig config =
				new InMemoryDirectoryServerConfig("dc=unity-example,dc=com");

		List<InMemoryListenerConfig> listenerConfigs = new ArrayList<>();
		
		BinaryCertChainValidator acceptAll = new BinaryCertChainValidator(true);
		KeystoreCredential credential = DBIntegrationTestBase.getDemoCredential();
		SSLServerSocketFactory serverSocketFactory = SocketFactoryCreator.getServerSocketFactory(credential, 
				acceptAll);
		SSLSocketFactory clientSocketFactory = SocketFactoryCreator.getSocketFactory(null, acceptAll);
		System.out.println(Arrays.toString(serverSocketFactory.getSupportedCipherSuites()));
		System.out.println(Arrays.toString(clientSocketFactory.getSupportedCipherSuites()));
		
		InMemoryListenerConfig sslListener = new InMemoryListenerConfig("SSL", InetAddress.getByName("localhost"), 
				0, serverSocketFactory, clientSocketFactory, null);
		InMemoryListenerConfig plainWithTlsListener = new InMemoryListenerConfig("plain", 
				InetAddress.getByName("localhost"), 0, null, null, clientSocketFactory);
		listenerConfigs.add(plainWithTlsListener);
		listenerConfigs.add(sslListener);
		config.setListenerConfigs(listenerConfigs);
		
		Schema def = Schema.getDefaultStandardSchema();
		Schema mini = Schema.getSchema("src/test/resources/nis-cut.ldif");
		Schema merged = Schema.mergeSchemas(mini, def);
		config.setSchema(merged);
		
		ds = new InMemoryDirectoryServer(config);
		ds.importFromLDIF(true, "src/test/resources/test-data.ldif");
		ds.startListening();
		return ds;
	}
	
	public LDAPConnection getPlainConnection() throws LDAPException
	{
		return ds.getConnection("plain");
	}

	public LDAPConnection getSSLConnection() throws LDAPException
	{
		return ds.getConnection("SSL");
	}

	
	public PKIManagement getPKIManagement4Client() throws KeyStoreException, IOException
	{
		KeystoreCertChainValidator regularValidator = DBIntegrationTestBase.getDemoValidator();
		KeystoreCertChainValidator emptyValidator = new KeystoreCertChainValidator(
				"src/test/resources/pki/empty.jks", 
				"the!empty".toCharArray(), "JKS", -1);
		
		return new PKIManagement()
		{
			@Override
			public Set<String> getValidatorNames() throws EngineException
			{
				return Collections.singleton("main");
			}
			
			@Override
			public X509CertChainValidatorExt getValidator(String name) throws EngineException
			{
				if (name.equals("REGULAR"))
					return regularValidator;
				if (name.equals("EMPTY"))
					return emptyValidator;
				throw new WrongArgumentException("No such validator " + name);
			}
			
			@Override
			public Set<String> getCredentialNames() throws EngineException
			{
				return null;
			}
			@Override
			public X509Credential getCredential(String name) throws EngineException
			{
				return null;
			}

			@Override
			public IAuthnAndTrustConfiguration getMainAuthnAndTrust()
			{
				return null;
			}

			@Override
			public Set<String> getAllCertificateNames() throws EngineException
			{
				return null;
			}

			@Override
			public NamedCertificate getCertificate(String name) throws EngineException
			{
				return null;
			}


			@Override
			public void addVolatileCertificate(String name, X509Certificate updated)
					throws EngineException
			{
			}


			@Override
			public void addPersistedCertificate(NamedCertificate toAdd) throws EngineException
			{

				
			}

			@Override
			public List<NamedCertificate> getPersistedCertificates() throws EngineException
			{
				return null;
			}

			@Override
			public void loadCertificatesFromConfigFile()
			{
				
			}

			@Override
			public List<NamedCertificate> getVolatileCertificates() throws EngineException
			{
				return null;
			}

			@Override
			public void removeCertificate(String toRemove) throws EngineException
			{
				
				
			}

			@Override
			public void updateCertificate(NamedCertificate toUpdate) throws EngineException
			{
				
			}
		};
	}
}
