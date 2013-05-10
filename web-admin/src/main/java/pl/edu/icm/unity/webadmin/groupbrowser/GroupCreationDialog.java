/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupbrowser;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.AbstractDialog;

import com.vaadin.server.UserError;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

/**
 * Asks about group name and description, returns output in the callback.
 * @author K. Benedyczak
 */
public class GroupCreationDialog extends AbstractDialog
{
	private static final long serialVersionUID = 1L;
	private Callback callback;
	private TextField name;
	private TextArea description;
	private Group parent;

	public GroupCreationDialog(UnityMessageSource msg, Group parent, Callback callback) 
	{
		super(msg, msg.getMessage("GroupCreationDialog.caption"),
				msg.getMessage("GroupCreationDialog.Create"),
				msg.getMessage("cancel"));
		this.parent = parent;
		this.callback = callback;
		this.defaultSizeUndfined = true;
	}

	@Override
	protected Component getContents()
	{
		FormLayout fl = new FormLayout();
		fl.setMargin(true);
		fl.setSpacing(true);
		
		name = new TextField(msg.getMessage("GroupCreationDialog.groupName"));
		fl.addComponent(name);
		description = new TextArea(msg.getMessage("GroupCreationDialog.groupDesc"));
		fl.addComponent(description);
		name.focus();
		return fl;
	}

	@Override
	protected void onConfirm()
	{
		try
		{
			Group group = new Group(parent, name.getValue());
			group.setDescription(description.getValue());
			close();
			callback.onGroupCreate(group);
		} catch (Exception e)
		{
			name.setComponentError(new UserError(
					msg.getMessage("GroupCreationDialog.invalidGroup")));
		}
	}
	
	public interface Callback 
	{
		public void onGroupCreate(Group toBeCreated);
	}
}
