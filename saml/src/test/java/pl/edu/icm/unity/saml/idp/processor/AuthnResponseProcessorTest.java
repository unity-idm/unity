/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.processor;

import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;

public class AuthnResponseProcessorTest extends DBIntegrationTestBase
{
	@Autowired
	private AttributeTypeSupport aTypeSupport;
	
	@Autowired
	private LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement;
	
	@Test
	public void shouldProcessAttributesFromProfileWithDynamicOne() throws Exception
	{
//		AuthnResponseProcessor processor = getMockProcessor();
//		TranslationResult userInfo = mock(TranslationResult.class);
//		Attribute attribute = StringAttribute.of("dynamic", "/", "value");
//		DynamicAttribute dynAttr = new DynamicAttribute(attribute);
//		when(userInfo.getAttributes()).thenReturn(Lists.newArrayList(dynAttr));
//		SPSettings preferences = mock(SPSettings.class);
//		SamlAttributeMapper mapper = mock(SamlAttributeMapper.class);
//		SAMLIdPConfiguration config = processor.getSamlConfiguration();
//		when(config.getAttributesMapper()).thenReturn(mapper);
//		when(mapper.isHandled(attribute)).thenReturn(true);
//
//		Collection<Attribute> attrs = processor.getAttributes(userInfo, preferences);
//
//		assertThat(attrs.size(), is(1));
	}

	@Test
	public void shouldFilterDynamicAttributeWithPreferences() throws Exception
	{
//		AuthnResponseProcessor processor = getMockProcessor();
//		TranslationResult userInfo = mock(TranslationResult.class);
//		Attribute attribute = StringAttribute.of("dynamic", "/", "value");
//		DynamicAttribute dynAttr = new DynamicAttribute(attribute);
//		when(userInfo.getAttributes()).thenReturn(Lists.newArrayList(dynAttr));
//		SamlAttributeMapper mapper = mock(SamlAttributeMapper.class);
//		SamlIdpProperties config = processor.getSamlConfiguration();
//		when(config.getAttributesMapper()).thenReturn(mapper);
//		when(mapper.isHandled(attribute)).thenReturn(true);

//		SPSettings preferences = mock(SPSettings.class);
//		when(preferences.getHiddenAttribtues()).thenReturn(ImmutableMap.of("dynamic", attribute));
//
//		Collection<Attribute> attrs = processor.getAttributes(userInfo, preferences);
//
//		assertThat(attrs.size(), is(1));
//		assertThat(attrs.iterator().next().getValues().size(), is(0));
	}
	
//	private AuthnResponseProcessor getMockProcessor()
//	{
////		SAMLAuthnContext context = mock(SAMLAuthnContext.class);
////		SamlIdpProperties config = mock(SamlIdpProperties.class);
////		when(context.getSamlConfiguration()).thenReturn(config);
////		when(config.getGroupChooser()).thenReturn(mock(GroupChooser.class));
////		AuthnRequestType request = mock(AuthnRequestType.class);
////		when(request.getIssuer()).thenReturn(mock(NameIDType.class));
////		when(context.getRequest()).thenReturn(request);
////		return new AuthnResponseProcessor(aTypeSupport, lastAccessAttributeManagement, context);
//	}
}
