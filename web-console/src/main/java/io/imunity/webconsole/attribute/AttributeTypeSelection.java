/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.attribute;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import com.vaadin.event.selection.SingleSelectionEvent;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeSelectionComboBox;
import pl.edu.icm.unity.webui.common.safehtml.HtmlSimplifiedLabelWithLinks;

class AttributeTypeSelection extends CustomComponent
{
	private MessageSource msg;
	private Label name;
	private Consumer<AttributeType> callback;
	private ComponentWithTooltip componentWithTooltip;
	private AttributeSelectionComboBox attributeTypesCombo;
	private FormLayoutWithFixedCaptionWidth main;

	AttributeTypeSelection(AttributeType attributeType, String groupPath, MessageSource msg)
	{
		this(Collections.singletonList(attributeType), groupPath, msg);
	}

	AttributeTypeSelection(Collection<AttributeType> attributeTypes, String groupPath, MessageSource msg)
	{
		this.msg = msg;
		this.main = FormLayoutWithFixedCaptionWidth.withVeryShortCaptions();
		setCompositionRoot(main);
		createAttributeSelectionWidget(attributeTypes);
	}

	private void createAttributeWidget(AttributeType type)
	{
		name = new Label(type.getName());
		componentWithTooltip = new ComponentWithTooltip(name, msg.getMessage("AttributeType.name"),
				type.getDescription().getValue(msg), false);
		main.addComponent(componentWithTooltip);
	}

	private void createAttributeSelectionWidget(Collection<AttributeType> attributeTypes)
	{
		attributeTypesCombo = new AttributeSelectionComboBox(null, attributeTypes);
		attributeTypesCombo.setWidth(FieldSizeConstans.SHORT_FIELD_WIDTH, FieldSizeConstans.SHORT_FIELD_WIDTH_UNIT);
		if (attributeTypes.size() == 1)
		{
			createAttributeWidget(attributeTypes.iterator().next());
		} else
		{
			componentWithTooltip = new ComponentWithTooltip(attributeTypesCombo, msg.getMessage("AttributeType.name"), "", true);
			main.addComponent(componentWithTooltip);
			attributeTypesCombo.addSelectionListener(event -> changeAttributeType(event));
		}
	}

	public void setAttributeType(String name2)
	{
		attributeTypesCombo.setSelectedItemByName(name2);
	}

	public AttributeType getAttributeType()
	{
		return attributeTypesCombo.getValue();
	}

	void setCallback(Consumer<AttributeType> callback)
	{
		this.callback = callback;
	}

	private void changeAttributeType(SingleSelectionEvent<AttributeType> event)
	{
		AttributeType type = event.getValue();
		componentWithTooltip.refreshTooltip(type.getDescription().getValue(msg));

		if (callback != null)
			callback.accept(type);
	}

	private static class ComponentWithTooltip extends CustomComponent
	{
		private HtmlSimplifiedLabelWithLinks icon;
		
		public ComponentWithTooltip(AbstractComponent component, String caption, String description, boolean centerIcon)
		{
			setCaption(caption);
			HorizontalLayout main = new HorizontalLayout();
			main.setMargin(false);
			icon = new HtmlSimplifiedLabelWithLinks();
			icon.addStyleName(Styles.iconOnlyLabel.toString());
			refreshTooltip(description);
			HorizontalLayout iconWrapper = new HorizontalLayout();
			iconWrapper.setWidth(1, Unit.EM);
			iconWrapper.addComponent(icon);
			if (centerIcon)
				iconWrapper.setComponentAlignment(icon, Alignment.MIDDLE_LEFT);
			iconWrapper.setMargin(false);
		
			main.setSpacing(true);
			main.addComponent(component);
			main.addComponent(iconWrapper);
			main.setExpandRatio(component, 2);
			main.setExpandRatio(iconWrapper, 0);
			setCompositionRoot(main);
		}

		void refreshTooltip(String description)
		{
			if (description != null && !description.isEmpty())
			{
				icon.setDescription(description);
				icon.setIcon(Images.question.getResource());
				icon.setVisible(true);
			} else
			{
				icon.setVisible(false);
			}
		}
	}
}
