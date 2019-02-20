/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * 
 * @author P.Piernik
 *
 */
public class TestPKICertificates extends DBIntegrationTestBase
{
	@Autowired
	private PKIManagement pkiMan;

	@Before
	public void clear() throws Exception
	{
		insecureServerMan.resetDatabase();
		for (NamedCertificate cert : pkiMan.getVolatileCertificates())
		{
			pkiMan.removeCertificate(cert.name);
		}
	}
	
	@Test
	public void shouldAddPersistedCert() throws Exception
	{
		pkiMan.addPersistedCertificate(new NamedCertificate("cert1", getX509Cert()));
		assertThat(pkiMan.getCertificate("cert1"), is(notNullValue()));
	}

	@Test
	public void shouldAddVolatileCert() throws Exception
	{
		pkiMan.addVolatileCertificate("cert1", getX509Cert());
		assertThat(pkiMan.getCertificate("cert1"), is(notNullValue()));
	}
	
	@Test
	public void shouldRemoveCert() throws Exception
	{
		pkiMan.addVolatileCertificate("cert1", getX509Cert());		
		pkiMan.addPersistedCertificate(new NamedCertificate("cert2", getX509Cert()));
		pkiMan.removeCertificate("cert1");
		pkiMan.removeCertificate("cert2");
		assertThat(pkiMan.getAllCertificateNames().size(), is(0));	
	}

	@Test
	public void shouldBlockAddingCertWithTheSameName() throws Exception
	{
		pkiMan.addVolatileCertificate("cert1", getX509Cert());

		Throwable exception = catchThrowable(
				() -> pkiMan.addPersistedCertificate(new NamedCertificate("cert1", getX509Cert())));
		assertExceptionType(exception, IllegalArgumentException.class);

		exception = catchThrowable(() -> pkiMan.addVolatileCertificate("cert1", getX509Cert()));
		assertExceptionType(exception, IllegalArgumentException.class);
	}
	
	@Test
	public void shouldListAllCertNames() throws Exception
	{
		pkiMan.addVolatileCertificate("cert1", getX509Cert());
		pkiMan.addVolatileCertificate("cert2", getX509Cert());
		pkiMan.addPersistedCertificate(new NamedCertificate("cert3", getX509Cert()));
		assertThat(pkiMan.getAllCertificateNames().size(), is(3));
		assertThat(pkiMan.getAllCertificateNames(), hasItems("cert1", "cert2", "cert3"));	
	}
	
	@Test
	public void shouldBlockAddingCertByUnprivilagedUser() throws Exception
	{
		setupPasswordAuthn();
		idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), CRED_REQ_PASS,
				EntityState.valid, false);
		setupUserContext("tuser", null);
		Throwable exception = catchThrowable(() ->  pkiMan.addPersistedCertificate(new NamedCertificate("cert1", getX509Cert())));
		assertExceptionType(exception, AuthorizationException.class);
		
	}
	
	private X509Certificate getX509Cert() throws IOException
	{
		String cert = "-----BEGIN CERTIFICATE-----\n"
				+ "MIIDbDCCAlSgAwIBAgIJAPm9oVHHE5w+MA0GCSqGSIb3DQEBBQUAMDoxCzAJBgNVBAYTAkVVMQ4w\n"
				+ "DAYDVQQKDAVVbml0eTEbMBkGA1UEAwwSVW5pdHkgTG93IFRydXN0IENBMB4XDTE2MDMyNDEzNTk0\n"
				+ "NFoXDTIxMDMyMzEzNTk0NFowQjELMAkGA1UEBhMCRVUxDzANBgNVBAcMBldhcnNhdzEOMAwGA1UE\n"
				+ "CgwFVW5pdHkxEjAQBgNVBAMMCWxvY2FsaG9zdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC\n"
				+ "ggEBAN6S2OegRYU11rM8w4uGwp4g3HToZ5dHjWIAeU3ngtpJ6yHMVwAqWwmPvOJTYg+Hed4tFwbr\n"
				+ "5+xt9kbZwNVjojbSFueoRjD6c5ycTkUjPA+ORKawflCxp8kUuggyr2abcp3+C+AuY76OcxttGBaR\n"
				+ "iyzkAca+I0LzcU3wdSl2/h5guEdUobyzBGmt8Kmm2lHqhj1iTPz6Chg5yFnsdCXTDUCVAoxB0Mf/\n"
				+ "bm0/1BDElCpA/pqE9ktGxFErUDtHr8K03eW3U5DZ48/3jKtSEPg443YfffDn81PDvvxRzebJP0T9\n"
				+ "qKzUYshMeZZr5kN9hwMwmpzLSYHYnL1mx+lhvZivRVkCAwEAAaNtMGswCQYDVR0TBAIwADARBglg\n"
				+ "hkgBhvhCAQEEBAMCBPAwCwYDVR0PBAQDAgTwMB0GA1UdDgQWBBTBrFcGtlUpRgdy9otQNqZZP/QN\n"
				+ "nTAfBgNVHSMEGDAWgBRm3s0c1pLap1K6RvAkez4McgfAFjANBgkqhkiG9w0BAQUFAAOCAQEAJN40\n"
				+ "mS+s2OU3t40h1ghTgLj3JNKtl5fucd3w8ZYawAHTgJGDw3l6eTUPwhDPGV6If1gNsIDzoGDUmb1i\n"
				+ "PwR27xzsP+iWZ5SO0q03tpH3lpYcFV1tnWJ8MrFYHgAvt/j7tRC1N9j2MMvRf9oSNYtlbj/pXq3f\n"
				+ "8MkLf4uGJK9SYTmd4EpCch5DiLYHHTkknmYLxMtdqwl2SRfpu2/Ch989Ha1yokQgkZfYJKFA3Bvg\n"
				+ "YgZc+Q6aeXeVUkBedE5SyDVpChGjdTN7iVgu9kL6l1aRGo1KnN78ce8JwhljjaAm9Fckt4OFMpuN\n"
				+ "hX879+8p8lQcNFBto0ILS+I4AjFR4Ljtlg==\n" + "-----END CERTIFICATE-----\n";

		return CertificateUtils.loadCertificate(new ByteArrayInputStream(cert.getBytes()), Encoding.PEM);
	}

	private void assertExceptionType(Throwable exception, Class<?> type)
	{
		Assertions.assertThat(exception).isNotNull().isInstanceOf(type);
	}
}
