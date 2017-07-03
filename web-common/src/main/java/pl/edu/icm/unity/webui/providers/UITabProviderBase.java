/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.providers;

import com.vaadin.ui.Component;

/**
 * Base for UI providers
 * @author P.Piernik
 *
 */
public interface UITabProviderBase
{
	/**
	 * 
	 * @return ui id
	 */
	public String getId();
	 
	/**
	  * Get main component of UI
	  * @return
	  */
	public Component getUI();
	
	/**
	 * Get label key
	 * @return
	 */
	public String getLabelKey();
	
	/**
	 * Get description key
	 * @return
	 */
	public String getDescriptionKey();

}
