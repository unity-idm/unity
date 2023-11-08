/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleActionHandler;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;

import java.util.Set;
import java.util.function.Supplier;

@Component
class EntityAttributeClassHandler
{
	private final AttributeClassManagement acMan;
	private final GroupsManagement groupsMan;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	EntityAttributeClassHandler(AttributeClassManagement acMan, GroupsManagement groupsMan, MessageSource msg,
			NotificationPresenter notificationPresenter)
	{
		this.acMan = acMan;
		this.groupsMan = groupsMan;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	SingleActionHandler<IdentityEntry> getAction(Runnable refreshCallback,
			Supplier<String> groupSupplier)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.editEntityACs"))
				.withIcon(VaadinIcon.TAGS)
				.withHandler(selection -> showDialog(selection,
						refreshCallback, groupSupplier))
				.build();
	}
	
	private void showDialog(Set<IdentityEntry> selection, Runnable refreshCallback, 
			Supplier<String> groupSupplier)
	{       
		EntityWithLabel entity = selection.iterator().next().getSourceEntity();
		EntityAttributesClassesDialog dialog = new EntityAttributesClassesDialog(
				msg, groupSupplier.get(), entity, acMan, groupsMan, refreshCallback, notificationPresenter);
		dialog.open();
	}
}
