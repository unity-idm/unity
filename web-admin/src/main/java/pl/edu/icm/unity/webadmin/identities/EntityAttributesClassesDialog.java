/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.webadmin.attributeclass.ACTwinColSelect;
import pl.edu.icm.unity.webadmin.attributeclass.AbstractAttributesClassesDialog;
import pl.edu.icm.unity.webadmin.attributeclass.EffectiveAttrClassViewer;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.SafePanel;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;


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
	private Table groupAcs;
	private Callback callback;
	
	public EntityAttributesClassesDialog(UnityMessageSource msg, String group, EntityWithLabel entity, 
			AttributesManagement attrMan, GroupsManagement groupsMan, Callback callback)
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
		acs.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				updateEffective();
			}
		});
		
		Panel extraInfo = new SafePanel(msg.getMessage("EntityAttributesClasses.infoPanel"));
		FormLayout extra = new FormLayout();
		extraInfo.setContent(extra);
		
		groupAcs = new Table();
		groupAcs.addContainerProperty(msg.getMessage("EntityAttributesClasses.groupAcs"), 
				String.class, null);
		groupAcs.setWidth(90, Unit.PERCENTAGE);
		groupAcs.setHeight(9, Unit.EM);

		effective = new EffectiveAttrClassViewer(msg);
		
		try
		{
			loadData();
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg, msg.getMessage("EntityAttributesClasses.errorloadingData"), e);
			throw e;
		}
		extra.addComponents(groupAcs, effective);
		
		FormLayout main = new FormLayout(info, acs, new Label(""), extraInfo);
		return main;
	}

	private void loadData() throws EngineException
	{
		loadACsData();
		
		Collection<AttributesClass> curClasses = attrMan.getEntityAttributeClasses(
				new EntityParam(entity.getEntity().getId()), groupPath);
		Set<String> currentSel = new HashSet<>(curClasses.size());
		for (AttributesClass ac: curClasses)
			currentSel.add(ac.getName());
		acs.setValue(currentSel);
		
		Group group = groupsMan.getContents(groupPath, GroupContents.METADATA).getGroup();
		for (String gac: group.getAttributesClasses())
			groupAcs.addItem(new String[] {gac}, gac);
		updateEffective();
	}
	
	@Override
	protected void onConfirm()
	{
		@SuppressWarnings("unchecked")
		Set<String> selected = (Set<String>) acs.getValue();
		try
		{
			attrMan.setEntityAttributeClasses(new EntityParam(entity.getEntity().getId()), 
					groupPath, selected);
			callback.onChange();
			close();
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg, msg.getMessage("EntityAttributesClasses.errorUpdatingACs"), e);
		}
	}
	
	public interface Callback
	{
		public void onChange();
	}

}
