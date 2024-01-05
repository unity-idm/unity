/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_browser.group_details;

import io.imunity.console.views.directory_browser.group_browser.GroupChangedEvent;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Component
class AttributeStatementController
{
	private final AttributeHandlerRegistry attributeHandlerRegistry;
	private final GroupsManagement groupsMan;
	private final AttributeTypeManagement attrsMan;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	AttributeStatementController(AttributeHandlerRegistry attributeHandlerRegistry, GroupsManagement groupsMan,
	                             AttributeTypeManagement attrsMan, MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.groupsMan = groupsMan;
		this.attrsMan = attrsMan;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	AttributeStatementEditDialog getEditStatementDialog(String group, AttributeStatement toEdit,
			Consumer<AttributeStatement> callback)
	{
		return new AttributeStatementEditDialog(msg, toEdit, attrsMan, group, attributeHandlerRegistry, groupsMan,
				callback::accept, notificationPresenter);

	}

	public void updateGroup(List<AttrStatementWithId> contents, Group group, EventsBus bus)
	{
		AttributeStatement[] attributeStatements = contents.stream().map(withId -> withId.statement)
				.toList().toArray(new AttributeStatement[contents.size()]);
		Group updated = group.clone();
		updated.setAttributeStatements(attributeStatements);
		try
		{
			groupsMan.updateGroup(updated.toString(), updated, "set group statement",  Arrays.asList(attributeStatements).toString());
			bus.fireEvent(new GroupChangedEvent(group, false));
		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("AttributeStatementController.updateGroupStatementsError",
							group.toString()),
					e.getMessage());
		}
	}
}
