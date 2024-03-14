/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services;

import com.vaadin.flow.component.Component;

/**
 * UI for service edit
 * 
 * @author P.Piernik
 *
 */
public interface ServiceEditorComponent 
{
	public enum ServiceEditorTab
	{
		GENERAL, AUTHENTICATION, CLIENTS, USERS, POLICY_AGREEMENTS, OTHER
	}

	public abstract void setActiveTab(String tab);
	
	public Component getComponent();
}
