/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.group_browser;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.console.views.directory_setup.attribute_classes.AbstractAttributesClassesDialog;
import io.imunity.console.views.directory_setup.attribute_classes.EffectiveAttrClassViewer;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.Panel;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;

import java.util.Set;


class GroupAttributesClassesDialog extends AbstractAttributesClassesDialog
{
	private final Callback callback;
	private final NotificationPresenter notificationPresenter;

	GroupAttributesClassesDialog(MessageSource msg, String group, 
			AttributeClassManagement acMan, GroupsManagement groupsMan, Callback callback, NotificationPresenter notificationPresenter)
	{
		super(msg, group, acMan, groupsMan);
		this.callback = callback;
		this.notificationPresenter = notificationPresenter;
		setHeader(msg.getMessage("GroupAttributesClasses.caption"));
		setConfirmButton(msg.getMessage("ok"), e -> onConfirm());
		setCancelable(true);
		add(getContents());
	}

	private Component getContents()
	{
		Span info = new Span(msg.getMessage("GroupAttributesClasses.groupInfo", groupPath));
		info.addClassName("u-bold");

		acs = new MultiSelectComboBox<>(msg.getMessage("AttributesClass.availableACs"),
				msg.getMessage("AttributesClass.selectedACs"));
		acs.addValueChangeListener(event -> updateEffective());
		acs.setWidthFull();
		effective = new EffectiveAttrClassViewer(msg);
		effective.setWidthFull();

		Panel extraInfo = new Panel(msg.getMessage("EntityAttributesClasses.infoPanel"));
		effective.setSplitterPosition(40);
		extraInfo.add(effective);
		extraInfo.setWidthFull();
		extraInfo.setMargin(false);

		try
		{
			loadData();
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("EntityAttributesClasses.errorloadingData"), e.getMessage());
			return new Div();
		}

		return new VerticalLayout(info, acs, extraInfo);
	}

	private void loadData() throws EngineException
	{
		loadACsData();
		
		Group group = groupsMan.getContents(groupPath, GroupContents.METADATA).getGroup();
		acs.setValue(group.getAttributesClasses());
		updateEffective();
	}

	private void onConfirm()
	{
		Set<String> selected = acs.getValue();
		try
		{
			Group group = groupsMan.getContents(groupPath, GroupContents.METADATA).getGroup();
			group.setAttributesClasses(selected);
			groupsMan.updateGroup(groupPath, group, "set attribute classes", "");
			callback.onUpdate(group);
			close();
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("GroupAttributesClasses.errorUpdatingACs"), e.getMessage());
		}
	}

	interface Callback
	{
		void onUpdate(Group updated);
	}
}
