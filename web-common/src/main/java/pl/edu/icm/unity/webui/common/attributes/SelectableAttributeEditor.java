/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GroupComboBox2;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Attribute editor allowing to choose an attribute. It can use a fixed group for returned attribute or can 
 * allow to select it. 
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
	private AttributeSelectionComboBox2 attributeSel;
	private GroupComboBox2 groupSel;
	private VerticalLayout main = new VerticalLayout();
	private SafePanel valuesPanel = new SafePanel();

	public SelectableAttributeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry, 
			Collection<AttributeType> attributeTypes, 
			Collection<String> allowedGroups)
	{
		super(msg, registry);
		this.attributeTypes = attributeTypes;
		this.allowedGroups = allowedGroups;
		initUI();
	}
	
	public SelectableAttributeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry, 
			Collection<AttributeType> attributeTypes, String fixedGroup)
	{
		this(msg, registry, attributeTypes, Collections.singleton(fixedGroup));
	}
	
	public void setInitialAttribute(Attribute initial)
	{
		attributeSel.setSelectedItemByName(initial.getName());
		if (groupSel != null)
			groupSel.setValue(initial.getGroupPath());
		setNewValuesUI();
		List<LabelledValue> labelledValues = new ArrayList<>(initial.getValues().size());
		
		String baseLabel = initial.getName();
		for (int i=0; i<initial.getValues().size(); i++)
		{
			String label = baseLabel + ((initial.getValues().size() > 1) ? " (" + (i+1) + "):" : "");
			String rawValue = initial.getValues().get(i);
			labelledValues.add(new LabelledValue(rawValue, label));
		}
		valuesComponent.setEntries(labelledValues);
	}
	
	public Attribute getAttribute() throws FormValidationException
	{
		List<?> labelledValues = valuesComponent == null ? new ArrayList<>(0) : valuesComponent.getElements();
		List<String> values = new ArrayList<>(labelledValues.size());
		AttributeType at = attributeSel.getValue();
		for (Object lv: labelledValues)
		{
			String value = ((LabelledValue)lv).getValue();
			values.add(value);
		}
		String group;
		if (attributeTypes.size() > 0)
			group = groupSel.getValue();
		else
			group = allowedGroups.iterator().next();
		return new Attribute(at.getName(), at.getValueSyntax(), group, values);
	}
	
	private void initUI()
	{
		main.setSpacing(true);
		main.setMargin(false);
		FormLayout top = new CompactFormLayout();
		attributeSel = new AttributeSelectionComboBox2(msg.getMessage("Attributes.attribute"), attributeTypes);
		attributeSel.addSelectionListener(event -> setNewValuesUI());
	
		top.addComponent(attributeSel);
		if (allowedGroups.size() > 1)
		{
			groupSel = new GroupComboBox2(msg.getMessage("Attributes.group"), allowedGroups);
			groupSel.setInput("/", true);
			top.addComponent(groupSel);
		}
		valuesPanel.setCaption(msg.getMessage("Attributes.values"));
		main.addComponent(top);
		main.addComponent(valuesPanel);
		if (attributeTypes.size() > 0)
			setNewValuesUI();
	}

	private void setNewValuesUI()
	{
		AttributeType selected = attributeSel.getValue();
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
