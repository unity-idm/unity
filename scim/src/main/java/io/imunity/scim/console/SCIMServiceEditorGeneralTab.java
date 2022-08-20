/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.List;
import java.util.Set;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.Component;

import io.imunity.tooltip.TooltipExtension;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.common.groups.MandatoryGroupSelection;
import pl.edu.icm.unity.webui.common.groups.OptionalGroupWithWildcardSelection;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.tabs.GeneralTab;

public class SCIMServiceEditorGeneralTab extends GeneralTab
{
	private Binder<SCIMServiceConfigurationBean> restBinder;
	private final List<Group> allGroups;

	public SCIMServiceEditorGeneralTab(MessageSource msg, EndpointTypeDescription type, List<String> usedEndpointsPaths,
			Set<String> serverContextPaths, List<Group> allGroups)
	{
		super(msg, type, usedEndpointsPaths, serverContextPaths);
		this.allGroups = List.copyOf(allGroups);
	}

	public void initUI(Binder<DefaultServiceDefinition> serviceBinder, Binder<SCIMServiceConfigurationBean> restBinder,
			boolean editMode)
	{
		super.initUI(serviceBinder, editMode);
		this.restBinder = restBinder;
		mainLayout.addComponent(buildScimSection());
		mainLayout.addComponent(buildCorsSection());
	}

	private Component buildScimSection()
	{

		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);

		MandatoryGroupSelection rootGroup = new MandatoryGroupSelection(msg);
		rootGroup.setCaption(msg.getMessage("SCIMServiceEditorGeneralTab.rootGroup"));
		rootGroup.setWidth(FieldSizeConstans.SHORT_FIELD_WIDTH, FieldSizeConstans.SHORT_FIELD_WIDTH_UNIT);
		rootGroup.setItems(allGroups);
		main.addComponent(rootGroup);
		restBinder.forField(rootGroup).asRequired().bind("rootGroup");
		
		OptionalGroupWithWildcardSelection memeberShipGroups = new OptionalGroupWithWildcardSelection(msg);
		memeberShipGroups.setSkipRemoveInvalidSelections(true);
		memeberShipGroups.setCaption(msg.getMessage("SCIMServiceEditorGeneralTab.memebershipGroups"));
		memeberShipGroups.setDescription(msg.getMessage("SCIMServiceEditorGeneralTab.memebershipGroupsDesc"));
		memeberShipGroups.setWidth(FieldSizeConstans.SHORT_FIELD_WIDTH, FieldSizeConstans.SHORT_FIELD_WIDTH_UNIT);
		//simplification
		memeberShipGroups.setItems(allGroups);
		main.addComponent(memeberShipGroups);
		restBinder.forField(memeberShipGroups).asRequired().withValidator((value, context) ->
		{
			if (value == null || value.isEmpty())
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			return ValidationResult.ok();
		}).bind("membershipGroups");
		TooltipExtension.tooltip(memeberShipGroups);

		OptionalGroupWithWildcardSelection excludedMembershipGroups = new OptionalGroupWithWildcardSelection(msg);
		excludedMembershipGroups.setSkipRemoveInvalidSelections(true);
		excludedMembershipGroups.setCaption(msg.getMessage("SCIMServiceEditorGeneralTab.excludedMembershipGroups"));
		excludedMembershipGroups.setDescription(msg.getMessage("SCIMServiceEditorGeneralTab.excludedMembershipGroupsDesc"));
		excludedMembershipGroups.setWidth(FieldSizeConstans.SHORT_FIELD_WIDTH, FieldSizeConstans.SHORT_FIELD_WIDTH_UNIT);
		
		//simplification
		excludedMembershipGroups.setItems(allGroups);
		main.addComponent(excludedMembershipGroups);
		restBinder.forField(excludedMembershipGroups).bind("excludedMembershipGroups");
		TooltipExtension.tooltip(excludedMembershipGroups);
			
		CollapsibleLayout corsSection = new CollapsibleLayout(msg.getMessage("SCIMServiceEditorGeneralTab.scimGroups"),
				main);
		corsSection.expand();
		return corsSection;
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

		CollapsibleLayout corsSection = new CollapsibleLayout(msg.getMessage("RestAdminServiceEditorComponent.cors"),
				main);
		return corsSection;
	}
}
