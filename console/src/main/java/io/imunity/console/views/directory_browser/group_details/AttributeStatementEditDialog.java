/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.group_details;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.Collection;

class AttributeStatementEditDialog extends ConfirmDialog
{
	private final AttributeHandlerRegistry attrHandlerRegistry;
	private final GroupsManagement groupsMan;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final AttributeStatement statement;
	private final AttributeTypeManagement attrsMan;
	private final Callback callback;
	private final String group;

	private AttributeStatementComponent component;

	AttributeStatementEditDialog(MessageSource msg, AttributeStatement attributeStatement,
			AttributeTypeManagement attrsMan, String group, AttributeHandlerRegistry attrHandlerRegistry,
			GroupsManagement groupsMan, Callback callback, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.statement = attributeStatement;
		this.attrsMan = attrsMan;
		this.group = group;
		this.callback = callback;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.groupsMan = groupsMan;
		this.notificationPresenter = notificationPresenter;
		setWidth("65em");
		setHeader(msg.getMessage("AttributeStatementEditDialog.caption"));
		setConfirmButton(msg.getMessage("ok"), e -> onConfirm());
		setCancelable(true);
		add(getContents());
	}

	protected Component getContents()
	{
		Collection<AttributeType> attributeTypes;
		
		try
		{
			attributeTypes = attrsMan.getAttributeTypes();
		} catch(Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AttributeStatementEditDialog.cantReadAttributeTypes"), e.getMessage());
			return new Div();
		}

		component = new AttributeStatementComponent(
				msg, groupsMan, attributeTypes, attrHandlerRegistry, group, notificationPresenter);
		
		if (statement != null)
			setInitialData(statement);
		return component;
	}

	private void setInitialData(AttributeStatement attributeStatement)
	{
		component.setInitialData(attributeStatement);
	}

	protected void onConfirm()
	{
		AttributeStatement ret;
		try
		{
			ret = component.getStatementFromComponent();
		} catch (FormValidationException e)
		{
			open();
			notificationPresenter.showError(msg.getMessage("AttributeStatementEditDialog.invalidFormSettings"),
					e.getMessage());
			return;
		}
		
		callback.onConfirm(ret);
	}

	
	interface Callback
	{
		void onConfirm(AttributeStatement newStatement);
	}

}