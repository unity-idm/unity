/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import eu.unicore.samly2.SAMLConstants;
import org.junit.Test;

import java.util.Properties;

import static pl.edu.icm.unity.saml.SamlProperties.*;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.P;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.*;

public class SAMLIdPConfigurationParserTest
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
	}
}
