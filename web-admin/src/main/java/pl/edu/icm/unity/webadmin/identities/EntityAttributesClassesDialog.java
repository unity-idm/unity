/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;

import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.webadmin.attributeclass.ACTwinColSelect;
import pl.edu.icm.unity.webadmin.attributeclass.AbstractAttributesClassesDialog;
import pl.edu.icm.unity.webadmin.attributeclass.EffectiveAttrClassViewer;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;


/**
 * Dialog allowing to edit entity's {@link AttributesClass}es assigned in a particular group.
 * <p>
 * Additionally for completeness relevant group's ACs are also shown, together the effective attribute rules.
 * 
 * @author K. Benedyczak
 */
public class EntityAttributesClassesDialog extends AbstractAttributesClassesDialog
{
	private EntityWithLabel entity;
	private GenericElementsTable<String> groupAcs;
	private Runnable callback;
	
	public EntityAttributesClassesDialog(UnityMessageSource msg, String group, EntityWithLabel entity, 
			AttributeClassManagement attrMan, GroupsManagement groupsMan, Runnable callback)
	{
		super(msg, group, attrMan, groupsMan, msg.getMessage("EntityAttributesClasses.caption"));
		this.entity = entity;
		this.callback = callback;
	}

	@Override
	protected Component getContents() throws EngineException
	{
		Label info = new Label(msg.getMessage("EntityAttributesClasses.entityInfo", entity, groupPath));
		info.setStyleName(Styles.bold.toString());
		
		acs = new ACTwinColSelect(msg.getMessage("AttributesClass.availableACs"),
				msg.getMessage("AttributesClass.selectedACs"));
		acs.addValueChangeListener(event -> updateEffective());
		
		Panel extraInfo = new SafePanel(msg.getMessage("EntityAttributesClasses.infoPanel"));
		extraInfo.addStyleName(Styles.vBorderLess.toString());
		FormLayout extra = new CompactFormLayout();
		extraInfo.setContent(extra);
		
		groupAcs = new GenericElementsTable<>(msg.getMessage("EntityAttributesClasses.groupAcs"));
		groupAcs.setWidth(90, Unit.PERCENTAGE);
		groupAcs.setHeight(9, Unit.EM);

		effective = new EffectiveAttrClassViewer(msg);
		
		try
		{
			loadData();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("EntityAttributesClasses.errorloadingData"), e);
			throw e;
		}
		extra.addComponents(groupAcs, effective);
		
		FormLayout main = new CompactFormLayout(info, acs, new Label(""), extraInfo);
		return main;
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
	
	@Override
	protected void onConfirm()
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
			NotificationPopup.showError(msg, msg.getMessage("EntityAttributesClasses.errorUpdatingACs"), e);
		}
	}
}
