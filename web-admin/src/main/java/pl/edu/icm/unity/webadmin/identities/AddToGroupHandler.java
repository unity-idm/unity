/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webadmin.utils.GroupManagementHelper;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.GroupComboBox;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Action to add user(s) to a given group, which is selected in a dialog. This is alternative to drag'n'drop 
 * way to add entities to a group.
 * 
 * @author K. Benedyczak
 */
@Component
class AddToGroupHandler
{
	private final GroupManagementHelper groupManagementHelper;
	private final GroupsManagement groupsManagement;
	private final UnityMessageSource msg;
	
	@Autowired
	public AddToGroupHandler(GroupManagementHelper groupManagementHelper, GroupsManagement groupsManagement,
			UnityMessageSource msg)
	{
		this.groupManagementHelper = groupManagementHelper;
		this.groupsManagement = groupsManagement;
		this.msg = msg;
	}
	
	public SingleActionHandler<IdentityEntry> getAction()
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("AddToGroupHandler.action"))
				.withIcon(Images.add.getResource())
				.withHandler(selection -> showAddToGroupDialog(selection))
				.multiTarget()
				.build();
	}

	private void showAddToGroupDialog(Set<IdentityEntry> selection)
	{
		HashMap<Long, EntityWithLabel> toAddMap = new HashMap<>();
		for (IdentityEntry node : selection)
			toAddMap.put(node.getSourceEntity().getEntity().getId(), node.getSourceEntity());
		Collection<EntityWithLabel> toAdd = toAddMap.values();
		
		new TargetGroupSelectionDialog(msg, group -> groupManagementHelper.bulkAddToGroup(group, toAdd, false))
			.show();
	}
	
	private class TargetGroupSelectionDialog extends AbstractDialog
	{
		private Consumer<String> selectionConsumer;
		private GroupComboBox groupSelection;

		public TargetGroupSelectionDialog(UnityMessageSource msg, Consumer<String> selectionConsumer)
		{
			super(msg, msg.getMessage("AddToGroupHandler.caption"));
			this.selectionConsumer = selectionConsumer;
			setSizeEm(30, 18);
		}

		@Override
		protected FormLayout getContents()
		{
			Label info = new Label(msg.getMessage("AddToGroupHandler.info"));
			info.setWidth(100, Unit.PERCENTAGE);
			groupSelection = new GroupComboBox(msg.getMessage("AddToGroupHandler.selectGroup"), 
					groupsManagement);
			groupSelection.setInput("/", false);
			groupSelection.setWidth(100, Unit.PERCENTAGE);
			FormLayout main = new CompactFormLayout();
			main.addComponents(info, groupSelection);
			main.setSizeFull();
			return main;
		}

		@Override
		protected void onConfirm()
		{
			selectionConsumer.accept(groupSelection.getValue());
			close();
		}
	}
}
