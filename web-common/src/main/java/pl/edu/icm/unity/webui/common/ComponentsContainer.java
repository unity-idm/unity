/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.FormLayout;

/**
 * Holds an ordered collection of {@link Component}s. It is used to return several tightly connected components,
 * which can be added to a wrapping layout one by one. This is useful (contrary to {@link CustomField}s) when 
 * the final UI should get the components with with several captions, properly aligned in the parent layout.
 * Especially in the {@link FormLayout} this is relevant.
 * <p>
 * It is assumed that the first component is the main one. It gets the label or description assigned 
 * with the helper methods, also it should be marked as required if needed.
 *  
 * @author K. Benedyczak
 */
public class ComponentsContainer
{
	private List<Component> components;
	
	public ComponentsContainer(Component... components)
	{
		this.components = new ArrayList<>(components.length);
		add(components);
	}
	
	public void add(Component... components)
	{
		for (Component c: components)
			this.components.add(c);
	}
	
	public void setDescription(String description)
	{
		if (components.get(0) instanceof AbstractComponent)
			((AbstractComponent)components.get(0)).setDescription(description);
	}
	
	public void setLabel(String label)
	{
		Component component = components.get(0);
		if (component instanceof ComponentWithLabel)
			((ComponentWithLabel) component).setLabel(label);
		else
			component.setCaption(label);
	}
	
	public Component[] getComponents()
	{
		return components.toArray(new Component[components.size()]);
	}
}
