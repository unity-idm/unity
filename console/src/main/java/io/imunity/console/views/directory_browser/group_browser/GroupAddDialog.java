/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.group_browser;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.vaadin.elements.LocalizedTextAreaDetails;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.HashSet;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

class GroupAddDialog extends ConfirmDialog
{
	private final MessageSource msg;
	private final Callback callback;
	private final String parent;

	private TextField name;
	private FormLayout.FormItem nameFormItem;
	private LocalizedTextFieldDetails displayedName;
	private LocalizedTextAreaDetails description;
	private Checkbox isPublic;

	GroupAddDialog(MessageSource msg, Group group, Callback callback) 
	{
		this.msg = msg;
		this.parent = group.toString();
		this.callback = callback;
		setWidth("50em");
		setHeight("25em");
		setHeader(msg.getMessage("GroupEditDialog.createCaption"));
		setConfirmButton(msg.getMessage("ok"), e -> onConfirm());
		setCancelable(true);
		add(getContents());
	}

	private Component getContents()
	{
		FormLayout fl = new FormLayout();
		fl.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		name = new TextField();
		name.setWidth(TEXT_FIELD_MEDIUM.value());
		nameFormItem = fl.addFormItem(name, msg.getMessage("GroupEditDialog.groupName"));
		name.setRequiredIndicatorVisible(true);
		
		displayedName = new LocalizedTextFieldDetails(new HashSet<>(msg.getEnabledLocales().values()), msg.getLocale());
		displayedName.setWidthFull();
		description = new LocalizedTextAreaDetails(new HashSet<>(msg.getEnabledLocales().values()), msg.getLocale());
		description.setWidthFull();

		isPublic = new Checkbox(msg.getMessage("GroupEditDialog.public"));
		
		fl.addFormItem(displayedName, msg.getMessage("displayedNameF"));
		fl.addFormItem(description, msg.getMessage("GroupEditDialog.groupDesc"));
		fl.addFormItem(isPublic, "");
		return fl;
	}

	private void onConfirm()
	{
		try
		{
			String gName = name.getValue();
			Group group = gName.equals("/") ? new Group("/") : new Group(new Group(parent), gName);
			I18nString description = new I18nString();
			description.addAllMapValues(this.description.getValue());
			group.setDescription(description);
			I18nString dispName = new I18nString();
			dispName.addAllMapValues(displayedName.getValue());
			dispName.setDefaultValue(group.toString());
			group.setDisplayedName(dispName);
			group.setPublic(isPublic.getValue());
			callback.onConfirm(group);
		}
		catch (Exception e)
		{
			name.setInvalid(true);
			name.setErrorMessage(msg.getMessage("GroupEditDialog.invalidGroup"));
			open();
			nameFormItem.getElement().setAttribute("invalid", true);
		}
	}
	
	interface Callback 
	{
		void onConfirm(Group newGroup);
	}
}
