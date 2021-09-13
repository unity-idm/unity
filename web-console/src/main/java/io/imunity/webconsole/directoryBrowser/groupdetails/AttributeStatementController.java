/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directoryBrowser.groupdetails;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.directoryBrowser.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@Component
class AttributeStatementController
{
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private GroupsManagement groupsMan;
	private AttributeTypeManagement attrsMan;
	private MessageSource msg;

	@Autowired
	AttributeStatementController(AttributeHandlerRegistry attributeHandlerRegistry, GroupsManagement groupsMan,
			AttributeTypeManagement attrsMan, MessageSource msg)
	{
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.groupsMan = groupsMan;
		this.attrsMan = attrsMan;
		this.msg = msg;
	}

	AttributeStatementEditDialog getEditStatementDialog(String group, AttributeStatement toEdit,
			Consumer<AttributeStatement> callback) throws ControllerException
	{
		try
		{
			return new AttributeStatementEditDialog(msg, toEdit, attrsMan, group, attributeHandlerRegistry,
					groupsMan, as -> callback.accept(as));
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AttributeStatementController.createEditorError"),
					e);
		}
	}

	public void updateGroup(List<AttrStatementWithId> contents, Group group, EventsBus bus)
			throws ControllerException
	{
		AttributeStatement[] attributeStatements = contents.stream().map(withId -> withId.statement)
				.collect(Collectors.toList()).toArray(new AttributeStatement[contents.size()]);
		Group updated = group.clone();
		updated.setAttributeStatements(attributeStatements);
		try
		{
			groupsMan.updateGroup(updated.toString(), updated, "set group statement",  Arrays.asList(attributeStatements).toString());
			bus.fireEvent(new GroupChangedEvent(group));
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AttributeStatementController.updateGroupStatementsError",
							group.toString()),
					e);
		}
	}
}
