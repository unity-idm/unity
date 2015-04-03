/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupbrowser;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

import com.vaadin.server.UserError;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

/**
 * Asks about group name and description, returns output in the callback. Useful for group creation and editing.
 * @author K. Benedyczak
 */
public class GroupEditDialog extends AbstractDialog
{
	private static final long serialVersionUID = 1L;
	private Callback callback;
	private TextField name;
	private I18nTextField displayedName;
	private I18nTextArea description;
	private String parent;
	private String originalName;
	private I18nString originalDispName;
	private I18nString originalDesc;

	public GroupEditDialog(UnityMessageSource msg, Group group, boolean edit, Callback callback) 
	{
		super(msg, edit ? msg.getMessage("GroupEditDialog.editCaption") : 
					msg.getMessage("GroupEditDialog.createCaption"),
				msg.getMessage("ok"),
				msg.getMessage("cancel"));
		this.parent = edit ? group.getParentPath() : group.toString();
		this.originalName = edit ? group.getName() : "";
		this.originalDesc = edit ? group.getDescription() : new I18nString();
		this.originalDispName = edit ? group.getDisplayedName() : new I18nString();
		this.callback = callback;
	}

	@Override
	protected Component getContents()
	{
		FormLayout fl = new CompactFormLayout();
		fl.setMargin(true);
		fl.setSpacing(true);
		
		name = new TextField(msg.getMessage("GroupEditDialog.groupName"));
		name.setValue(originalName);
		if (originalName.equals("/"))
			name.setReadOnly(true);
		fl.addComponent(name);
		
		displayedName = new I18nTextField(msg, msg.getMessage("displayedNameF"));
		displayedName.setValue(originalDispName);
		
		description = new I18nTextArea(msg, msg.getMessage("GroupEditDialog.groupDesc"));
		description.setValue(originalDesc);
		fl.addComponents(displayedName, description);
		if (name.isReadOnly())
			description.focus();
		else
			name.focus();
		return fl;
	}

	@Override
	protected void onConfirm()
	{
		try
		{
			String gName = name.getValue();
			Group group = gName.equals("/") ? new Group("/") : new Group(new Group(parent), name.getValue());
			group.setDescription(description.getValue());
			I18nString dispName = displayedName.getValue();
			dispName.setDefaultValue(group.toString());
			group.setDisplayedName(dispName);
			close();
			callback.onConfirm(group);
		} catch (Exception e)
		{
			name.setComponentError(new UserError(
					msg.getMessage("GroupEditDialog.invalidGroup")));
		}
	}
	
	public interface Callback 
	{
		public void onConfirm(Group newGroup);
	}
}
