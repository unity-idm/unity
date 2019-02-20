/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static pl.edu.icm.unity.saml.SamlProperties.METADATA_ISSUER_CERT;
import static pl.edu.icm.unity.saml.SamlProperties.METADATA_SIGNATURE;
import static pl.edu.icm.unity.saml.SamlProperties.METADATA_URL;
import static pl.edu.icm.unity.saml.SamlProperties.PUBLISH_METADATA;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.CREDENTIAL;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDPMETA_PREFIX;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDPMETA_REGISTRATION_FORM;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDPMETA_TRANSLATION_PROFILE;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_ADDRESS;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_BINDING;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_CERTIFICATE;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_CERTIFICATES;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_GROUP_MEMBERSHIP_ATTRIBUTE;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_ID;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_LOGO;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_NAME;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_PREFIX;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_REQUESTED_NAME_FORMAT;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_SIGN_REQUEST;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.P;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.REQUESTER_ID;
import static pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties.REGISTRATION_FORM;
import static pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties.TRANSLATION_PROFILE;

import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import java.util.Properties;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.metadata.srv.RemoteMetadataService;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;

public class TestSpCfgFromMeta extends DBIntegrationTestBase
{
	@Autowired
	private RemoteMetadataService metadataService;
	
	@Autowired
	@Qualifier("insecure")
	private  PKIManagement pkiManagement;
	
	@Autowired
	private UnityMessageSource msg;
	
	@Before
	public void reset()
	{
		metadataService.reset();
	}
	
	@Test
	public void testConfigureSPFromMetadata() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(P+CREDENTIAL, "MAIN");
		p.setProperty(P+REQUESTER_ID, "me");
		p.setProperty(P+PUBLISH_METADATA, "false");

		p.setProperty(P+IDPMETA_PREFIX+"1." + METADATA_URL, "file:src/test/resources/metadata.switchaai.xml");
		p.setProperty(P+IDPMETA_PREFIX+"1." + IDPMETA_TRANSLATION_PROFILE, "metaTrP");
		p.setProperty(P+IDPMETA_PREFIX+"1." + IDPMETA_REGISTRATION_FORM, "metaRegForm");
		p.setProperty(P+IDPMETA_PREFIX+"1." + METADATA_SIGNATURE, "require");
		X509Certificate cert = CertificateUtils.loadCertificate(new ByteArrayInputStream(CERT.getBytes()), Encoding.PEM);
		pkiManagement.addVolatileCertificate("issuerCert", cert);
		p.setProperty(P+IDPMETA_PREFIX+"1." + METADATA_ISSUER_CERT, "issuerCert");

		p.setProperty(P+IDP_PREFIX+"1." + IDP_ADDRESS, "https://aai.unifr.ch/idp/profile/SAML2/Redirect/SSO");
		p.setProperty(P+IDP_PREFIX+"1." + IDP_BINDING, "HTTP_POST");
		p.setProperty(P+IDP_PREFIX+"1." + IDP_CERTIFICATE, "MAIN");
		p.setProperty(P+IDP_PREFIX+"1." + IDP_GROUP_MEMBERSHIP_ATTRIBUTE, "memberOf");
		p.setProperty(P+IDP_PREFIX+"1." + IDP_ID, "https://aai.unifr.ch/idp/shibboleth");
		p.setProperty(P+IDP_PREFIX+"1." + IDP_LOGO, "http://example.com");
		p.setProperty(P+IDP_PREFIX+"1." + IDP_NAME, "Name");
		p.setProperty(P+IDP_PREFIX+"1." + REGISTRATION_FORM, "regForm");
		p.setProperty(P+IDP_PREFIX+"1." + IDP_REQUESTED_NAME_FORMAT, "foo");
		p.setProperty(P+IDP_PREFIX+"1." + IDP_SIGN_REQUEST, "true");
		p.setProperty(P+IDP_PREFIX+"1." + TRANSLATION_PROFILE, "trProf");

		p.setProperty(P+IDP_PREFIX+"2." + IDP_ADDRESS, "https://aai-login.fh-htwchur.ch/idp/profile/SAML2/POST/SSO");
		p.setProperty(P+IDP_PREFIX+"2." + IDP_ID, "https://aai-login.fh-htwchur.ch/idp/shibboleth");


		SAMLSPProperties configuration = new SAMLSPProperties(p, pkiManagement);
		RemoteMetaManager manager = new RemoteMetaManager(configuration, 
				pkiManagement, 
				new MetaToSPConfigConverter(pkiManagement, msg), 
				metadataService, SAMLSPProperties.IDPMETA_PREFIX);
		
		Awaitility.await()
			.atMost(Duration.FIVE_SECONDS)
			.untilAsserted(() -> assertRemoteMetadataLoaded(manager));

		SAMLSPProperties ret = (SAMLSPProperties) manager.getVirtualConfiguration();
		String pfx = ret.getPrefixOfIdP("https://aai.unifr.ch/idp/shibboleth");
		assertThat(pfx, is(notNullValue()));
		assertEquals("https://aai.unifr.ch/idp/profile/SAML2/Redirect/SSO", ret.getValue(pfx + IDP_ADDRESS));
		assertEquals("HTTP_POST", ret.getValue(pfx + IDP_BINDING));
		assertEquals("MAIN", ret.getValue(pfx + IDP_CERTIFICATE));
		assertEquals("memberOf", ret.getValue(pfx + IDP_GROUP_MEMBERSHIP_ATTRIBUTE));
		assertEquals("https://aai.unifr.ch/idp/shibboleth", ret.getValue(pfx + IDP_ID));
		assertEquals("http://example.com", ret.getValue(pfx + IDP_LOGO));
		assertEquals("Name", ret.getValue(pfx + IDP_NAME));
		assertEquals("regForm", ret.getValue(pfx + REGISTRATION_FORM));
		assertEquals("foo", ret.getValue(pfx + IDP_REQUESTED_NAME_FORMAT));
		assertEquals("true", ret.getValue(pfx + IDP_SIGN_REQUEST));
		assertEquals("trProf", ret.getValue(pfx + TRANSLATION_PROFILE));
		
		pfx = ret.getPrefixOfIdP("https://aai-login.fh-htwchur.ch/idp/shibboleth");
		assertThat(pfx, is(notNullValue()));
		assertEquals("https://aai-login.fh-htwchur.ch/idp/profile/SAML2/POST/SSO", ret.getValue(pfx + IDP_ADDRESS));
		assertEquals("HTTP_REDIRECT", ret.getValue(pfx + IDP_BINDING));
		String certName = ret.getValue(pfx + IDP_CERTIFICATES + "1");
		assertNotNull(pkiManagement.getCertificate(certName));
		assertNull(ret.getValue(pfx + IDP_GROUP_MEMBERSHIP_ATTRIBUTE));
		assertEquals("https://aai-login.fh-htwchur.ch/idp/shibboleth", ret.getValue(pfx + IDP_ID));
		assertEquals(LOGO, ret.getValue(pfx + IDP_LOGO));
		assertEquals("HTW Chur - University of Applied Sciences HTW Chur", ret.getValue(pfx + IDP_NAME+".en"));
		assertEquals("HTW Chur - Hochschule für Technik und Wirtschaft", ret.getValue(pfx + IDP_NAME+".de"));
		assertEquals("metaRegForm", ret.getValue(pfx + REGISTRATION_FORM));
		assertNull(ret.getValue(pfx + IDP_REQUESTED_NAME_FORMAT));
		assertEquals("false", ret.getValue(pfx + IDP_SIGN_REQUEST));
		assertEquals("metaTrP", ret.getValue(pfx + TRANSLATION_PROFILE));
	}

	@Test
	public void shouldConfigure2TrustedFederations() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(P+CREDENTIAL, "MAIN");
		p.setProperty(P+REQUESTER_ID, "me");
		p.setProperty(P+PUBLISH_METADATA, "false");

		p.setProperty(P+IDPMETA_PREFIX+"1." + METADATA_URL, "file:src/test/resources/metadata.switchaai.xml");
		p.setProperty(P+IDPMETA_PREFIX+"1." + IDPMETA_TRANSLATION_PROFILE, "metaTrP");
		p.setProperty(P+IDPMETA_PREFIX+"1." + IDPMETA_REGISTRATION_FORM, "metaRegForm");

		p.setProperty(P+IDPMETA_PREFIX+"2." + METADATA_URL, "file:src/test/resources/metadata.switchaai-one.xml");
		p.setProperty(P+IDPMETA_PREFIX+"2." + IDPMETA_TRANSLATION_PROFILE, "metaTrP");
		p.setProperty(P+IDPMETA_PREFIX+"2." + IDPMETA_REGISTRATION_FORM, "metaRegForm");

		SAMLSPProperties configuration = new SAMLSPProperties(p, pkiManagement);
		RemoteMetaManager manager = new RemoteMetaManager(configuration, 
				pkiManagement, 
				new MetaToSPConfigConverter(pkiManagement, msg), 
				metadataService, SAMLSPProperties.IDPMETA_PREFIX);
		
		Awaitility.await()
			.atMost(Duration.FIVE_SECONDS)
			.untilAsserted(() -> {
				SAMLSPProperties ret = (SAMLSPProperties) manager.getVirtualConfiguration();
				
				String pfx = ret.getPrefixOfIdP("https://aai-login.fh-htwchur.ch/idp/shibboleth");
				assertThat(pfx, is(notNullValue()));
				assertEquals("HTW Chur - Hochschule für Technik und Wirtschaft", ret.getValue(pfx + IDP_NAME+".de"));
				
				String pfx2 = ret.getPrefixOfIdP("https://fake.idp.eu");
				assertThat(pfx2, is(notNullValue()));
				assertEquals("Universität Freiburg", ret.getValue(pfx2 + IDP_NAME+".de"));
				
			});
	}

	
	private void assertRemoteMetadataLoaded(RemoteMetaManager manager) throws EngineException
	{
		SAMLSPProperties ret = (SAMLSPProperties) manager.getVirtualConfiguration();
		
		String pfx = ret.getPrefixOfIdP("https://aai-login.fh-htwchur.ch/idp/shibboleth");
		assertThat(pfx, is(notNullValue()));
		assertEquals("HTW Chur - Hochschule für Technik und Wirtschaft", ret.getValue(pfx + IDP_NAME+".de"));
	}
	
	private static final String LOGO = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFAAAAA8CAIAAAB+RarbAAAC0GlDQ1BJQ0NQcm9maWxlAAB4nI2Uz0sUYRjHv7ONGChBYGZ7iKFDSKhMFmVE5a6/2LRtWX+UEsTs7Lu7k7Oz08zsmiIRXjpm0T0qDx76Azx46JSXwsAsAuluUUSCl5LteWfG3RHtxwsz83mfH9/ned/hfYEaWTFNPSQBecOxkn1R6fromFT7ESEcQR3CqFNU24wkEgOgwWOxa2y+h8C/K617+/866tK2mgeE/UDoR5rZKrDvF9kLWWoEELlew4RjOsT3OFue/THnlMfzrn0o2UW8SHxANS0e/5q4Q80paaBGJG7JBmJSAc7rRdXv5yA99cwYHqTvcerpLrN7fBZm0kp3P3Eb8ec06+7hmsTzGa03RtxMz1rG6h32WDihObEhj0Mjhh4f8LnJSMWv+pqi6UST2/p2abBn235LuZwgDhMnxwv9PKaRcjunckPXPBb0qVxX3Od3VjHJ6x6jmDlTd/8X9RZ6hVHoYNBg0NuAhCT6EEUrTFgoIEMejSI0sjI3xiK2Mb5npI5EgCXyr1POuptzG0XK5lkjiMYx01JRkOQP8ld5VX4qz8lfZsPF5qpnxrqpqcsPvpMur7yt63v9njx9lepGyKsjS9Z8ZU12oNNAdxljNlxV4jXY/fhmYJUsUKkVKVdp3K1Ucn02vSOBan/aPYpdml5sqtZaFRdurNQvTe/Yq8KuVbHKqnbOq3HBfCYeFU+KMbFDPAdJvCR2ihfFbpqdFwcqGcOkomHCVbKhUJaBSfKaO/6ZFwvvrLmjoY8ZzNJUiZ//hFXIaDoLHNF/uP9z8HvFo7Ei8MIGDp+u2jaS7h0iNC5Xbc4V4MI3ug/eVm3NdB4OPQEWzqhFq+RLC8IbimZ3HD7pKpiTlpbNOVK7LJ+VInQlMSlmqG0tkqLrkuuyJYvZzCqxdBvszKl2T6WedqXmU7m8Qeev9hGw9bBc/vmsXN56Tj2sAS/138C8/UXN/ALEAAAJI2lUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNC40LjAiPgogICA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPgogICAgICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIi8+CiAgIDwvcmRmOlJERj4KPC94OnhtcG1ldGE+CiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAKPD94cGFja2V0IGVuZD0idyI/Pqfd9JIAAAAhdEVYdFNvZnR3YXJlAEdyYXBoaWNDb252ZXJ0ZXIgKEludGVsKXeH+hkAAAbvSURBVHic7JdZTFRnFMevwfhQTaPUhgfeWm1kLVBNNFRtDPpgQ2360PIgwYYH1PBCYmIV4vLSRCVYdCxg6bDJrrLJqqCoICiIOyCI7DAMi8CMgDC0v+HD25EhRAtt0+s9ubn57vnOd77z/87ynSv98Z6R9F8b8G+TCljppAJWOqmAlU4qYKWTCljppAJWOqmAlU4qYKWTCljp9P4BLixrTclvSC1o5GGQWdI0OjYhT09MTBiNxr6+vtbW1mfPnvFpraK5ufnp06e8Eaivr9fr9d3d3U+ePGlsbKytrYXPWgQ6Ozvb2tpgog0x+LKGa9euRUREZGRkjI2NITk+Pv4PAnb5LkVyOC05nzE/jpoVG7W6vpdiDtPv3btXVVV1e4ru3LljMBisVQQEBCxdunT//v3btm2ztbW9cOGCr6+vu7s7Yw8Pj3Xr1gUFBS1btuzYsWO7d+92cHDIz8+3s7NjLJZfunSJKQ4oMTExJiZGo9EMDg7Oauvk5KTJZJovYA+fdMktQvoiyvy4R9p5xfX0TwPu7++vrKysek1g7unpsVZRVla2ePFizPX391+9ejVmBQYGpqamAjgqKmrXrl04dtWqVQkJCQcOHECgoaGB4+jq6hLLDx8+3NvbK+946NCh48ePh4eHM+YshH40JCUlEQWzGrBggEdHR+/evWsJ+Pnz59YqysvLbWxsvL29XV1dnZycXr16RWRiIl5NS0tjjMy+ffs2bNiwdu1aYsHHx8fPz0+s5XSOHDkyPDwsawOwTqcDXklJyYkTJ+DEx8cTaEePHm1vb58nWjNgtx/SJNdfgWp+Po/4eEusDBhrHj9+TCSDljfefvToEcwZKvDDokWLsOngwYP29vbCXQ8ePFiyZAk+EfLV1dWSJHl5ee3cuZNBdna2vBx/cpQMWFhcXIxvyWEELl++HBoaCv/s2bM1NTWnTp0aGhpaAMCbf8xc7hm9fJPW/Hz5+2ffJOkHRuRp6lBFRQWAAUCl6ejomJFF4NmzZ4+joyOJumPHDhcXF0IX/t69e52dnbdu3Uqt4hMM27dvJ0WpT56engMDA7IGYiE4OBicISEhN27cSE5ORvjq1aucYEpKysmTJ9GMGaSMZSD8fcBdemNL51BL17D56Rxu1xlMpr98yKGSNhTqWeuzoNE3iZCWmSMjI3JEAEOMhYAlwaGSCzxiI45VCLM7s4znMODdAC+Ilv8RqYDfnUSIimosaEb4WX7KYkQp+ULYi08GJLN8z1uWRnLBUrlMhD1TIokg0kcwLRfKY/YSZki5pc3x2XUJOfU88dn16UWNI687LePIeEpBQ/zUlHk2s7amTm+9MSWN6l1QUCCqGtdsUVERhbelpUX0Z1euXOEupcHiar1+/Trtl26KuLToq5qamrjtKBOUKO4C5GnCuOq4lih4aEYhNYwaDuwXL17QxlFByW1uB5RQ57jzKeksqaurwwzeyPBGAJMwAw3cc0xxHJIzndaa05LTGfPjoFlu0Wm1dRs+WB8Nc3r2k19+Cq+wBsyWGKfVarlLzp07B1pUM05PT+ezsLAQTm5u7sWLF7EAVGFhYRwB4AGMoVyz9+/fRw9nQaMSHR2dmZlZWlqak5ODBhayigtZHBmf58+fz8vLo5ijHCVsTfHn6mJH9mJrtqCkoyErKwslcXFxzDJFh2cGPEfjQcVesTnGfD+LWUdNyJnb1oA5dRThAbyBcx4+fMgbU/AexwwTB9It8saBNFgI4DqCFi/hK8RevjTviAa8jTdYBZOrC2H04EY8xhJikmhCD1NsgTaCVvicdOANn1l0EibcaixBJwN2ZACTyF8AwNiExaLfME3RDAG2wTmWHAwFGwNSwPKWIg8BIH+yCkm6PdFjyQk/H5Lcvn+j01pp0WkB+MONWpjTs2tOB2sqrVXQmXC6BAzxSTfGJyfK582bN2/dukXIccbELXx+lXA1KUBHhTAei4yMRIZgZrZ4iljFEjjkLcFMsSGwYRLSRL5YiwCrWMJZsBH1glnrFnB2wFv8s1Zu1K78Ksb8bNI6fJssd1qdeuOnXyfCFLMfrf/t5+hqaxXAwwhSiLJBqjAgzei3SCFaQpG6ZB1/UcAgPuEjRuwRluQkmUxFISd5M0V6x8bGsgqcqMXhIkspBMQweYty+HyikwFHydlRt97S/xL+7NAZOnqmHp2hq9cod1oTpkkwy7M4fNAw+/VAJSSqxd1ALSUOSSc6JziEOlPcNzhHBDapi3G4V5iIJO4iCYkOsZwowLEI88ZvxDPL+VdFIcVclGuhGQHe8GekzFyA31Ju/rRQveE8Se20lE4qYKWTCljppAJWOqmAlU4qYKWTCljppAJWOqmAlU4qYKXTewf4TwAAAP//AwDTJKa92O1TGAAAAABJRU5ErkJggg==";
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
