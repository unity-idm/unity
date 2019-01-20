/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.server.Resource;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.renderers.ImageRenderer;

import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKeyUtils;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Component showing a group of {@link VaadinAuthenticationUI}s. All of them are presented in Vaadin {@link Grid}.
 */
public class AuthnsGridWidget extends CustomComponent
{
	private final AuthNPanelFactory authNPanelFactory;
	private final List<AuthNOption> options;

	private Map<String, AuthenticationFlow> authNOptionsById;
	private Map<String, VaadinAuthenticationUI> authenticatorById;
	private Grid<AuthenticationOptionGridEntry> providersChoice;
	private String firstOptionId;
	private Collator collator;
	private List<AuthenticationOptionGridEntry> providers;
	private ListDataProvider<AuthenticationOptionGridEntry> dataProvider;
	private final int height;
	
	public AuthnsGridWidget(List<AuthNOption> options, UnityMessageSource msg,
			AuthNPanelFactory authNPanelFactory, int height)
	{
		this.options = options;
		this.authNPanelFactory = authNPanelFactory;
		this.height = height;
		collator = Collator.getInstance(msg.getLocale());
		initUI(null);
	}

	private void initUI(String filter)
	{
		if (filter != null && filter.trim().equals(""))
			filter = null;
		if (filter != null)
			filter = filter.toLowerCase();

		providers = new ArrayList<>();
		dataProvider = DataProvider.ofCollection(providers);
		providersChoice = new Grid<>(dataProvider);
		providersChoice.setSelectionMode(SelectionMode.NONE);
		Column<AuthenticationOptionGridEntry, Resource> imageColumn = providersChoice.addColumn(
				AuthenticationOptionGridEntry::getImage, new ImageRenderer<>());
		Column<AuthenticationOptionGridEntry, Component> buttonColumn = providersChoice
				.addComponentColumn(AuthenticationOptionGridEntry::getComponent);

		providersChoice.addStyleName("u-authnGrid");
		providersChoice.setHeaderVisible(false);
		providersChoice.setSizeFull();
		providersChoice.setStyleGenerator(item -> "idpentry_" + 
					AuthenticationOptionKeyUtils.encodeToCSS(item.getId()));
		providersChoice.setHeightByRows(height);
		imageColumn.setExpandRatio(1);
		buttonColumn.setExpandRatio(6);
		
		providersChoice.setWidth(100, Unit.PERCENTAGE);
		imageColumn.setWidth(74);
		
		reloadContents();
		setCompositionRoot(providersChoice);
		setWidth(100, Unit.PERCENTAGE);
	}

	/**
	 * Shows all authN UIs of all enabled authN options. The options not matching the given filter are 
	 * added too, but at the end and are hidden. This trick guarantees that the containing box 
	 * stays with a fixed size, while the user only sees the matching options at the top.
	 * @param filter
	 */
	private void reloadContents()
	{
		providers.clear();
		authNOptionsById = new HashMap<>();
		authenticatorById = new HashMap<>();
		firstOptionId = null;

		for (AuthNOption entry: options)
		{
			String name = entry.authenticatorUI.getLabel();
			Resource logo = entry.authenticatorUI.getImage();
			String id = entry.authenticatorUI.getId();
			final String globalId = AuthenticationOptionKeyUtils.encode(
					entry.authenticator.getAuthenticatorId(), id);
			if (firstOptionId == null)
				firstOptionId = globalId;
			authNOptionsById.put(globalId, entry.flow);
			authenticatorById.put(globalId, entry.authenticatorUI);

			NameWithTags nameWithTags = new NameWithTags(name,
					entry.authenticatorUI.getTags(), collator);
			Resource logoImage = logo == null ? Images.empty.getResource() : logo;
			FirstFactorAuthNPanel authnPanel = authNPanelFactory.createGridCompatibleAuthnPanel(entry);
			AuthenticationOptionGridEntry providerEntry = new AuthenticationOptionGridEntry(globalId, nameWithTags,
					logoImage, authnPanel);
			providers.add(providerEntry);
		}
		
		providers.sort(null);
		
		dataProvider.refreshAll();
		setVisible(size() != 0);
	}
	
	private int size()
	{
		return authenticatorById.size();
	}
	
	public void filter(String filter)
	{
		dataProvider.clearFilters();
		dataProvider.addFilter(v -> v.getNameWithTags().contains(filter));
	
	}

	public static class NameWithTags implements Comparable<Object>
	{
		private String name;
		private Set<String> tags;
		private Collator collator;

		public NameWithTags(String name, Set<String> tags, Collator collator)
		{
			this.name = name;
			this.tags = tags;
			this.collator = collator;
		}

		@Override
		public String toString()
		{
			return name;
		}

		public Set<String> getTags()
		{
			return tags;
		}

		public boolean contains(String what)
		{
			String whatLowercase = what.toLowerCase();
			if (name.toLowerCase().contains(whatLowercase))
				return true;
			for (String tag : tags)
				if (tag.toLowerCase().contains(whatLowercase))
					return true;
			return false;
		}

		@Override
		public int compareTo(Object o)
		{
			return collator.compare(toString(), o.toString());
		}
	}

	private class AuthenticationOptionGridEntry implements Comparable<AuthenticationOptionGridEntry>
	{
		private String id;
		private NameWithTags nameWithTags;
		private Resource image;
		private Component component;

		public AuthenticationOptionGridEntry(String id, NameWithTags nameWithTags, Resource image, Component component)
		{
			this.id = id;
			this.nameWithTags = nameWithTags;
			this.image = image;
			this.component = component;
		}

		public String getId()
		{
			return id;
		}

		public NameWithTags getNameWithTags()
		{
			return nameWithTags;
		}

		public Resource getImage()
		{
			return image;
		}

		public Component getComponent()
		{
			return component;
		}

		@Override
		public int compareTo(AuthenticationOptionGridEntry o)
		{
			String otherName = o.getNameWithTags().name;
			String thisName = getNameWithTags().name;
			return thisName.compareToIgnoreCase(otherName);
		}
	}
}
