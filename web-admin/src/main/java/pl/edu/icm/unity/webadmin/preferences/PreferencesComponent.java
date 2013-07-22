/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.preferences;

import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.preferences.PreferencesHandlerRegistry;

import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

/**
 * Allows for viewing and editing of preferences of the logged user.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PreferencesComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private PreferencesHandlerRegistry registry;
	private PreferencesManagement prefMan;


	public PreferencesComponent(UnityMessageSource msg, PreferencesHandlerRegistry registry,
			PreferencesManagement prefMan)
	{
		this.msg = msg;
		this.registry = registry;
		this.prefMan = prefMan;
		init();
	}


	private void init()
	{
		TabSheet tabs = new TabSheet();
		Set<String> types = registry.getSupportedPreferenceTypes();
		for (String type: types)
		{
			PreferenceViewTab tab = new PreferenceViewTab(msg, registry.getHandler(type), prefMan);
			tabs.addTab(tab);
		}
		tabs.setSizeFull();
		addComponent(tabs);
	}
}
