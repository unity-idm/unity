/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.preferences;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.common.preferences.PreferencesHandlerRegistry;

/**
 * Allows for viewing and editing of preferences of the logged user.
 * @author K. Benedyczak
 */
public class PreferencesComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private PreferencesHandlerRegistry registry;
	private PreferencesManagement prefMan;
	private EndpointManagement endpMan;

	public PreferencesComponent(UnityMessageSource msg, PreferencesHandlerRegistry registry,
			PreferencesManagement prefMan, EndpointManagement endpMan)
	{
		super();
		this.msg = msg;
		this.registry = registry;
		this.prefMan = prefMan;
		this.endpMan = endpMan;
		init();
	}

	private void init()
	{
		TabSheet tabs = new TabSheet();
		
		Set<String> deployedTypes = null;
		try
		{
			List<ResolvedEndpoint> deployed = endpMan.getEndpoints();
			deployedTypes = new HashSet<String>();
			for (ResolvedEndpoint desc: deployed)
				deployedTypes.add(desc.getType().getName());
		} catch (EngineException e)
		{
			//no authz - no problem, just add all preferences
		}
		
		Set<String> types = registry.getSupportedPreferenceTypes(deployedTypes);
		for (String type: types)
		{
			PreferenceViewTab tab = new PreferenceViewTab(msg, registry.getHandler(type), prefMan);
			tabs.addTab(tab);
		}
		setMargin(false);
		setSpacing(false);
		tabs.setSizeFull();
		addComponent(tabs);
	}
}
