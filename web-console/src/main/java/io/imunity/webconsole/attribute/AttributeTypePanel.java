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

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeSelectionComboBox;
import pl.edu.icm.unity.webui.common.safehtml.HtmlSimplifiedLabelWithLinks;


class AttributeTypePanel extends FormLayoutWithFixedCaptionWidth
{
	private MessageSource msg;
	private Label name;
	private Consumer<AttributeType> callback;
	private ComponentWithTooltip componentWithTooltip;
	private AttributeSelectionComboBox attributeTypesC;
	
	AttributeTypePanel(AttributeType attributeType, String groupPath, MessageSource msg)
	{
		this(Collections.singletonList(attributeType), groupPath, msg);
	}
	
	AttributeTypePanel(Collection<AttributeType> attributeTypes, String groupPath, 
			MessageSource msg)
	{
		super();
		this.msg = msg;
		createAttributeSelectionWidget(attributeTypes);
		
	}
	
	private void createAttributeWidget(AttributeType type)
	{
		name = new Label(type.getName());
		name.setDescription(type.getDescription().getValue(msg));
		componentWithTooltip = new ComponentWithTooltip(name, msg.getMessage("AttributeType.name"));
		addComponent(componentWithTooltip);
	}
	
	private void createAttributeSelectionWidget(Collection<AttributeType> attributeTypes)
	{
		attributeTypesC = new AttributeSelectionComboBox(null, 
				attributeTypes); 
	
		
		if (attributeTypes.size() == 1)
		{
			createAttributeWidget(attributeTypes.iterator().next());
		} else
		{
			componentWithTooltip = new ComponentWithTooltip(attributeTypesC, msg.getMessage("AttributeType.name"));
			addComponent(componentWithTooltip);
			attributeTypesC.setWidth(100, Unit.PERCENTAGE);
			attributeTypesC.addSelectionListener(event -> changeAttribute(event));
		}
	}
	
	public void setAttributeType(String name2)
	{
		 attributeTypesC.setSelectedItemByName(name2);
		
	}

	public AttributeType getAttributeType()
	{
		return attributeTypesC.getValue();
	}
	

	void setCallback(Consumer<AttributeType> callback)
	{
		this.callback = callback;
	}

	private void changeAttribute(SingleSelectionEvent<AttributeType> event)
	{
		AttributeType type = event.getValue();
		attributeTypesC.setDescription(type.getDescription().getValue(msg));
		componentWithTooltip.refreshTooltip();
		
		if (callback != null)
			callback.accept(type);
	}

	private static class ComponentWithTooltip extends CustomComponent
	{
		private AbstractComponent component;
		private HtmlSimplifiedLabelWithLinks icon;
		public ComponentWithTooltip(AbstractComponent component, String caption)
		{
			this.component = component;
			setCaption(caption);
			HorizontalLayout main = new HorizontalLayout();
			main.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
			main.setMargin(false);
			icon = new HtmlSimplifiedLabelWithLinks();
			icon.addStyleName(Styles.iconOnlyLabel.toString());
			refreshTooltip();
			HorizontalLayout iconWrapper = new HorizontalLayout();
			iconWrapper.setWidth(1, Unit.EM);
			iconWrapper.addComponent(icon);
			iconWrapper.setComponentAlignment(icon, Alignment.MIDDLE_LEFT);
			iconWrapper.setMargin(false);
			
			main.setSpacing(true);
			main.addComponent(component);
			main.addComponent(iconWrapper);	
			main.setExpandRatio(component, 2);
			main.setExpandRatio(iconWrapper, 0);
			setCompositionRoot(main);		
		}
		
		void refreshTooltip()
		{
			String description = component.getDescription();
			if (description != null && !description.isEmpty())
			{	
				icon.setDescription(description);
				icon.setIcon(Images.question.getResource());
				icon.setVisible(true);
			}
				else
			{
				icon.setVisible(false);
			}		
		}
	}	
}
