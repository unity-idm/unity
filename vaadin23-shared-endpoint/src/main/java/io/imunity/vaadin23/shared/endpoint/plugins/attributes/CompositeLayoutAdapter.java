/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.attributes;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class CompositeLayoutAdapter
{
	private final VerticalLayout layout;
	
	private List<Element> elements = new ArrayList<>();

	private int offset = 0;
	
	public CompositeLayoutAdapter(VerticalLayout layout, ComposableComponents... container)
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
		layout.add(element.getComponents());
	}

	private void removeFromLayout(Component component)
	{
		layout.remove(component);
	}

	private void addToLayout(Element element, Component component, Integer index)
	{
		int elmentStart = findElementStart(element);
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

	public static interface ComposableComponents
	{
		List<Component> getComponents();
		void setComponentInsertionListener(BiConsumer<Component, Integer> listener);
		void setComponentRemovalListener(Consumer<Component> listener);
	}

	private static interface Element
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
