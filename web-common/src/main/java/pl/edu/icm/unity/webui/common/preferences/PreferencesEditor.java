/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.preferences;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Implementation provides an UI allowing for editing preferences. 
 * @author K. Benedyczak
 */
public interface PreferencesEditor
{
	/**
	 * @return component displaying the initial state of the preference and allowing to edit them
	 */
	public Component getComponent();
	
	/**
	 * @return current value of the preferences
	 * @throws FormValidationException if the form state is invalid.
	 */
	public String getValue() throws FormValidationException;
	
	/**
	 * Registers a listener, which is used to notify owner that preferences were updated in the editor.
	 * @param listener
	 */
	public void setChangeListener(ModificationListener listener);
	
	public interface ModificationListener
	{
		public void preferencesModified();
	}
}
