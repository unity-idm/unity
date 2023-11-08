/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import io.imunity.console.tprofile.AttributeSelectionComboBox;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;

import java.util.Collection;


class AddAttributeColumnDialog extends ConfirmDialog
{
	private final AttributeTypeManagement attrsMan;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final Callback callback;

	private ComboBox<AttributeType> attributeType;
	private Checkbox useRootGroup;
	
	
	AddAttributeColumnDialog(MessageSource msg, AttributeTypeManagement attrsMan, 
			Callback callback, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
		this.attrsMan = attrsMan;
		this.callback = callback;
		setHeader(msg.getMessage("AddAttributeColumnDialog.caption"));
		setConfirmButton(msg.getMessage("ok"), e -> onConfirm());
		setCancelable(true);
		setWidth("40em");
		setHeight("20em");
		add(getContents());
	}

	private FormLayout getContents()
	{
		Span info = new Span(msg.getMessage("AddAttributeColumnDialog.info"));
		Collection<AttributeType> attrTypes;
		try
		{
			attrTypes = attrsMan.getAttributeTypes();
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("error"),
					msg.getMessage("AddAttributeColumnDialog.cantGetAttrTypes"));
			throw new IllegalStateException();
		}
		attributeType = new AttributeSelectionComboBox(msg.getMessage("AddAttributeColumnDialog.info2"),
				attrTypes, false);
		attributeType.setWidthFull();
		
		useRootGroup = new Checkbox(msg.getMessage("AddAttributeColumnDialog.useRootGroup"), true);
		useRootGroup.setTooltipText(msg.getMessage("AddAttributeColumnDialog.useRootGroupTooltip"));
		
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addFormItem(info, "");
		main.addFormItem(attributeType, msg.getMessage("AddAttributeColumnDialog.attribute"));
		main.addFormItem(useRootGroup, "");
		main.setSizeFull();
		return main;
	}

	private void onConfirm()
	{
		String group = useRootGroup.getValue() ? "/" : null;
		callback.onChosen(attributeType.getValue().getName(), group);
		close();
	}
	
	interface Callback 
	{
		void onChosen(String attributeType, String group);
	}
}
