/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.console.views.directory_browser.group_browser.GroupManagementHelper;
import io.imunity.console.views.directory_browser.group_details.GroupComboBox;
import io.imunity.vaadin.elements.DialogWithActionFooter;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.GroupsManagement;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;

@Component
class AddToGroupHandler
{
	private final GroupManagementHelper groupManagementHelper;
	private final GroupsManagement groupsManagement;
	private final MessageSource msg;
	
	AddToGroupHandler(GroupManagementHelper groupManagementHelper, GroupsManagement groupsManagement,
			MessageSource msg)
	{
		this.groupManagementHelper = groupManagementHelper;
		this.groupsManagement = groupsManagement;
		this.msg = msg;
	}
	
	SingleActionHandler<IdentityEntry> getAction()
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("AddToGroupHandler.action"))
				.withIcon(VaadinIcon.PLUS_CIRCLE_O)
				.withHandler(this::showAddToGroupDialog)
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
			.open();
	}
	
	private class TargetGroupSelectionDialog extends DialogWithActionFooter
	{
		private final Consumer<String> selectionConsumer;
		private GroupComboBox groupSelection;

		TargetGroupSelectionDialog(MessageSource msg, Consumer<String> selectionConsumer)
		{
			super(msg::getMessage);
			this.selectionConsumer = selectionConsumer;
			setHeaderTitle(msg.getMessage("AddToGroupHandler.caption"));
			setActionButton(msg.getMessage("ok"), this::onConfirm);
			setWidth("30em");
			setHeight("18em");
			add(getContents());
		}

		private FormLayout getContents()
		{
			groupSelection = new GroupComboBox(msg.getMessage("AddToGroupHandler.info"),
					groupsManagement);
			groupSelection.setInput("/", false);
			groupSelection.setWidthFull();
			FormLayout main = new FormLayout();
			main.addFormItem(groupSelection, msg.getMessage("AddToGroupHandler.selectGroup"))
					.getStyle().set("--vaadin-form-item-label-width", "3em");
			main.setSizeFull();
			return main;
		}

		private void onConfirm()
		{
			selectionConsumer.accept(groupSelection.getValue());
			close();
		}
	}
}
