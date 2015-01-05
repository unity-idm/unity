/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.*;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import pl.edu.icm.unity.saml.SimplePKIManagement;
import pl.edu.icm.unity.saml.metadata.SPMetadataGenerator;
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
		
		SAMLSPProperties config = new SAMLSPProperties(p, new SimplePKIManagement());
		
		SPMetadataGenerator generator = new SPMetadataGenerator(config, 
				new IndexedEndpointType[0], new EndpointType[0]);
		
		Assert.assertNotNull(generator.getMetadata());
	}
	
}
