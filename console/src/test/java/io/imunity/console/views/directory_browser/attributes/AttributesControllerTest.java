/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.attributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.authn.CredentialInfo;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.EffectiveAttributesCacheService;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;

@ExtendWith(MockitoExtension.class)
class AttributesControllerTest
{
	@Mock
	private AttributesManagement attrMan;
	@Mock
	private AttributeClassManagement acMan;
	@Mock
	private AttributeTypeManagement aTypeManagement;
	@Mock
	private GroupsManagement groupsManagement;
	@Mock
	private AttributeHandlerRegistry registry;
	@Mock
	private AttributeTypeSupport atSupport;
	@Mock
	private MessageSource msg;
	@Mock
	private NotificationPresenter notificationPresenter;
	@Mock
	private EffectiveAttributesCacheService cacheService;

	private final EntityWithLabel owner = new EntityWithLabel(
			new Entity(List.of(), new EntityInformation(7L), new CredentialInfo("cr", Map.of())), "John");

	private AttributesController controller()
	{
		return new AttributesController(attrMan, acMan, aTypeManagement, groupsManagement, registry, atSupport, msg,
				notificationPresenter, cacheService);
	}

	@Test
	void returnsCompleteConsistentAttributesOfSelectedEntityAndGroup() throws Exception
	{
		AttributeExt name = new AttributeExt(StringAttribute.of("name", "/A", "John"), true);
		AttributeExt emails = new AttributeExt(StringAttribute.of("email", "/A", List.of("a@x", "b@x")), false);
		when(cacheService.getAttributes(7L, "/A")).thenReturn(Map.of("name", name, "email", emails));

		assertThat(controller().getAttributes(owner, "/A")).containsExactlyInAnyOrder(name, emails);
		verify(cacheService, never()).getAttributesFast(anyLong(), anyString());
	}

	@Test
	void reportsRetrievalErrorAndReturnsNoAttributes() throws Exception
	{
		when(cacheService.getAttributes(7L, "/A")).thenThrow(new AuthorizationException("denied"));
		when(msg.getMessage(eq("Attribute.internalError"), any(Object[].class))).thenReturn("error caption");

		assertThat(controller().getAttributes(owner, "/A")).isEmpty();
		verify(notificationPresenter).showError("error caption", "denied");
	}
}
