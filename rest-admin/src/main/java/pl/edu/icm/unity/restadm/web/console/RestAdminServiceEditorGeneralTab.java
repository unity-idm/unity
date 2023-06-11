/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.web.console;

import java.util.List;
import java.util.Set;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.tabs.GeneralTab;

/**
 * REST Admin service editor general tab
 * 
 * @author P.Piernik
 *
 */
public class RestAdminServiceEditorGeneralTab extends GeneralTab
{

	private Binder<RestAdminServiceConfiguration> restBinder;

	public RestAdminServiceEditorGeneralTab(MessageSource msg, EndpointTypeDescription type,
			List<String> usedEndpointsPaths, Set<String> serverContextPaths)
	{
		super(msg, type, usedEndpointsPaths, serverContextPaths);
	}

	public void initUI(Binder<DefaultServiceDefinition> serviceBinder,
			Binder<RestAdminServiceConfiguration> restBinder, boolean editMode)
	{
		super.initUI(serviceBinder, editMode);
		this.restBinder = restBinder;
		mainLayout.addComponent(buildCorsSection());
	}

	private Component buildCorsSection()
	{

		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);

		ChipsWithTextfield allowedCORSheaders = new ChipsWithTextfield(msg);
		allowedCORSheaders.setCaption(msg.getMessage("RestAdminServiceEditorComponent.allowedCORSheaders"));
		restBinder.forField(allowedCORSheaders).bind("allowedCORSheaders");
		main.addComponent(allowedCORSheaders);

		ChipsWithTextfield allowedCORSorigins = new ChipsWithTextfield(msg);
		allowedCORSorigins.setCaption(msg.getMessage("RestAdminServiceEditorComponent.allowedCORSorigins"));
		main.addComponent(allowedCORSorigins);
		restBinder.forField(allowedCORSorigins).bind("allowedCORSorigins");

		CollapsibleLayout corsSection = new CollapsibleLayout(
				msg.getMessage("RestAdminServiceEditorComponent.cors"), main);
		return corsSection;
	}
}
