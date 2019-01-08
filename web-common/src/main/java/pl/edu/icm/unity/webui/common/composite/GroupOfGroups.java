/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.composite;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.webui.common.composite.CompositeLayoutAdapter.ComposableComponents;

/**
 * Maintains list of {@link ComponentsGroup}s exposing them as a single, consolidated {@link ComposableComponents}.
 * 
 * @author K. Benedyczak
 */
public class GroupOfGroups implements ComposableComponents
{
	private List<ComposableComponents> elements = new ArrayList<>();
	private BiConsumer<Component, Integer> componentInsertionListener;
	private Consumer<Component> componentRemovalListener;
	
	public void addComposableComponents(ComposableComponents container)
	{
		elements.add(container);
		container.setComponentInsertionListener((comp, idx) -> {
			if (componentInsertionListener != null)
				componentInsertionListener.accept(comp, findGroupStart(container) + idx);
		});

		container.setComponentRemovalListener(c -> 
		{
			if (componentRemovalListener != null)
				componentRemovalListener.accept(c);
		});
		
		if (componentInsertionListener != null)
		{
			int start = findGroupStart(container);
			int i=0;
			for (Component c: container.getComponents())
				componentInsertionListener.accept(c, start + i++);
		}
	}
	
	private int findGroupStart(ComposableComponents element)
	{
		int pos = 0;
		for (ComposableComponents e: elements)
		{
			if (e == element)
				return pos;
			else
				pos += e.getComponents().size();
		}
		throw new IllegalStateException("Can't find group " + element);
	}

	@Override
	public List<Component> getComponents()
	{
		List<Component> ret = new ArrayList<>();
		for (ComposableComponents group: elements)
			ret.addAll(group.getComponents());
		return ret;
	}

	@Override
	public void setComponentInsertionListener(BiConsumer<Component, Integer> listener)
	{
		this.componentInsertionListener = listener;
	}

	@Override
	public void setComponentRemovalListener(Consumer<Component> listener)
	{
		this.componentRemovalListener = listener;
	}
}
