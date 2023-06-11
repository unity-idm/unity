/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.groupbrowser;

import java.util.Set;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;

import io.imunity.webconsole.directorySetup.attributeClasses.ACTwinColSelect;
import io.imunity.webconsole.directorySetup.attributeClasses.AbstractAttributesClassesDialog;
import io.imunity.webconsole.directorySetup.attributeClasses.EffectiveAttrClassViewer;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;


/**
 * Dialog allowing to edit group's {@link AttributesClass}es assigned in a particular group.
 * 
 * @author K. Benedyczak
 */
class GroupAttributesClassesDialog extends AbstractAttributesClassesDialog
{
	private Callback callback;
	
	GroupAttributesClassesDialog(MessageSource msg, String group, 
			AttributeClassManagement acMan, GroupsManagement groupsMan, Callback callback)
	{
		super(msg, group, acMan, groupsMan, msg.getMessage("GroupAttributesClasses.caption"));
		this.callback = callback;
	}

	@Override
	protected Component getContents() throws EngineException
	{
		Label info = new Label(msg.getMessage("GroupAttributesClasses.groupInfo", groupPath));
		info.setStyleName(Styles.bold.toString());
		
		acs = new ACTwinColSelect(msg.getMessage("AttributesClass.availableACs"),
				msg.getMessage("AttributesClass.selectedACs"));
		acs.addValueChangeListener(event -> updateEffective());
		
		Panel extraInfo = new SafePanel(msg.getMessage("EntityAttributesClasses.infoPanel"));
		extraInfo.addStyleName(Styles.vBorderLess.toString());
		FormLayout extra = new CompactFormLayout();
		extraInfo.setContent(extra);
		
		effective = new EffectiveAttrClassViewer(msg);
		
		try
		{
			loadData();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("EntityAttributesClasses.errorloadingData"), e);
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
			NotificationPopup.showError(msg, msg.getMessage("GroupAttributesClasses.errorUpdatingACs"), e);
		}
	}

	interface Callback
	{
		void onUpdate(Group updated);
	}
}
