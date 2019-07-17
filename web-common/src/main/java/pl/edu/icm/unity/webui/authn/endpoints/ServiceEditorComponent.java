/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.authn.endpoints;

import com.vaadin.ui.Component;

public interface ServiceEditorComponent extends Component
{
	public enum ServiceEditorTab { GENERAL, AUTHENTICATION}
	void setActiveTab(ServiceEditorTab tab);
}
