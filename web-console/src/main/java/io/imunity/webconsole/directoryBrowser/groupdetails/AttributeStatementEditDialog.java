/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.groupdetails;

import java.util.Collection;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;

/**
 * Allows to create/edit/view an attribute statement of a group.
 * @author K. Benedyczak
 */
class AttributeStatementEditDialog extends AbstractDialog
{
	private AttributeStatement statement;
	private AttributeTypeManagement attrsMan;
	private final Callback callback;
	private final String group;
	
	private AttributeStatementComponent component;

	private AttributeHandlerRegistryV8 attrHandlerRegistry;
	private GroupsManagement groupsMan;
	
	/**
	 * @param attributeStatement attribute statement to be displayed initially or 
	 * null when a new statement should be created
	 */
	AttributeStatementEditDialog(MessageSource msg, AttributeStatement attributeStatement,
			AttributeTypeManagement attrsMan, String group, AttributeHandlerRegistryV8 attrHandlerRegistry,
			GroupsManagement groupsMan, Callback callback)
	{
		super(msg, msg.getMessage("AttributeStatementEditDialog.caption"));
		this.statement = attributeStatement;
		this.attrsMan = attrsMan;
		this.group = group;
		this.callback = callback;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.groupsMan = groupsMan;
	}

	@Override
	protected Component getContents() throws Exception
	{
		Collection<AttributeType> attributeTypes;
		
		try
		{
			attributeTypes = attrsMan.getAttributeTypes();
		} catch(Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("AttributeStatementEditDialog.cantReadAttributeTypes"), e);
			throw e;
		}

		component = new AttributeStatementComponent(
				msg, groupsMan, attributeTypes, attrHandlerRegistry, group);
		
		if (statement != null)
			setInitialData(statement);
		return component;
	}

	private void setInitialData(AttributeStatement attributeStatement)
	{
		component.setInitialData(attributeStatement);
	}

	@Override
	protected void onConfirm()
	{
		AttributeStatement ret;
		try
		{
			ret = component.getStatementFromComponent();
		} catch (FormValidationException e)
		{
			NotificationPopup.showError(msg.getMessage("AttributeStatementEditDialog.invalidFormSettings"), 
					e.getMessage());
			return;
		}
		
		callback.onConfirm(ret);
		close();
	}

	
	interface Callback
	{
		public void onConfirm(AttributeStatement newStatement);
	}

}
