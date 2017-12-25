/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupdetails;

import java.util.Collection;

import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.webadmin.groupdetails.GroupAttributesClassesDialog.Callback;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.GenericElementsTable2;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler2;

/**
 * Table with group {@link AttributesClass}es with possibility to active edit dialog.
 * @author K. Benedyczak
 */
public class GroupAttributesClassesTable extends GenericElementsTable2<String>
{
	private UnityMessageSource msg;
	private GroupsManagement groupsManagement;
	private AttributeClassManagement acMan;
	private Group group;
	private EventsBus bus;
	
	public GroupAttributesClassesTable(UnityMessageSource msg, GroupsManagement groupsManagement, 
			AttributeClassManagement attrMan)
	{
		super(msg.getMessage("GroupDetails.groupAcs"), s -> s, false);
		this.msg = msg;
		this.acMan = attrMan;
		this.groupsManagement = groupsManagement;
		this.bus = WebSession.getCurrent().getEventBus();
		addActionHandler(getEditAction());		
	}


	public void setInput(Group group)
	{
		this.group = group;
		setInput(group.getAttributesClasses());
	
	}

	private SingleActionHandler2<String> getEditAction()
	{
		return SingleActionHandler2.builder4Edit(msg, String.class).dontRequireTarget()
				.withIcon(Images.attributes.getResource())
				.withHandler(this::showEditDialog).build();
	}
	
	private void showEditDialog(Collection<String> target)
	{
		GroupAttributesClassesDialog dialog = new GroupAttributesClassesDialog(msg, 
				group.toString(), acMan, groupsManagement, new Callback()
				{
					@Override
					public void onUpdate(Group updated)
					{
						bus.fireEvent(new GroupChangedEvent(group.toString()));
					}
				});
		dialog.show();
	}
}
