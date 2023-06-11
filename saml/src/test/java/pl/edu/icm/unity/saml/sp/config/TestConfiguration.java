/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.config;

import static org.mockito.Mockito.mock;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.CREDENTIAL;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.DEF_SIGN_REQUEST;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_BINDING;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_ID;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_PREFIX;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.METADATA_PATH;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.P;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.REQUESTER_ID;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.saml.SimplePKIManagement;
import pl.edu.icm.unity.saml.metadata.SPMetadataGenerator;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;
import xmlbeans.org.oasis.saml2.metadata.IndexedEndpointType;

public class TestConfiguration
{
	@Test
	public void testDefaultsAreUsed()
	{
		Properties p = new Properties();
		p.setProperty(P+REQUESTER_ID, "foo");
		p.setProperty(P+DEF_SIGN_REQUEST, "true");
		p.setProperty(P+CREDENTIAL, "MAIN");
		p.setProperty(P+METADATA_PATH, "meta");
		p.setProperty(P+IDP_PREFIX+"K1."+IDP_ID, "idp");
		p.setProperty(P+IDP_PREFIX+"K1."+IDP_BINDING, "HTTP_POST");
		
		SAMLSPProperties config = new SAMLSPProperties(p, new SimplePKIManagement());
		Assert.assertTrue(config.isSignRequest("K1"));
	}
	
	@Test
	public void testMetadataGenerationWorksWithoutIdpsDefined()
	{
		Properties p = new Properties();
		p.setProperty(P+REQUESTER_ID, "foo");
		p.setProperty(P+DEF_SIGN_REQUEST, "true");
		p.setProperty(P+CREDENTIAL, "MAIN");
		p.setProperty(P+METADATA_PATH, "meta");
		
		SAMLSPConfigurationParser configParser = new SAMLSPConfigurationParser(new SimplePKIManagement(),
				mock(MessageSource.class));
		SAMLSPConfiguration configuration = configParser.parse(p);
		
		SPMetadataGenerator generator = new SPMetadataGenerator(configuration, 
				new IndexedEndpointType[0], new EndpointType[0]);
		
		Assert.assertNotNull(generator.getMetadata());
	}
	
}
