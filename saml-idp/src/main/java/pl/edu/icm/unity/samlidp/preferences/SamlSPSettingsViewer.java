/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.preferences;

import java.util.Iterator;
import java.util.Set;

import pl.edu.icm.unity.samlidp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;

/**
 * Shows a single {@link SPSettings}.
 * 
 * @author K. Benedyczak
 */
public class SamlSPSettingsViewer extends FormLayout
{
	protected UnityMessageSource msg;
	protected Label autoConfirm;
	protected ListSelect hiddenAttributes;
	protected Label defaultIdentity;
	
	
	public SamlSPSettingsViewer(UnityMessageSource msg)
	{
		this.msg = msg;
		
		setSpacing(true);
		setMargin(true);
		autoConfirm = new Label();
		autoConfirm.setCaption(msg.getMessage("SAMLPreferences.autoConfirm"));
		defaultIdentity = new Label();
		defaultIdentity.setCaption(msg.getMessage("SAMLPreferences.defaultIdentity"));
		hiddenAttributes = new ListSelect(msg.getMessage("SAMLPreferences.hiddenAttributes"));
		hiddenAttributes.setWidth(90, Unit.PERCENTAGE);
		hiddenAttributes.setRows(6);
		hiddenAttributes.setNullSelectionAllowed(false);
		
		addComponents(autoConfirm, defaultIdentity, hiddenAttributes);
	}
	
	public void setInput(SPSettings spSettings)
	{
		if (spSettings == null)
		{
			setVisibleRec(false);
			return;
		}
		setVisibleRec(true);
		
		if (spSettings.isDoNotAsk())
		{
			if (spSettings.isDefaultAccept())
				autoConfirm.setValue(msg.getMessage("SAMLPreferences.accept"));
			else
				autoConfirm.setValue(msg.getMessage("SAMLPreferences.decline"));
		} else
			autoConfirm.setValue(msg.getMessage("no"));
		
		hiddenAttributes.setReadOnly(false);
		hiddenAttributes.removeAllItems();
		Set<String> hidden = spSettings.getHiddenAttribtues();
		if (hidden.size() > 0)
		{
			for (String h: hidden)
			{
				if (h.equals(SamlPreferences.SYMBOLIC_GROUP_ATTR))
					hiddenAttributes.addItem(msg.getMessage("SAMLPreferences.groupMembershipAttribute"));
				else
					hiddenAttributes.addItem(h);
			}
			hiddenAttributes.setVisible(true);
		} else
		{
			hiddenAttributes.setVisible(false);
		}
		hiddenAttributes.setReadOnly(true);
		
		String selIdentity = spSettings.getSelectedIdentity();
		if (selIdentity != null)
		{
			defaultIdentity.setValue(selIdentity);
			defaultIdentity.setVisible(true);
		} else
			defaultIdentity.setVisible(false);
	}
	
	private void setVisibleRec(boolean how)
	{
		Iterator<Component> children = iterator();
		while (children.hasNext())
		{
			Component c = children.next();
			c.setVisible(how);
		}
	}
}
