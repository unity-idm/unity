/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.List;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

/**
 * Renders attribute values panel.
 * When the attribute has only a single value then a simple panel is displayed with the only value.
 * When the attribute has more then one value, then a table with values is displayed in the top part,
 * and the actually selected value in the bottom part.
 * @author K. Benedyczak
 */
public class ValuesRendererPanel extends VerticalLayout
{
	private UnityMessageSource msg;
	
	public ValuesRendererPanel(UnityMessageSource msg)
	{
		this.msg = msg;
		setMargin(new MarginInfo(false, false, false, true));
		setSizeFull();
	}
	
	public void removeValues()
	{
		removeAllComponents();
	}
	
	public void setValues(WebAttributeHandler<?> handler, AttributeValueSyntax<?> syntax, 
			List<?> values)
	{
		removeValues();
		if (values.size() > 1)
			buildMultiValueView(handler, syntax, values);
		else if (values.size() == 1)
			buildSingleValueView(handler, syntax, values.get(0));
		else
			buildNoValueView();
	}
	
	@SuppressWarnings("rawtypes")
	private void buildMultiValueView(final WebAttributeHandler handler, final AttributeValueSyntax<?> syntax, 
			List<?> values)
	{
		final VerticalSplitPanel main = new VerticalSplitPanel();
		main.setSplitPosition(33, Unit.PERCENTAGE);
		
		final ValuesTable valuesTable = new ValuesTable(msg);
		valuesTable.setValues(values, syntax, handler);
		main.setFirstComponent(valuesTable);
		
		valuesTable.setImmediate(true);
		valuesTable.addValueChangeListener(new ValueChangeListener()
		{
			@SuppressWarnings("unchecked")
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Object selectedId = valuesTable.getValue();
				Object value = valuesTable.getItemById(selectedId);
				Component c = handler.getRepresentation(value, syntax);
				c.setSizeUndefined();
				
				Panel valuePanel = new Panel(msg.getMessage("Attribute.selectedValue"));
				valuePanel.setContent(c);
				valuePanel.setSizeFull();
				main.setSecondComponent(valuePanel);
			}
		});
		Object firstId = valuesTable.getItemIds().iterator().next();
		valuesTable.select(firstId);
		addComponent(main);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void buildSingleValueView(WebAttributeHandler handler, AttributeValueSyntax<?> syntax, 
			Object value)
	{
		Component c = handler.getRepresentation(value, syntax);
		c.setSizeUndefined();
		Panel valuePanel = new Panel(msg.getMessage("Attribute.value"));
		valuePanel.setSizeFull();
		valuePanel.setContent(c);
		addComponent(valuePanel);
	}
	
	private void buildNoValueView()
	{
		Label noValue = new Label(msg.getMessage("Attribute.noValue"));
		addComponent(noValue);
	}

}
