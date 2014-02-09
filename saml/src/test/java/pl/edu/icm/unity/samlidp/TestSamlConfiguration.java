/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.*;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.saml.idp.AttributeFilters;
import pl.edu.icm.unity.saml.idp.GroupChooser;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
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
		SamlIdpProperties cfg = new SamlIdpProperties(p, getPKIManagement());
		
		GroupChooser chooser = cfg.getGroupChooser();
		assertEquals("/some/gr", chooser.chooseGroup("http://sp.org1"));
		assertEquals("/", chooser.chooseGroup("sp2"));
		assertEquals("/def", chooser.chooseGroup("other"));
	}
	
	@Test
	public void testAttributeFilter() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(P+ISSUER_URI, "foo");
		
		p.setProperty(P+ATTRIBUTE_FILTER+"1."+ATTRIBUTE_FILTER_TARGET, "sp.*");
		p.setProperty(P+ATTRIBUTE_FILTER+"1."+ATTRIBUTE_FILTER_EXCLUDE+"1", "a");
		p.setProperty(P+ATTRIBUTE_FILTER+"1."+ATTRIBUTE_FILTER_EXCLUDE+"2", "b");
		p.setProperty(P+ATTRIBUTE_FILTER+"1."+ATTRIBUTE_FILTER_INCLUDE+"1", "z");
		
		p.setProperty(P+ATTRIBUTE_FILTER+"2."+ATTRIBUTE_FILTER_TARGET, "gg");
		p.setProperty(P+ATTRIBUTE_FILTER+"2."+ATTRIBUTE_FILTER_EXCLUDE+"1", "a");
		p.setProperty(P+ATTRIBUTE_FILTER+"2."+ATTRIBUTE_FILTER_EXCLUDE+"2", "b");
		p.setProperty(P+ATTRIBUTE_FILTER+"2."+ATTRIBUTE_FILTER_EXCLUDE+"3", "c");

		p.setProperty(P+ATTRIBUTE_FILTER+"3."+ATTRIBUTE_FILTER_TARGET, "qq");
		p.setProperty(P+ATTRIBUTE_FILTER+"3."+ATTRIBUTE_FILTER_INCLUDE+"1", "a");

		p.setProperty(P+DEFAULT_GROUP, "/");
		p.setProperty(P+CREDENTIAL, "MAIN");
		SamlIdpProperties cfg = new SamlIdpProperties(p, getPKIManagement());
		
		AttributeFilters filter = cfg.getAttributeFilter();
		
		List<Attribute<?>> attributes = getAttrs();
		filter.filter(attributes, "spAAA");
		assertEquals(1, attributes.size());
		assertEquals("z", attributes.get(0).getName());
		
		attributes = getAttrs();
		filter.filter(attributes, "gg");
		assertEquals(2, attributes.size());
		assertEquals("d", attributes.get(0).getName());
		assertEquals("z", attributes.get(1).getName());
		
		attributes = getAttrs();
		filter.filter(attributes, "qq");
		assertEquals(1, attributes.size());
		assertEquals("a", attributes.get(0).getName());
	}
	
	private List<Attribute<?>> getAttrs()
	{
		List<Attribute<?>> attributes = new ArrayList<Attribute<?>>();
		attributes.add(new StringAttribute("a", "/", AttributeVisibility.local, ""));
		attributes.add(new StringAttribute("b", "/", AttributeVisibility.local, ""));
		attributes.add(new StringAttribute("c", "/", AttributeVisibility.local, ""));
		attributes.add(new StringAttribute("d", "/", AttributeVisibility.local, ""));
		attributes.add(new StringAttribute("z", "/", AttributeVisibility.local, ""));
		return attributes;
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
		};
	}
}
