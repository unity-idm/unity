/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static pl.edu.icm.unity.saml.SamlProperties.IDENTITY_LOCAL;
import static pl.edu.icm.unity.saml.SamlProperties.IDENTITY_MAPPING_PFX;
import static pl.edu.icm.unity.saml.SamlProperties.IDENTITY_SAML;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.CREDENTIAL;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.DEFAULT_GROUP;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.GROUP;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.GROUP_PFX;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.GROUP_TARGET;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.ISSUER_URI;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.P;

import java.util.Properties;

import org.junit.Test;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import pl.edu.icm.unity.saml.SimplePKIManagement;
import pl.edu.icm.unity.saml.idp.GroupChooser;
import pl.edu.icm.unity.saml.idp.IdentityTypeMapper;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.stdext.identity.TargetedPersistentIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;

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
		SamlIdpProperties cfg = new SamlIdpProperties(p, new SimplePKIManagement());
		
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
		SamlIdpProperties cfg = new SamlIdpProperties(p, new SimplePKIManagement());

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
}
