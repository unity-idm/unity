/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupdetails;

import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.webadmin.attributeclass.ACTwinColSelect;
import pl.edu.icm.unity.webadmin.attributeclass.AbstractAttributesClassesDialog;
import pl.edu.icm.unity.webadmin.attributeclass.EffectiveAttrClassViewer;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;


/**
 * Dialog allowing to edit group's {@link AttributesClass}es assigned in a particular group.
 * 
 * @author K. Benedyczak
 */
public class GroupAttributesClassesDialog extends AbstractAttributesClassesDialog
{
	private Callback callback;
	
	public GroupAttributesClassesDialog(UnityMessageSource msg, String group, 
			AttributesManagement attrMan, GroupsManagement groupsMan, Callback callback)
	{
		super(msg, group, attrMan, groupsMan, msg.getMessage("GroupAttributesClasses.caption"));
		this.callback = callback;
	}

	@Override
	protected Component getContents() throws EngineException
	{
		Label info = new Label(msg.getMessage("GroupAttributesClasses.groupInfo", groupPath));
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
		FormLayout extra = new CompactFormLayout();
		extraInfo.setContent(extra);
		
		effective = new EffectiveAttrClassViewer(msg);
		
		try
		{
			loadData();
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg, msg.getMessage("EntityAttributesClasses.errorloadingData"), e);
			throw e;
		}
		extra.addComponents(effective);
		
		FormLayout main = new CompactFormLayout(info, acs, new Label(""), extraInfo);
		return main;
	}

	private void loadData() throws EngineException
	{
		loadACsData();
		
		Group group = groupsMan.getContents(groupPath, GroupContents.METADATA).getGroup();
		acs.setValue(group.getAttributesClasses());
		updateEffective();
	}
	
	@Override
	protected void onConfirm()
	{
		@SuppressWarnings("unchecked")
		Set<String> selected = (Set<String>) acs.getValue();
		try
		{
			Group group = groupsMan.getContents(groupPath, GroupContents.METADATA).getGroup();
			group.setAttributesClasses(selected);
			groupsMan.updateGroup(groupPath, group);
			callback.onUpdate(group);
			close();
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg, msg.getMessage("GroupAttributesClasses.errorUpdatingACs"), e);
		}
	}

	public interface Callback
	{
		public void onUpdate(Group updated);
	}
}
