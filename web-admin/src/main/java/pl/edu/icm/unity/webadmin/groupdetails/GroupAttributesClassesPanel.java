/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupdetails;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.Reindeer;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webadmin.groupdetails.GroupAttributesClassesDialog.Callback;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Table with group {@link AttributesClass}es with possibility to active edit dialog.
 * @author K. Benedyczak
 */
public class GroupAttributesClassesPanel extends HorizontalLayout
{
	private UnityMessageSource msg;
	private GroupsManagement groupsManagement;
	private AttributesManagement attrMan;
	private Table table;
	private Button edit;
	private Group group;
	
	public GroupAttributesClassesPanel(UnityMessageSource msg, GroupsManagement groupsManagement, 
			AttributesManagement attrMan)
	{
		this.msg = msg;
		this.attrMan = attrMan;
		this.groupsManagement = groupsManagement;
		table = new Table();
		table.addContainerProperty(msg.getMessage("GroupDetails.groupAcs"), 
				String.class, null);
		table.setWidth(90, Unit.PERCENTAGE);
		table.setHeight(9, Unit.EM);
		table.addActionHandler(new EditHandler());
		edit = new Button();
		edit.setStyleName(Reindeer.BUTTON_SMALL);
		edit.setIcon(Images.attributes.getResource());
		edit.setDescription(msg.getMessage("GroupDetails.editDescription"));
		edit.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				showEditor();
			}
		});
		setWidth(100, Unit.PERCENTAGE);
		setSpacing(true);
		addComponents(table, edit);
		setComponentAlignment(edit, Alignment.TOP_LEFT);
		setExpandRatio(table, 1.0f);
	}
	
	public void setInput(Group group)
	{
		this.group = group;
		table.removeAllItems();
		if (group == null)
			return;
		for (String ac: group.getAttributesClasses())
			table.addItem(new String[]{ac}, ac);
	}
	
	private void showEditor()
	{
		GroupAttributesClassesDialog dialog = new GroupAttributesClassesDialog(msg, 
				group.toString(), attrMan, groupsManagement, new Callback()
				{
					@Override
					public void onUpdate(Group updated)
					{
						setInput(updated);
					}
				});
		dialog.show();
	}
	
	private class EditHandler extends SingleActionHandler
	{
		public EditHandler()
		{
			super(msg.getMessage("GroupDetails.editACAction"), 
					Images.attributes.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			showEditor();
		}
	}

}
