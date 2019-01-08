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
 * Ordered group of components which should be placed together, but have no preset layout, i.e. can be composed 
 * as a part of chosen layout.
 *   
 * @author K. Benedyczak
 */
public class ComponentsGroup implements ComposableComponents
{
	private List<Component> components = new ArrayList<>();
	private BiConsumer<Component, Integer> componentInsertionListener;
	private Consumer<Component> componentRemovalListener;

	public ComponentsGroup(Component... initialComponents)
	{
		for (Component c: initialComponents)
			addComponent(c);
	}
	
	public void addComponent(Component component)
	{
		addComponent(component, components.size());
	}

	public void addComponent(Component component, int at)
	{
		components.add(at, component);
		if (componentInsertionListener != null)
			componentInsertionListener.accept(component, at);
	}
	
	public void removeComponent(Component component)
	{
		components.remove(component);
		if (componentRemovalListener != null)
			componentRemovalListener.accept(component);
	}
	
	@Override
	public List<Component> getComponents()
	{
		return new ArrayList<>(components);
	}
	
	public void removeAll()
	{
		for (int i=components.size()-1; i>=0; i--)
			removeComponent(components.get(i));
	}
	
	public int getComponentIndex(Component component)
	{
		return components.indexOf(component);
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
