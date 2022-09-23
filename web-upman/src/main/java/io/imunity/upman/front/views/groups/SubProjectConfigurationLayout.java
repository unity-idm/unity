/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.groups;

import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.upman.front.model.Group;
import io.imunity.vaadin23.elements.FormLayoutLabel;
import io.imunity.vaadin23.elements.TooltipAttacher;
import pl.edu.icm.unity.MessageSource;

import static com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition.ASIDE;

class SubProjectConfigurationLayout extends FormLayout
{
	public final Checkbox enableDelegation;
	public final Checkbox enableSubprojects;
	public final TextField logoUrl;

	public SubProjectConfigurationLayout(MessageSource msg, HtmlContainer tooltipAttachment, Group group)
	{
		this(msg, tooltipAttachment);
		enableDelegation.setValue(group.delegationEnabled);
		logoUrl.setEnabled(group.delegationEnabled);
		enableSubprojects.setEnabled(group.delegationEnabled);

		enableSubprojects.setValue(group.subprojectsDelegationEnabled);
		if(group.logoUrl != null)
			logoUrl.setValue(group.logoUrl);
	}

	public SubProjectConfigurationLayout(MessageSource msg, HtmlContainer tooltipAttachment)
	{
		setResponsiveSteps(new FormLayout.ResponsiveStep("1em", 1, ASIDE));

		enableDelegation = new Checkbox(msg.getMessage("SubprojectDialog.enableDelegationCaption"));
		logoUrl = new TextField();
		enableSubprojects = new Checkbox(msg.getMessage("SubprojectDialog.enableSubprojects"));

		enableDelegation.addValueChangeListener(event ->
		{
			logoUrl.setEnabled(event.getValue());
			enableSubprojects.setEnabled(event.getValue());
		});
		enableDelegation.focus();

		TooltipAttacher.attachTooltip(msg.getMessage("SubprojectDialog.enableDelegationDescription"), enableDelegation, tooltipAttachment);

		logoUrl.setEnabled(false);
		enableSubprojects.setEnabled(false);

		addFormItem(enableDelegation, "");
		addFormItem(logoUrl, new FormLayoutLabel(msg.getMessage("SubprojectDialog.logoUrlCaption")));
		addFormItem(enableSubprojects, "");

		setWidth("30em");
	}
}
