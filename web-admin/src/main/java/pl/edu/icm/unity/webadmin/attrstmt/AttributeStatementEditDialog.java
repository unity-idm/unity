/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attrstmt;

import java.util.Collection;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeStatement2;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

import com.vaadin.ui.Component;

/**
 * Allows to create/edit/view an attribute statement of a group.
 * @author K. Benedyczak
 */
public class AttributeStatementEditDialog extends AbstractDialog
{
	private AttributeStatement2 statement;
	private AttributesManagement attrsMan;
	private final Callback callback;
	private final String group;
	
	private AttributeStatementComponent component;

	private AttributeHandlerRegistry attrHandlerRegistry;
	private GroupsManagement groupsMan;
	
	/**
	 * @param msg
	 * @param attributeStatement attribute statement to be displayed initially or 
	 * null when a new statement should be created
	 */
	public AttributeStatementEditDialog(UnityMessageSource msg, AttributeStatement2 attributeStatement,
			AttributesManagement attrsMan, String group, AttributeHandlerRegistry attrHandlerRegistry, 
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

	private void setInitialData(AttributeStatement2 attributeStatement)
	{
		component.setInitialData(attributeStatement);
	}

	@Override
	protected void onConfirm()
	{
		AttributeStatement2 ret;
		try
		{
			ret = component.getStatementFromComponent();
		} catch (FormValidationException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("AttributeStatementEditDialog.invalidFormSettings"), 
					e.getMessage());
			return;
		}
		
		callback.onConfirm(ret);
		close();
	}

	
	public interface Callback
	{
		public void onConfirm(AttributeStatement2 newStatement);
	}

}
