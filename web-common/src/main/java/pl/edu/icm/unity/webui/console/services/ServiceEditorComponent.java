/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services;

import com.vaadin.ui.Component;

/**
 * UI for service edit
 * 
 * @author P.Piernik
 *
 */
public interface ServiceEditorComponent extends Component
{
	public enum ServiceEditorTab
	{
		GENERAL, AUTHENTICATION, CLIENTS, USERS, POLICY_AGREEMENTS, OTHER
	}

	void setActiveTab(String tab);
}
