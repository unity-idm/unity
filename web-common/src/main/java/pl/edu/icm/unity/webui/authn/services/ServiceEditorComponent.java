/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.authn.services;

import com.vaadin.ui.Component;

/**
 * 
 * @author P.Piernik
 *
 */
public interface ServiceEditorComponent extends Component
{
	public enum ServiceEditorTab { GENERAL, AUTHENTICATION, CLIENTS, USERS}
	void setActiveTab(ServiceEditorTab tab);
}
