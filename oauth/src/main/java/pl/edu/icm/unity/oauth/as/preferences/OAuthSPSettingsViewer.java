/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.preferences;

import java.util.Iterator;

import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

/**
 * Shows a single {@link OAuthClientSettings}.
 * 
 * @author K. Benedyczak
 */
public class OAuthSPSettingsViewer extends FormLayout
{
	protected UnityMessageSource msg;
	protected Label autoConfirm;
	protected Label defaultIdentity;
	
	
	public OAuthSPSettingsViewer(UnityMessageSource msg)
	{
		this.msg = msg;
		
		setSpacing(true);
		setMargin(true);
		autoConfirm = new Label();
		autoConfirm.setCaption(msg.getMessage("OAuthPreferences.autoConfirm"));
		defaultIdentity = new Label();
		defaultIdentity.setCaption(msg.getMessage("OAuthPreferences.defaultIdentity"));
		
		addComponents(autoConfirm, defaultIdentity);
	}
	
	public void setInput(OAuthClientSettings spSettings)
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
				autoConfirm.setValue(msg.getMessage("OAuthPreferences.accept"));
			else
				autoConfirm.setValue(msg.getMessage("OAuthPreferences.decline"));
		} else
			autoConfirm.setValue(msg.getMessage("no"));
		
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
