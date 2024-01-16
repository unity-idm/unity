/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.SlotUtils;
import com.vaadin.flow.dom.ElementConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class CompositeLayoutAdapter
{
	private final HasComponents layout;
	
	private List<Element> elements = new ArrayList<>();

	private int offset = 0;
	
	public CompositeLayoutAdapter(HasComponents layout, ComposableComponents... container)
	{
		this.layout = layout;
		for (ComposableComponents c: container)
			addContainer(c);
	}

	public void setOffset(int offset)
	{
		this.offset = offset;
		
	}
	
	public void addContainer(ComposableComponents container)
	{
		GroupElement element = new GroupElement(container);
		elements.add(element);
		addToLayout(element);
		container.setComponentInsertionListener((comp, idx) -> addToLayout(element, comp, idx));
		container.setComponentRemovalListener(this::removeFromLayout);
	}
	
	private void addToLayout(Element element)
	{
		if(layout instanceof FormLayout formLayout)
		{
			for (Component component: element.getComponents())
			{
				if(component instanceof Checkbox checkbox)
				{
					formLayout.addFormItem(checkbox, "")
							.setVisible(component.isVisible());
					continue;
				}
				if(component instanceof HasLabel hasLabel)
				{
					formLayout.addFormItem(component, hasLabel.getLabel())
							.setVisible(component.isVisible());
					hasLabel.setLabel("");
				}
				else
					formLayout.add(component);
			}
			return;
		}
		layout.add(element.getComponents());
	}

	private void removeFromLayout(Component component)
	{
		if(layout instanceof FormLayout formLayout)
		{
			formLayout.getChildren()
					.filter(formItem -> formItem.getChildren().anyMatch(child -> child.equals(component)))
					.forEach(layout::remove);
		}
		if(layout.getElement().getChildren().anyMatch(child -> child.equals(component.getElement())))
			layout.remove(component);
	}

	private void addToLayout(Element element, Component component, Integer index)
	{
		int elmentStart = findElementStart(element);
		if(layout instanceof FormLayout)
		{
			if(component instanceof Checkbox checkbox)
			{
				FormLayout.FormItem formItem = new FormLayout.FormItem(checkbox);
				formItem.setVisible(component.isVisible());
				layout.addComponentAtIndex(index + elmentStart, formItem);
				return;
			}
			if(component instanceof HasLabel hasLabel)
			{
				String label = hasLabel.getLabel();
				FormLayout.FormItem formItem = new FormLayout.FormItem(component);
				SlotUtils.addToSlot(formItem, ElementConstants.LABEL_PROPERTY_NAME, new Span(label));
				formItem.setVisible(component.isVisible());
				layout.addComponentAtIndex(index + elmentStart, formItem);
				hasLabel.setLabel("");
				return;
			}
		}
		layout.addComponentAtIndex(index + elmentStart, component);
	}

	private int findElementStart(Element element)
	{
		int pos = offset;
		for (Element e: elements)
		{
			if (e == element)
				return pos;
			else
				pos += e.getComponents().length;
		}
		throw new IllegalStateException("Can't find element " + element);
	}

	public interface ComposableComponents
	{
		List<Component> getComponents();
		void setComponentInsertionListener(BiConsumer<Component, Integer> listener);
		void setComponentRemovalListener(Consumer<Component> listener);
	}

	private interface Element
	{
		Component[] getComponents();
	}
	
	private static class GroupElement implements Element
	{
		private final ComposableComponents container;

		GroupElement(ComposableComponents container)
		{
			this.container = container;
		}

		@Override
		public Component[] getComponents()
		{
			List<Component> list = container.getComponents();
			return list.toArray(new Component[list.size()]);
		}
	}
}
