/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.processor;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.saml.idp.GroupChooser;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration;
import pl.edu.icm.unity.saml.idp.SamlAttributeMapper;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthnResponseProcessorTest extends DBIntegrationTestBase
{
	@Autowired
	private AttributeTypeSupport aTypeSupport;
	
	@Autowired
	private LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement;
	
	@Test
	public void shouldProcessAttributesFromProfileWithDynamicOne() throws Exception
	{
		AuthnResponseProcessor processor = getMockProcessor();
		TranslationResult userInfo = mock(TranslationResult.class);
		Attribute attribute = StringAttribute.of("dynamic", "/", "value");
		DynamicAttribute dynAttr = new DynamicAttribute(attribute);
		when(userInfo.getAttributes()).thenReturn(Lists.newArrayList(dynAttr));
		SamlPreferences.SPSettings preferences = mock(SamlPreferences.SPSettings.class);
		SamlAttributeMapper mapper = mock(SamlAttributeMapper.class);
		SAMLIdPConfiguration config = processor.getSamlConfiguration();
		when(config.getAttributesMapper()).thenReturn(mapper);
		when(mapper.isHandled(attribute)).thenReturn(true);

		Collection<Attribute> attrs = processor.getAttributes(userInfo, preferences);

		assertThat(attrs.size()).isEqualTo(1);
	}

	@Test
	public void shouldFilterDynamicAttributeWithPreferences() throws Exception
	{
		AuthnResponseProcessor processor = getMockProcessor();
		TranslationResult userInfo = mock(TranslationResult.class);
		Attribute attribute = StringAttribute.of("dynamic", "/", "value");
		DynamicAttribute dynAttr = new DynamicAttribute(attribute);
		when(userInfo.getAttributes()).thenReturn(Lists.newArrayList(dynAttr));
		SamlAttributeMapper mapper = mock(SamlAttributeMapper.class);
		SAMLIdPConfiguration config = processor.getSamlConfiguration();
		when(config.getAttributesMapper()).thenReturn(mapper);
		when(mapper.isHandled(attribute)).thenReturn(true);

		SamlPreferences.SPSettings preferences = mock(SamlPreferences.SPSettings.class);
		when(preferences.getHiddenAttribtues()).thenReturn(ImmutableMap.of("dynamic", attribute));

		Collection<Attribute> attrs = processor.getAttributes(userInfo, preferences);

		assertThat(attrs.size()).isEqualTo(1);
		assertThat(attrs.iterator().next().getValues().size()).isEqualTo(0);
	}
	
	private AuthnResponseProcessor getMockProcessor()
	{
		SAMLAuthnContext context = mock(SAMLAuthnContext.class);
		SAMLIdPConfiguration config = mock(SAMLIdPConfiguration.class);
		when(context.getSamlConfiguration()).thenReturn(config);
		GroupChooser groupChooser = mock(GroupChooser.class);
		when(config.getGroupChooser()).thenReturn(groupChooser);
//		when(groupChooser.chooseGroup(any())).thenReturn("group");
		AuthnRequestType request = mock(AuthnRequestType.class);
		when(request.getIssuer()).thenReturn(mock(NameIDType.class));
		when(context.getRequest()).thenReturn(request);
		return new AuthnResponseProcessor(aTypeSupport, lastAccessAttributeManagement, context);
	}
}
