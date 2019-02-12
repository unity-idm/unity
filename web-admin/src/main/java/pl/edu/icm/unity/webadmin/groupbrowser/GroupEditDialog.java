/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupbrowser;

import com.vaadin.server.UserError;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

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
	private CheckBox isPublic;
	private String parent;
	private String originalName;
	private Group originalGroup;
	private I18nString originalDispName;
	private I18nString originalDesc;
	private boolean originalIsPublic;
	

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
		this.originalIsPublic = edit ? group.isPublic() : false;
		this.originalGroup = edit ? group : null;
		this.callback = callback;
		setSizeMode(SizeMode.MEDIUM);
	}

	@Override
	protected Component getContents()
	{
		FormLayout fl = new CompactFormLayout();
		fl.setMargin(true);
		fl.setSpacing(true);
		
		name = new TextField(msg.getMessage("GroupEditDialog.groupName"));
		name.setValue(originalName);
		if (!originalName.isEmpty())
			name.setReadOnly(true);
		fl.addComponent(name);
		
		displayedName = new I18nTextField(msg, msg.getMessage("displayedNameF"));
		displayedName.setValue(originalDispName);
		
		description = new I18nTextArea(msg, msg.getMessage("GroupEditDialog.groupDesc"));
		description.setValue(originalDesc);
		
		isPublic = new CheckBox(msg.getMessage("GroupEditDialog.public"));
		isPublic.setValue(originalIsPublic);
		
		fl.addComponents(displayedName, description, isPublic);
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
			Group group;
			if (originalGroup != null)
				group = originalGroup.clone();
			else
				group = gName.equals("/") ? new Group("/") : 
					new Group(new Group(parent), gName);
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
	
	public interface Callback 
	{
		public void onConfirm(Group newGroup);
	}
}
