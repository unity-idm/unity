/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest.console;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.common.groups.MandatoryGroupSelection;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.tabs.GeneralTab;

import java.util.List;
import java.util.Set;

public class UpmanRestServiceEditorGeneralTab extends GeneralTab
{

	private Binder<UpmanRestServiceConfiguration> restBinder;
	private final List<Group> allGroups;


	public UpmanRestServiceEditorGeneralTab(MessageSource msg, EndpointTypeDescription type,
	                                        List<String> usedEndpointsPaths, Set<String> serverContextPaths,
	                                        List<Group> allGroups)
	{
		super(msg, type, usedEndpointsPaths, serverContextPaths);
		this.allGroups = allGroups;
	}

	public void initUI(Binder<DefaultServiceDefinition> serviceBinder,
	                   Binder<UpmanRestServiceConfiguration> restBinder, boolean editMode)
	{
		super.initUI(serviceBinder, editMode);
		this.restBinder = restBinder;
		mainLayout.addComponent(buildGroupSection());
		mainLayout.addComponent(buildCorsSection());
	}

	private Component buildGroupSection()
	{

		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);

		MandatoryGroupSelection rootGroup = new MandatoryGroupSelection(msg);
		rootGroup.setCaption(msg.getMessage("UpmanRestServiceEditorComponent.rootGroup"));
		rootGroup.setWidth(FieldSizeConstans.SHORT_FIELD_WIDTH, FieldSizeConstans.SHORT_FIELD_WIDTH_UNIT);
		rootGroup.setItems(allGroups);
		main.addComponent(rootGroup);
		restBinder.forField(rootGroup).asRequired().bind("rootGroup");

		MandatoryGroupSelection authorizationGroup = new MandatoryGroupSelection(msg);
		authorizationGroup.setCaption(msg.getMessage("UpmanRestServiceEditorComponent.authorizationGroup"));
		authorizationGroup.setWidth(FieldSizeConstans.SHORT_FIELD_WIDTH, FieldSizeConstans.SHORT_FIELD_WIDTH_UNIT);
		authorizationGroup.setItems(allGroups);
		main.addComponent(authorizationGroup);
		restBinder.forField(authorizationGroup).asRequired().bind("authorizationGroup");

		return new CollapsibleLayout(
				msg.getMessage("UpmanRestServiceEditorComponent.groups"), main);
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
