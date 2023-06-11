/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.groupbrowser;

import com.vaadin.server.UserError;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

class GroupAddDialog extends AbstractDialog
{
	private Callback callback;
	private TextField name;
	private I18nTextField displayedName;
	private I18nTextArea description;
	private CheckBox isPublic;
	private String parent;
	

	GroupAddDialog(MessageSource msg, Group group, Callback callback) 
	{
		super(msg, msg.getMessage("GroupEditDialog.createCaption"),
				msg.getMessage("ok"),
				msg.getMessage("cancel"));
		this.parent = group.toString();
		this.callback = callback;
		setSizeEm(50, 30);
	}

	@Override
	protected Component getContents()
	{
		FormLayout fl = new CompactFormLayout();
		fl.setMargin(true);
		fl.setSpacing(true);
		
		name = new TextField(msg.getMessage("GroupEditDialog.groupName"));
		fl.addComponent(name);
		
		displayedName = new I18nTextField(msg, msg.getMessage("displayedNameF"));
		description = new I18nTextArea(msg, msg.getMessage("GroupEditDialog.groupDesc"));
		
		isPublic = new CheckBox(msg.getMessage("GroupEditDialog.public"));
		
		fl.addComponents(displayedName, description, isPublic);
		name.focus();
		return fl;
	}

	@Override
	protected void onConfirm()
	{
		try
		{
			String gName = name.getValue();
			Group group = gName.equals("/") ? new Group("/") : new Group(new Group(parent), gName);
			group.setDescription(description.getValue());
			I18nString dispName = displayedName.getValue();
			dispName.setDefaultValue(group.toString());
			group.setDisplayedName(dispName);
			group.setPublic(isPublic.getValue());
			close();
			callback.onConfirm(group);
		}
		catch (Exception e)
		{
			name.setComponentError(new UserError(
					msg.getMessage("GroupEditDialog.invalidGroup")));
		}
	}
	
	interface Callback 
	{
		public void onConfirm(Group newGroup);
	}
}
