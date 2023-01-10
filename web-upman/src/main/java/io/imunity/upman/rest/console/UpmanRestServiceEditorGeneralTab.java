/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest.console;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.tabs.GeneralTab;

import java.util.List;
import java.util.Set;

public class UpmanRestServiceEditorGeneralTab extends GeneralTab
{

	private Binder<UpmanRestServiceConfiguration> restBinder;

	public UpmanRestServiceEditorGeneralTab(MessageSource msg, EndpointTypeDescription type,
	                                        List<String> usedEndpointsPaths, Set<String> serverContextPaths)
	{
		super(msg, type, usedEndpointsPaths, serverContextPaths);
	}

	public void initUI(Binder<DefaultServiceDefinition> serviceBinder,
	                   Binder<UpmanRestServiceConfiguration> restBinder, boolean editMode)
	{
		super.initUI(serviceBinder, editMode);
		this.restBinder = restBinder;
		mainLayout.addComponent(buildGroupSection());
	}

	private Component buildGroupSection()
	{

		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);

		ChipsWithTextfield rootGroup = new ChipsWithTextfield(msg);
		rootGroup.setCaption(msg.getMessage("UpmanRestServiceEditorComponent.rootGroup"));
		restBinder.forField(rootGroup).bind("rootGroup");
		main.addComponent(rootGroup);

		ChipsWithTextfield authorizationGroup = new ChipsWithTextfield(msg);
		authorizationGroup.setCaption(msg.getMessage("UpmanRestServiceEditorComponent.authorizationGroup"));
		main.addComponent(authorizationGroup);
		restBinder.forField(authorizationGroup).bind("authorizationGroup");

		return new CollapsibleLayout(
				msg.getMessage("UpmanRestServiceEditorComponent.groups"), main);
	}
}
