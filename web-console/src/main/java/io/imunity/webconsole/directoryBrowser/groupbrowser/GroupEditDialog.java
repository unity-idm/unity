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

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

class GroupEditDialog extends AbstractDialog
{
	private Callback callback;
	private TextField path;
	private I18nTextField displayedName;
	private I18nTextArea description;
	private CheckBox isPublic;
	private Group originalGroup;
	

	GroupEditDialog(MessageSource msg, Group group, Callback callback) 
	{
		super(msg, msg.getMessage("GroupEditDialog.editCaption"),
				msg.getMessage("ok"),
				msg.getMessage("cancel"));
		this.originalGroup = group;
		this.callback = callback;
		setSizeEm(50, 30);
	}

	@Override
	protected Component getContents()
	{
		FormLayout fl = new CompactFormLayout();
		fl.setMargin(true);
		fl.setSpacing(true);
		
		path = new TextField(msg.getMessage("GroupEditDialog.groupPath"));
		path.setValue(originalGroup.getPathEncoded());
		path.setReadOnly(true);
		path.setWidthFull();
		fl.addComponent(path);
		
		displayedName = new I18nTextField(msg, msg.getMessage("displayedNameF"));
		displayedName.setValue(originalGroup.getDisplayedName());
		
		description = new I18nTextArea(msg, msg.getMessage("GroupEditDialog.groupDesc"));
		description.setValue(originalGroup.getDescription());
		
		isPublic = new CheckBox(msg.getMessage("GroupEditDialog.public"));
		isPublic.setValue(originalGroup.isPublic());
		
		fl.addComponents(displayedName, description, isPublic);
		description.focus();
		return fl;
	}

	@Override
	protected void onConfirm()
	{
		try
		{
			Group group = originalGroup.clone();
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
			path.setComponentError(new UserError(
					msg.getMessage("GroupEditDialog.invalidGroup")));
		}
	}
	
	interface Callback 
	{
		public void onConfirm(Group newGroup);
	}
}
