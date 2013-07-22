/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.preferences;

import java.util.Set;

import pl.edu.icm.unity.samlidp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

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
	private UnityMessageSource msg;
	private Label autoConfirm;
	private ListSelect hiddenAttributes;
	private Label defaultIdentity;
	
	
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
		
		addComponents(autoConfirm, defaultIdentity, hiddenAttributes);
	}
	
	public void setInput(SPSettings spSettings)
	{
		if (spSettings == null)
		{
			setVisible(false);
			return;
		}
		setVisible(true);
		
		if (spSettings.isDoNotAsk())
		{
			if (spSettings.isDefaultAccept())
				autoConfirm.setValue(msg.getMessage("SAMLPreferences.accept"));
			else
				autoConfirm.setValue(msg.getMessage("SAMLPreferences.decline"));
		} else
			autoConfirm.setValue(msg.getMessage("no"));
		
		hiddenAttributes.setReadOnly(false);
		Set<String> hidden = spSettings.getHiddenAttribtues();
		if (hidden.size() > 0)
		{
			for (String h: hidden)
				hiddenAttributes.addItem(h);
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
}
