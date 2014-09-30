/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.CREDENTIAL;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.DEFAULT_GROUP;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.GROUP;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.GROUP_PFX;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.GROUP_TARGET;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.IDENTITY_LOCAL;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.IDENTITY_MAPPING_PFX;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.IDENTITY_SAML;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.ISSUER_URI;
import static pl.edu.icm.unity.saml.idp.SAMLIDPProperties.P;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.saml.idp.GroupChooser;
import pl.edu.icm.unity.saml.idp.IdentityTypeMapper;
import pl.edu.icm.unity.saml.idp.SAMLIDPProperties;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.stdext.identity.TargetedPersistentIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;

public class TestSamlConfiguration
{
	@Test
	public void testGroupChooser() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(P+ISSUER_URI, "foo");
		
		p.setProperty(P+GROUP_PFX+"1."+GROUP_TARGET, "http://sp.org1");
		p.setProperty(P+GROUP_PFX+"1."+GROUP, "/some/gr");
		p.setProperty(P+GROUP_PFX+"2."+GROUP_TARGET, "sp2");
		p.setProperty(P+GROUP_PFX+"2."+GROUP, "/");
		p.setProperty(P+DEFAULT_GROUP, "/def");
		p.setProperty(P+CREDENTIAL, "MAIN");
		SAMLIDPProperties cfg = new SAMLIDPProperties(p, getPKIManagement());
		
		GroupChooser chooser = cfg.getGroupChooser();
		assertEquals("/some/gr", chooser.chooseGroup("http://sp.org1"));
		assertEquals("/", chooser.chooseGroup("sp2"));
		assertEquals("/def", chooser.chooseGroup("other"));
	}
	
	@Test
	public void testIdentityMapper() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(P+ISSUER_URI, "foo");
		p.setProperty(P+CREDENTIAL, "MAIN");
		p.setProperty(P+DEFAULT_GROUP, "/");
		
		p.setProperty(P+IDENTITY_MAPPING_PFX+"1."+IDENTITY_LOCAL, "qqq");
		p.setProperty(P+IDENTITY_MAPPING_PFX+"1."+IDENTITY_SAML, "123");
		p.setProperty(P+IDENTITY_MAPPING_PFX+"2."+IDENTITY_LOCAL, "aaa");
		p.setProperty(P+IDENTITY_MAPPING_PFX+"2."+IDENTITY_SAML, SAMLConstants.NFORMAT_TRANSIENT);
		p.setProperty(P+IDENTITY_MAPPING_PFX+"3."+IDENTITY_LOCAL, "");
		p.setProperty(P+IDENTITY_MAPPING_PFX+"3."+IDENTITY_SAML, "unity:identifier");
		SAMLIDPProperties cfg = new SAMLIDPProperties(p, getPKIManagement());

		IdentityTypeMapper idMapper = cfg.getIdTypeMapper();
		assertEquals("qqq", idMapper.mapIdentity("123"));
		assertEquals("aaa", idMapper.mapIdentity(SAMLConstants.NFORMAT_TRANSIENT));
		assertEquals(X500Identity.ID, idMapper.mapIdentity(SAMLConstants.NFORMAT_DN));
		assertEquals(TargetedPersistentIdentity.ID, idMapper.mapIdentity(SAMLConstants.NFORMAT_UNSPEC));
		
		try
		{
			idMapper.mapIdentity("unity:identifier");
			fail("Should get exception");
		} catch (SAMLRequesterException e)
		{
			//OK
		}
		
		assertEquals(idMapper.getSupportedIdentityTypes().toString(), 7, idMapper.getSupportedIdentityTypes().size());
	}

	private PKIManagement getPKIManagement()
	{
		return new PKIManagement()
		{
			@Override
			public Set<String> getValidatorNames() throws EngineException
			{
				return new HashSet<>();
			}
			
			@Override
			public X509CertChainValidatorExt getValidator(String name) throws EngineException
			{
				throw new WrongArgumentException("No such validator " + name);
			}
			
			@Override
			public Set<String> getCredentialNames() throws EngineException
			{
				return Collections.singleton("MAIN");
			}
			@Override
			public X509Credential getCredential(String name) throws EngineException
			{
				if (name.equals("MAIN"))
					try
					{
						return new KeystoreCredential("src/test/resources/demoKeystore.p12",
								"the!uvos".toCharArray(), "the!uvos".toCharArray(), 
								null, "PKCS12");
					} catch (Exception e)
					{
						throw new InternalException("error loading credential", e);
					}
				throw new WrongArgumentException("No such validator " + name);				
			}

			@Override
			public IAuthnAndTrustConfiguration getMainAuthnAndTrust()
			{
				return null;
			}

			@Override
			public Set<String> getCertificateNames() throws EngineException
			{
				return null;
			}

			@Override
			public X509Certificate getCertificate(String name) throws EngineException
			{
				return null;
			}

			@Override
			public void updateCertificate(String name, X509Certificate updated)
					throws EngineException
			{
			}

			@Override
			public void removeCertificate(String name) throws EngineException
			{
			}

			@Override
			public void addCertificate(String name, X509Certificate updated)
					throws EngineException
			{
			}
		};
	}
}
