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

	@Test
	void shouldReturnCertificateWithLatestExpiration()
	{
		X509Certificate cert1 = mock(X509Certificate.class);
		X509Certificate cert2 = mock(X509Certificate.class);

		Date now = new Date();

		when(cert1.getNotBefore()).thenReturn(new Date(now.getTime() - 10000));
		when(cert1.getNotAfter()).thenReturn(new Date(now.getTime() + 10000));

		when(cert2.getNotBefore()).thenReturn(new Date(now.getTime() - 10000));
		when(cert2.getNotAfter()).thenReturn(new Date(now.getTime() + 20000));

		TrustedServiceProvider sp = TrustedServiceProvider.builder()
				.withAllowedKey("sp1")
				.withEntityId("entity")
				.withEncrypt(true)
				.withReturnUrl("https://return")
				.withCertificates(Set.of(cert1, cert2))
				.build();

		SAMLIdPConfiguration config = configuration(sp);

		X509Certificate result = config.getEncryptionCertificateForRequester(requester("entity"));

		assertEquals(cert2, result);
	}

	@Test
	void shouldReturnNullWhenEncryptionDisabled()
	{
		TrustedServiceProvider sp = TrustedServiceProvider.builder()
				.withAllowedKey("sp1")
				.withEntityId("entity")
				.withEncrypt(false)
				.withReturnUrl("https://return")
				.build();

		SAMLIdPConfiguration config = configuration(sp);

		X509Certificate result = config.getEncryptionCertificateForRequester(requester("entity"));

		assertNull(result);
	}

	@Test
	void shouldIgnoreCertificatesNotYetValid()
	{
		X509Certificate futureCert = mock(X509Certificate.class);
		X509Certificate validCert = mock(X509Certificate.class);

		Date now = new Date();

		when(futureCert.getNotBefore()).thenReturn(new Date(now.getTime() + 100000));

		when(validCert.getNotBefore()).thenReturn(new Date(now.getTime() - 1000));
		when(validCert.getNotAfter()).thenReturn(new Date(now.getTime() + 200000));

		TrustedServiceProvider sp = TrustedServiceProvider.builder()
				.withAllowedKey("sp1")
				.withEntityId("entity")
				.withEncrypt(true)
				.withReturnUrl("https://return")
				.withCertificates(Set.of(futureCert, validCert))
				.build();

		SAMLIdPConfiguration config = configuration(sp);

		X509Certificate result = config.getEncryptionCertificateForRequester(requester("entity"));

		assertEquals(validCert, result);
	}

	@Test
	void shouldReturnNullWhenNoValidCertificates()
	{
		X509Certificate expiredCert = mock(X509Certificate.class);

		Date now = new Date();

		when(expiredCert.getNotBefore()).thenReturn(new Date(now.getTime() + 20000));
		when(expiredCert.getNotAfter()).thenReturn(new Date(now.getTime() - 10000));

		TrustedServiceProvider sp = TrustedServiceProvider.builder()
				.withAllowedKey("sp1")
				.withEntityId("entity")
				.withEncrypt(true)
				.withReturnUrl("https://return")
				.withCertificates(Set.of(expiredCert))
				.build();

		SAMLIdPConfiguration config = configuration(sp);

		X509Certificate result = config.getEncryptionCertificateForRequester(requester("entity"));

		assertNull(result);
	}

	@Test
	void shouldReturnNullWhenServiceProviderNotFound()
	{
		TrustedServiceProvider sp = TrustedServiceProvider.builder()
				.withAllowedKey("sp1")
				.withEntityId("different-entity")
				.withEncrypt(true)
				.withReturnUrl("https://return")
				.withCertificates(Set.of(mock(X509Certificate.class)))
				.build();

		SAMLIdPConfiguration config = configuration(sp);

		X509Certificate result = config.getEncryptionCertificateForRequester(requester("entity"));

		assertNull(result);
	}
}