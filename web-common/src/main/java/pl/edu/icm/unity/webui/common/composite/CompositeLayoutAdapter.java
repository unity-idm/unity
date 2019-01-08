/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.composite;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Component;

/**
 * Maintains list of {@link ComponentsGroup}s and puts them into a given layout. Whenever contents of a group
 * is changed, the changes are propagated to the layout, however maintaining the order of the groups.
 * 
 * @author K. Benedyczak
 */
public class CompositeLayoutAdapter
{
	private final AbstractOrderedLayout layout;
	
	private List<Element> elements = new ArrayList<>();

	private int offset = 0;
	
	public CompositeLayoutAdapter(AbstractOrderedLayout layout, ComposableComponents... container)
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
		layout.addComponents(element.getComponents());
	}

	private void removeFromLayout(Component component)
	{
		layout.removeComponent(component);
	}

	private void addToLayout(Element element, Component component, Integer index)
	{
		int elmentStart = findElementStart(element);
		layout.addComponent(component, index + elmentStart);
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
