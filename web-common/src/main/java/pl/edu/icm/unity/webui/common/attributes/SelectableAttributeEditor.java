/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GroupComboBox;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.VerticalLayout;

/**
 * Attribute editor allowing to choose an attribute. It can use a fixed group for returned attribute or can 
 * allow to select it. It can allow to edit attribute visibility too.
 * <p>
 * This class is not a component on its own - it returns one.
 * 
 * @author K. Benedyczak
 */
public class SelectableAttributeEditor extends AbstractAttributeEditor
{
	private Collection<AttributeType> attributeTypes;
	private Collection<String> allowedGroups;
	private ListOfEmbeddedElementsStub<LabelledValue> valuesComponent;
	private AttributeSelectionComboBox attributeSel;
	private GroupComboBox groupSel;
	private EnumComboBox<AttributeVisibility> visibilitySel;
	private boolean showVisibilityWidget;
	private VerticalLayout main = new VerticalLayout();
	private SafePanel valuesPanel = new SafePanel();

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
	
	public void setInitialAttribute(Attribute<?> initial)
	{
		attributeSel.setValue(initial.getName());
		if (groupSel != null)
			groupSel.setValue(initial.getGroupPath());
		if (visibilitySel != null)
			visibilitySel.setEnumValue(initial.getVisibility());
		setNewValuesUI();
		List<LabelledValue> labelledValues = new ArrayList<>(initial.getValues().size());
		
		String baseLabel = initial.getName();
		for (int i=0; i<initial.getValues().size(); i++)
		{
			String label = baseLabel + ((initial.getValues().size() > 1) ? " (" + (i+1) + "):" : ""); 
			labelledValues.add(new LabelledValue(initial.getValues().get(i), label));
		}
		valuesComponent.setEntries(labelledValues);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Attribute<?> getAttribute() throws FormValidationException
	{
		List<?> labelledValues = valuesComponent == null ? new ArrayList<>(0) : valuesComponent.getElements();
		List<Object> values = new ArrayList<>(labelledValues.size());
		for (Object lv: labelledValues)
			values.add(((LabelledValue)lv).getValue());
		AttributeType at = attributeSel.getSelectedValue();
		String group;
		if (attributeTypes.size() > 0)
			group = (String) groupSel.getValue();
		else
			group = allowedGroups.iterator().next();
		AttributeVisibility visibility = showVisibilityWidget ? visibilitySel.getSelectedValue() : 
			at.getVisibility();
		return new Attribute(at.getName(), at.getValueType(), group, visibility, values);
	}
	
	private void initUI()
	{
		main.setSpacing(true);
		FormLayout top = new CompactFormLayout();
		attributeSel = new AttributeSelectionComboBox(msg.getMessage("Attributes.attribute"), attributeTypes);
		attributeSel.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				setNewValuesUI();
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
		if (attributeTypes.size() > 0)
			setNewValuesUI();
	}

	private void setNewValuesUI()
	{
		AttributeType selected = attributeSel.getSelectedValue();
		FormLayout ct = new CompactFormLayout();
		ct.setMargin(true);
		valuesComponent = getValuesPart(selected, selected.getName(), true, true, ct);
		valuesPanel.setContent(ct);
	}
	
	public Component getComponent()
	{
		return main;
	}

}
