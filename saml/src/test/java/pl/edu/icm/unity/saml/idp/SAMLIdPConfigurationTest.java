package pl.edu.icm.unity.saml.idp;

/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

import xmlbeans.org.oasis.saml2.assertion.NameIDType;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SAMLIdPConfigurationTest
{

	@Test
	void shouldReturnCertificateWithLatestExpiration()
	{
		long now = System.currentTimeMillis();

		X509Certificate cert1 = cert(now - seconds(100), now + seconds(100));
		X509Certificate cert2 = cert(now - seconds(100), now + seconds(200));

		SAMLIdPConfiguration config = configuration(sp("entity", true, Set.of(cert1, cert2)));

		X509Certificate result = config.getEncryptionCertificateForRequester(requester("entity"));

		assertEquals(cert2, result);
	}

	@Test
	void shouldReturnNullWhenEncryptionDisabled()
	{
		long now = System.currentTimeMillis();
		X509Certificate validCert = cert(now - seconds(100), now + seconds(200));
		SAMLIdPConfiguration config = configuration(sp("entity", false, Set.of(validCert)));
		X509Certificate result = config.getEncryptionCertificateForRequester(requester("entity"));

		assertNull(result);
	}

	@Test
	void shouldIgnoreCertificatesNotYetValid()
	{
		long now = System.currentTimeMillis();

		X509Certificate futureCert = cert(now + seconds(100), now + seconds(200));
		X509Certificate validCert = cert(now - seconds(100), now + seconds(200));

		SAMLIdPConfiguration config = configuration(sp("entity", true, Set.of(futureCert, validCert)));

		X509Certificate result = config.getEncryptionCertificateForRequester(requester("entity"));

		assertEquals(validCert, result);
	}

	@Test
	void shouldReturnNullWhenNoValidCertificates()
	{
		long now = System.currentTimeMillis();

		X509Certificate expiredCert = cert(now + seconds(200), now + seconds(300));

		SAMLIdPConfiguration config = configuration(sp("entity", true, Set.of(expiredCert)));

		X509Certificate result = config.getEncryptionCertificateForRequester(requester("entity"));

		assertNull(result);
	}

	@Test
	void shouldReturnNullWhenServiceProviderNotFound()
	{
		SAMLIdPConfiguration config = configuration(sp("different-entity", true, Set.of(cert())));

		X509Certificate result = config.getEncryptionCertificateForRequester(requester("entity"));

		assertNull(result);
	}

	private long seconds(long s)
	{
		return s * 1000;
	}

	private NameIDType requester(String entityId)
	{
		NameIDType requester = mock(NameIDType.class);
		when(requester.getFormat()).thenReturn(null);
		when(requester.getStringValue()).thenReturn(entityId);
		return requester;
	}

	private SAMLIdPConfiguration configuration(TrustedServiceProvider sp)
	{
		TrustedServiceProviders providers = new TrustedServiceProviders(Set.of(sp));

		return SAMLIdPConfiguration.builder()
				.withIssuerURI("https://issuer")
				.withTrustedServiceProviders(providers)
				.build();
	}

	private X509Certificate cert()
	{
		return mock(X509Certificate.class);
	}

	private X509Certificate cert(long notBefore, long notAfter)
	{
		X509Certificate cert = mock(X509Certificate.class);
		when(cert.getNotBefore()).thenReturn(new Date(notBefore));
		when(cert.getNotAfter()).thenReturn(new Date(notAfter));
		return cert;
	}

	private TrustedServiceProvider sp(String entityId, boolean encrypt, Set<X509Certificate> certs)
	{
		return TrustedServiceProvider.builder()
				.withAllowedKey("sp1")
				.withEntityId(entityId)
				.withEncrypt(encrypt)
				.withReturnUrl("https://return")
				.withCertificates(certs)
				.build();
	}
}