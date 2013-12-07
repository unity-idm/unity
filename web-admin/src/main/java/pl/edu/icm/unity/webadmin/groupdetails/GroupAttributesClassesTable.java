/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupdetails;

import com.vaadin.ui.Table;

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
public class GroupAttributesClassesTable extends Table
{
	private UnityMessageSource msg;
	private GroupsManagement groupsManagement;
	private AttributesManagement attrMan;
	private Group group;
	private SingleActionHandler[] handlers;
	
	public GroupAttributesClassesTable(UnityMessageSource msg, GroupsManagement groupsManagement, 
			AttributesManagement attrMan)
	{
		this.msg = msg;
		this.attrMan = attrMan;
		this.groupsManagement = groupsManagement;
		addContainerProperty(msg.getMessage("GroupDetails.groupAcs"), 
				String.class, null);
		handlers = new SingleActionHandler [] {new EditHandler()};
		addActionHandler(handlers[0]);
		setWidth(100, Unit.PERCENTAGE);
	}
	
	public SingleActionHandler[] getHandlers()
	{
		return handlers;
	}

	public void setInput(Group group)
	{
		this.group = group;
		removeAllItems();
		if (group == null)
			return;
		for (String ac: group.getAttributesClasses())
			addItem(new String[]{ac}, ac);
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
