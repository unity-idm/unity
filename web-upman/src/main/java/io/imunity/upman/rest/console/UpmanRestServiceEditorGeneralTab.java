/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest.console;

import java.util.List;
import java.util.Set;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;

import io.imunity.tooltip.TooltipExtension;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.common.groups.MandatoryGroupSelection;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.tabs.GeneralTab;

class UpmanRestServiceEditorGeneralTab extends GeneralTab
{

	private Binder<UpmanRestServiceConfiguration> restBinder;
	private final List<Group> allGroups;
	private final List<String> attributes;

	public UpmanRestServiceEditorGeneralTab(MessageSource msg, EndpointTypeDescription type,
	                                        List<String> usedEndpointsPaths, Set<String> serverContextPaths,
	                                        List<Group> allGroups, List<String> attributes)
	{
		super(msg, type, usedEndpointsPaths, serverContextPaths);
		this.allGroups = allGroups;
		this.attributes = attributes;
	}

	public void initUI(Binder<DefaultServiceDefinition> serviceBinder,
	                   Binder<UpmanRestServiceConfiguration> restBinder, boolean editMode)
	{
		super.initUI(serviceBinder, editMode);
		this.restBinder = restBinder;
		mainLayout.addComponent(buildGroupSection());
		mainLayout.addComponent(buildAttributeSection());
		mainLayout.addComponent(buildCorsSection());
	}

	
	private Component buildAttributeSection()
	{
		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);
		ChipsWithDropdown<String> rootGroupAttributes = new ChipsWithDropdown<>();
		rootGroupAttributes.setCaption(msg.getMessage("UpmanRestServiceEditorComponent.rootGroupAttributes"));
		rootGroupAttributes.setItems(attributes);
		rootGroupAttributes.setDescription(msg.getMessage("UpmanRestServiceEditorComponent.rootGroupAttributesDescription"));
		rootGroupAttributes.setWidth(FieldSizeConstans.SHORT_FIELD_WIDTH, FieldSizeConstans.SHORT_FIELD_WIDTH_UNIT);

		TooltipExtension.tooltip(rootGroupAttributes);
		
		main.addComponent(rootGroupAttributes);
		restBinder.forField(rootGroupAttributes).asRequired().bind("rootGroupAttributes");
		CollapsibleLayout attributes = new CollapsibleLayout(msg.getMessage("UpmanRestServiceEditorComponent.attributes"), main);
		attributes.expand();
		return attributes;
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

		CollapsibleLayout components = new CollapsibleLayout(msg.getMessage("UpmanRestServiceEditorComponent.groups"), main);
		components.expand();		
		return components;
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
