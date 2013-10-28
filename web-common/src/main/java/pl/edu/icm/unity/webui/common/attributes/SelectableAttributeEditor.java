/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GroupComboBox;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElements;

/**
 * Attribute editor allowing to choose an attribute. It can use a fixed group for returned attribute or can 
 * allow to select it. It can allow to edit attribute visibility too.
 * 
 * @author K. Benedyczak
 */
public class SelectableAttributeEditor extends AbstractAttributeEditor
{
	private Collection<AttributeType> attributeTypes;
	private Collection<String> allowedGroups;
	private ListOfEmbeddedElements<?> valuesComponent;
	private AttributeSelectionComboBox attributeSel;
	private GroupComboBox groupSel;
	private EnumComboBox<AttributeVisibility> visibilitySel;
	private boolean showVisibilityWidget;
	private VerticalLayout main = new VerticalLayout();
	private Panel valuesPanel = new Panel();

	public SelectableAttributeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry, 
			Collection<AttributeType> attributeTypes, boolean showVisibilityWidget, 
			Collection<String> allowedGroups)
	{
		super(msg, registry);
		this.attributeTypes = attributeTypes;
		this.allowedGroups = allowedGroups;
		this.showVisibilityWidget = showVisibilityWidget;
		initUI();
	}
	
	public SelectableAttributeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry, 
			Collection<AttributeType> attributeTypes, boolean showVisibilityWidget, String fixedGroup)
	{
		this(msg, registry, attributeTypes, showVisibilityWidget, Collections.singleton(fixedGroup));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Attribute<?> getAttribute() throws FormValidationException
	{
		List<?> values = valuesComponent == null ? new ArrayList<>(0) : valuesComponent.getElements();
		AttributeType at = attributeSel.getSelectedValue();
		String group;
		if (attributeTypes.size() > 0)
			group = (String) groupSel.getValue();
		else
			group = allowedGroups.iterator().next();
		AttributeVisibility visibility = showVisibilityWidget ? visibilitySel.getSelectedValue() : 
			AttributeVisibility.full;
		return new Attribute(at.getName(), at.getValueType(), group, visibility, values);
	}
	
	private void initUI()
	{
		main.setSpacing(true);
		FormLayout top = new FormLayout();
		attributeSel = new AttributeSelectionComboBox(msg.getMessage("Attributes.attribute"), attributeTypes);
		attributeSel.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				AttributeType selected = attributeSel.getSelectedValue();
				valuesComponent = getValuesPart(selected);
				valuesPanel.setContent(valuesComponent);
			}
		});
		attributeSel.setImmediate(true);
		top.addComponent(attributeSel);
		if (allowedGroups.size() > 1)
		{
			groupSel = new GroupComboBox(msg.getMessage("Attributes.group"), allowedGroups);
			groupSel.setInput("/", true, true);
			top.addComponent(groupSel);
		}
		if (showVisibilityWidget)
		{
			visibilitySel = new EnumComboBox<AttributeVisibility>(msg.getMessage("Attributes.visibility"),
					msg, "AttributeVisibility.", 
					AttributeVisibility.class, AttributeVisibility.full);
			top.addComponent(visibilitySel);
		}
		valuesPanel.setCaption(msg.getMessage("Attributes.values"));
		main.addComponent(top);
		main.addComponent(valuesPanel);
		setCompositionRoot(main);
		if (attributeTypes.size() > 0)
		{
			valuesComponent = getValuesPart(attributeSel.getSelectedValue());
			valuesPanel.setContent(valuesComponent);
		}
	}

}
