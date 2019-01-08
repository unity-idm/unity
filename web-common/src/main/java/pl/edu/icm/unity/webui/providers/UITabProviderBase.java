/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
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
	String getId();
	 
	/**
	  * Get main component of UI
	  * @return
	  */
	Component getUI();
	
	/**
	 * Get label key
	 * @return
	 */
	String getLabelKey();
	
	/**
	 * Get description key
	 * @return
	 */
	String getDescriptionKey();

}
