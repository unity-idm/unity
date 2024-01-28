/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;

import io.imunity.vaadin.elements.CssClassNames;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Base for all service editor. Editor based on two tabs - general and
 * authentication.
 * 
 * @author P.Piernik
 *
 */
public abstract class ServiceEditorBase extends TabSheet implements ServiceEditorComponent
{
	protected MessageSource msg;
	private Map<String, Tab> defTabs;
	private Map<String, TabLayout> defTabContents;

	public ServiceEditorBase(MessageSource msg)
	{
		this.msg = msg;
		defTabs = new HashMap<>();
		defTabContents = new HashMap<>();
		addSelectedChangeListener(e -> setErrorInTabs());
	}

	protected void setErrorInTabs()
	{
		for (String tab : defTabs.keySet())
		{
			defTabContents.get(tab).error.setVisible(false);
			Tab tabContent = defTabs.get(tab);

			Set<Component> invalid = new HashSet<>();
			collectInvlidComponents(Stream.of(getComponent(tabContent)), invalid);
			defTabContents.get(tab).error.setVisible(invalid.size() > 0);
		}
	}

	private void collectInvlidComponents(Stream<Component> stream, Set<Component> invalid)
	{
		if (invalid.size() > 0)
			return;

		Set<Component> list = stream.collect(Collectors.toSet());
		for (Component component : list)
		{
			if (component instanceof HasValidation c)
			{
				if (c.isInvalid())
					invalid.add(component);
			}

			if (component.getChildren()
					.count() > 0)
			{
				collectInvlidComponents(component.getChildren(), invalid);
			}
		}
	}
	
	@Override
	public Component getComponent()
	{
		return this;
	}

	protected void registerTab(EditorTab editorTab)
	{
		TabLayout tabLayout = new TabLayout(msg, editorTab.getIcon(), editorTab.getCaption());
		Tab add = add(tabLayout, editorTab.getComponent());
		defTabs.put(editorTab.getType(), add);
		defTabContents.put(editorTab.getType(), tabLayout);
	}

	public void setActiveTab(String tab)
	{
		setSelectedTab(defTabs.get(tab));
	}

	public interface EditorTab
	{
		VaadinIcon getIcon();
		String getType();
		Component getComponent();
		String getCaption();
	}

	private static class TabLayout extends HorizontalLayout
	{
		public final Icon error = VaadinIcon.EXCLAMATION.create();

		public TabLayout(MessageSource msg, VaadinIcon icon, String caption)
		{
			add(new Icon(icon));
			error.addClassName(CssClassNames.SMALL_ICON.getName());
			error.addClassName(CssClassNames.ERROR.getName());
			error.setVisible(false);
			error.setTooltipText(msg.getMessage("error"));
			setSpacing(true);
			setPadding(false);
			HorizontalLayout wrapper = new HorizontalLayout(new NativeLabel(caption), error);
			add(wrapper);
			wrapper.setSpacing(false);
			wrapper.setMargin(false);
			wrapper.setPadding(false);
		}

	}

}
