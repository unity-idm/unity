/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.console.views.directory_setup.attribute_classes.AbstractAttributesClassesDialog;
import io.imunity.console.views.directory_setup.attribute_classes.EffectiveAttrClassViewer;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.GenericElementsTable;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class EntityAttributesClassesDialog extends AbstractAttributesClassesDialog
{
	private final EntityWithLabel entity;
	private final Runnable callback;
	private final NotificationPresenter notificationPresenter;
	private GenericElementsTable<String> groupAcs;

	EntityAttributesClassesDialog(MessageSource msg, String group, EntityWithLabel entity, 
			AttributeClassManagement attrMan, GroupsManagement groupsMan, Runnable callback,
			NotificationPresenter notificationPresenter)
	{
		super(msg, group, attrMan, groupsMan);
		this.entity = entity;
		this.callback = callback;
		this.notificationPresenter = notificationPresenter;
		setHeader(msg.getMessage("EntityAttributesClasses.caption"));
		setConfirmButton(msg.getMessage("ok"), e -> onConfirm());
		setCancelable(true);
		add(getContents());
	}

	private Component getContents()
	{
		Span info = new Span(msg.getMessage("EntityAttributesClasses.entityInfo", entity, groupPath));
		info.addClassName("u-bold");
		acs = new MultiSelectComboBox<>(msg.getMessage("AttributesClass.availableACs"));
		acs.addValueChangeListener(event -> updateEffective());
		acs.setWidthFull();

		groupAcs = new GenericElementsTable<>(msg.getMessage("EntityAttributesClasses.groupAcs"));
		groupAcs.setWidth(90, Unit.PERCENTAGE);
		groupAcs.setHeight(9, Unit.EM);

		effective = new EffectiveAttrClassViewer(msg);
		effective.setSplitterPosition(40);
		effective.setWidthFull();
		
		try
		{
			loadData();
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("EntityAttributesClasses.errorloadingData"), e.getMessage());
		}

		return new VerticalLayout(info, acs, new Span(msg.getMessage("EntityAttributesClasses.infoPanel")), groupAcs, effective);
	}

	private void loadData() throws EngineException
	{
		loadACsData();
		
		Collection<AttributesClass> curClasses = acMan.getEntityAttributeClasses(
				new EntityParam(entity.getEntity().getId()), groupPath);
		Set<String> currentSel = new HashSet<>(curClasses.size());
		for (AttributesClass ac: curClasses)
			currentSel.add(ac.getName());
		acs.setValue(currentSel);
		
		Group group = groupsMan.getContents(groupPath, GroupContents.METADATA).getGroup();
		groupAcs.setItems(group.getAttributesClasses());
		updateEffective();
	}
	
	private void onConfirm()
	{
		Set<String> selected = acs.getValue();
		try
		{
			acMan.setEntityAttributeClasses(new EntityParam(entity.getEntity().getId()), 
					groupPath, selected);
			callback.run();
			close();
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("EntityAttributesClasses.errorUpdatingACs"), e.getMessage());
		}
	}
}
