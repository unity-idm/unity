/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupbrowser;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.AbstractDialog;

import com.google.common.html.HtmlEscapers;
import com.vaadin.server.UserError;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextArea;
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
	private TextArea description;
	private String parent;
	private String originalName;
	private String originalDesc;

	public GroupEditDialog(UnityMessageSource msg, Group group, boolean edit, Callback callback) 
	{
		super(msg, edit ? msg.getMessage("GroupEditDialog.editCaption") : 
					msg.getMessage("GroupEditDialog.createCaption"),
				msg.getMessage("ok"),
				msg.getMessage("cancel"));
		this.parent = edit ? group.getParentPath() : group.toString();
		this.originalName = edit ? group.getName() : "";
		this.originalDesc = edit ? group.getDescription() : "";
		this.callback = callback;
	}

	@Override
	protected Component getContents()
	{
		FormLayout fl = new FormLayout();
		fl.setMargin(true);
		fl.setSpacing(true);
		
		name = new TextField(msg.getMessage("GroupEditDialog.groupName"));
		name.setValue(originalName);
		if (originalName.equals("/"))
			name.setReadOnly(true);
		fl.addComponent(name);
		description = new TextArea(msg.getMessage("GroupEditDialog.groupDesc"));
		description.setValue(originalDesc);
		description.setWidth(100, Unit.PERCENTAGE);
		fl.addComponent(description);
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
			group.setDescription(HtmlEscapers.htmlEscaper().escape(description.getValue()));
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
