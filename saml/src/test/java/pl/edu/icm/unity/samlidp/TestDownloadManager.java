package pl.edu.icm.unity.samlidp;

import static pl.edu.icm.unity.saml.SAMLProperties.METADATA_ISSUER_CERT;
import static pl.edu.icm.unity.saml.SAMLProperties.METADATA_REFRESH;
import static pl.edu.icm.unity.saml.SAMLProperties.METADATA_SIGNATURE;
import static pl.edu.icm.unity.saml.SAMLProperties.METADATA_URL;
import static pl.edu.icm.unity.saml.SAMLProperties.PUBLISH_METADATA;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.ALLOWED_SP_CERTIFICATE;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.ALLOWED_SP_ENCRYPT;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.ALLOWED_SP_ENTITY;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.ALLOWED_SP_LOGO;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.ALLOWED_SP_NAME;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.ALLOWED_SP_PREFIX;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.ALLOWED_SP_RETURN_URL;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.CREDENTIAL;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.DEFAULT_GROUP;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.GROUP;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.ISSUER_URI;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.P;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.SPMETA_PREFIX;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Properties;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.SAMLIDPProperties;
import pl.edu.icm.unity.saml.metadata.cfg.MetaDownloadManager;
import pl.edu.icm.unity.saml.metadata.cfg.MetaToIDPConfigConverter;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;

public class TestDownloadManager extends DBIntegrationTestBase
{
	@Autowired
	private ExecutorsService executorsService;

	@Autowired
	private UnityServerConfiguration mainConfig;

	@Autowired
	private PKIManagement pkiManagement;

	@Autowired
	private MetaDownloadManager downloadManager;

	@Test
	public void testDownload() throws IOException, EngineException, InterruptedException
	{

		Properties p = new Properties();
		p.setProperty(P + CREDENTIAL, "MAIN");
		p.setProperty(P + PUBLISH_METADATA, "false");
		p.setProperty(P + ISSUER_URI, "me");
		p.setProperty(P + GROUP, "group");
		p.setProperty(P + DEFAULT_GROUP, "group");

		p.setProperty(P + SPMETA_PREFIX + "1." + METADATA_URL,
				"http://metadata.aai.switch.ch/metadata.switchaai.xml");
		p.setProperty(P + SPMETA_PREFIX + "1." + METADATA_SIGNATURE, "require");
		X509Certificate cert = CertificateUtils.loadCertificate(new ByteArrayInputStream(
				CERT.getBytes()), Encoding.PEM);
		pkiManagement.addCertificate("issuerCert", cert);
		p.setProperty(P + SPMETA_PREFIX + "1." + METADATA_ISSUER_CERT, "issuerCert");
		p.setProperty(P + METADATA_REFRESH, "3600");

		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_ENTITY,
				"https://support.hes-so.ch/shibboleth");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_RETURN_URL, "URL");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_NAME, "Name");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_LOGO, "http://example.com");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_ENCRYPT, "true");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_CERTIFICATE, "MAIN");

		SAMLIDPProperties configuration = new SAMLIDPProperties(p, pkiManagement);

		ArrayList<RemoteMetaManager> mans = new ArrayList<RemoteMetaManager>();

		for (int i = 0; i < 15; i++)
		{

			RemoteMetaManager manager = new RemoteMetaManager(configuration,
					mainConfig, executorsService, pkiManagement,
					new MetaToIDPConfigConverter(pkiManagement),
					downloadManager, SAMLIDPProperties.SPMETA_PREFIX);
			mans.add(manager);

		}

		int i = 0;
		for (RemoteMetaManager m : mans)
		{	i++;
			Thread.sleep(500);
			m.start();
			System.out.println("Start "+ i +"\n\n");
			
		} 
		
		Thread.sleep(100000);
		

	}

	private static final String CERT = "-----BEGIN CERTIFICATE-----\nMIIEJjCCAw6gAwIBAgISSWITCHaaiMetadataSig2014MA0GCSqGSIb3DQEBBQUAMEYxCzAJBgNV"
			+ "BAYTAkNIMQ8wDQYDVQQKEwZTV0lUQ0gxJjAkBgNVBAMTHVNXSVRDSGFhaSBNZXRhZGF0YSBTaWdu"
			+ "aW5nIENBMB4XDTE0MDQwMzA3MDAwMFoXDTE2MDUwMzA2NDQ1OVowQjELMAkGA1UEBhMCQ0gxDzAN"
			+ "BgNVBAoTBlNXSVRDSDEiMCAGA1UEAxMZU1dJVENIYWFpIE1ldGFkYXRhIFNpZ25lcjCCASIwDQYJ"
			+ "KoZIhvcNAQEBBQADggEPADCCAQoCggEBAIJRlIwbBV1lsgsZ+l7N4YRijr2Cm9uy14EXSOBb1KqA"
			+ "fve/20/qFwAsWuWcqkU/v94Q6RN086X6tYdm+jNrqOVUcjVxiyVxieye98Hgyq0d8wYCllVQMdJv"
			+ "Hg7mJz+1mSxCvhFz9pJ3xwjgzTPtNsVmIk3l6ZSAHsN3PDPxxdjDbyqpJbdHAI8S4HW33mDb2BAo"
			+ "/mTrPG2wqY+xo8xf7QXFGyeGvU58fs/jvnD3s+XN4NL3qh4QocK4uiIo2jwsxo5auFIPq8YM0YeL"
			+ "2H2sk5ZO6YQttw6/7+ib/oJquyd1DcqWTUcgZTjqp4PDjJApHC2PnOUmRpD08rzQFPmauwkCAwEA"
			+ "AaOCARAwggEMMA4GA1UdDwEB/wQEAwIHgDAdBgNVHQ4EFgQU2XfrpZMcl7uByGsnJ+L8V8r4Lgsw"
			+ "HwYDVR0jBBgwFoAUkKnCDUaLZTU5RGduPD4q0qEnBbYwTAYDVR0fBEUwQzBBoD+gPYY7aHR0cDov"
			+ "L2NybC5hYWkuc3dpdGNoLmNoL1NXSVRDSGFhaU1ldGFkYXRhU2lnbmluZ0NBMjAxMS5jcmwwVgYI"
			+ "KwYBBQUHAQEESjBIMEYGCCsGAQUFBzAChjpodHRwOi8vY2EuYWFpLnN3aXRjaC5jaC9TV0lUQ0hh"
			+ "YWlNZXRhZGF0YVNpZ25pbmdDQTIwMTEuY3J0MBQGA1UdIAQNMAswCQYHYIV0AQIGBzANBgkqhkiG"
			+ "9w0BAQUFAAOCAQEAjEmjDbK9guQ0uXG3oLy368qCDl4swEZa3uIrmFJ/VJNNPSv2A7kvRpW2Od/p"
			+ "I+VjOJueYfJrSu+zeuB9Epem+2/Pvbjssq4ZJpj+91RUtlmA/MPNqRu0/PxrDBpHZ5HrXqH6NSI1"
			+ "DwZeIW5xEp4m3oYFptb14u9vP9UWUY8OFU2uhJF3FZ9AtzUHWAcIXg0evaw2fId6h+lOYVjl2hG9"
			+ "EP7pnM2z+s8qGPZAz/YjHM7I4/ToPpGvJ/YAnhJZ4MYzflqJxp/Ol1hpfPMeLOBL4G0FPZjrpxHZ"
			+ "56aNfU0OhQHYwveA73avWZ4T2NVo4Jipa5KfQFg5YYxnq6UWMciZAw=="
			+ "\n-----END CERTIFICATE-----";
}
