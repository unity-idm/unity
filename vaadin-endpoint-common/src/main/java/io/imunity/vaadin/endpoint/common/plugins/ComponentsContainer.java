/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.Component;

import io.imunity.vaadin.endpoint.common.HtmlTooltipAttacher;

public class ComponentsContainer
{
	private final List<Component> components;
	
	public ComponentsContainer(Component... components)
	{
		this.components = new ArrayList<>(components.length);
		add(components);
	}
	
	public void add(Component... components)
	{
		this.components.addAll(Arrays.asList(components));
	}
	
	public void setDescription(String description)
	{
		HtmlTooltipAttacher.to(components.get(0), description);
	}
	
	public void setLabel(String label)
	{
		components.get(0).getElement().setProperty("label", label == null ? "" : label);
	}
	
	public Component[] getComponents()
	{
		return components.toArray(new Component[0]);
	}
}
