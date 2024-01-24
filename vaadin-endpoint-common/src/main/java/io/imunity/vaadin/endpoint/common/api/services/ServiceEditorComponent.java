/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services;

import com.vaadin.flow.component.Component;

/**
 * UI for service edit
 * 
 * @author P.Piernik
 *
 */
public abstract class ServiceEditorComponent extends Component
{
	public enum ServiceEditorTab
	{
		GENERAL, AUTHENTICATION, CLIENTS, USERS, POLICY_AGREEMENTS, OTHER
	}

	public abstract void setActiveTab(String tab);
}
