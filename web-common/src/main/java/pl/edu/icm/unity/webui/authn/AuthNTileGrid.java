/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
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
import com.vaadin.ui.Panel;
import com.vaadin.ui.renderers.ImageRenderer;

import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Component showing a group of {@link VaadinAuthenticationUI}s. All of them are presented in Vaadin {@link Grid}.
 *   
 * @author K. Benedyczak
 */
public class AuthNTileGrid extends CustomComponent implements AuthNTile
{
	private List<AuthenticationOption> authenticators;
	private SelectionChangedListener listener;
	private Map<String, AuthenticationOption> authNOptionsById;
	private Map<String, VaadinAuthenticationUI> authenticatorById;
	private Grid<AuthNTileProvider> providersChoice;
	private String name;
	private Panel tilePanel;
	private String firstOptionId;
	private Collator collator;
	private List<AuthNTileProvider> providers;
	private ListDataProvider<AuthNTileProvider> dataProvider;
	
	public AuthNTileGrid(List<AuthenticationOption> authenticators, UnityMessageSource msg, 
			SelectionChangedListener listener, String name)
	{
		this.authenticators = authenticators;
		this.listener = listener;
		this.name = name;
		collator = Collator.getInstance(msg.getLocale());
		initUI(null);
	}

	private void initUI(String filter)
	{
		if (filter != null && filter.trim().equals(""))
			filter = null;
		if (filter != null)
			filter = filter.toLowerCase();
		tilePanel = new SafePanel();
		tilePanel.setSizeUndefined();
		if (name != null)
			tilePanel.setCaption(name);

		providers = new ArrayList<>();
		dataProvider = DataProvider.ofCollection(providers);
		providersChoice = new Grid<AuthNTileGrid.AuthNTileProvider>(dataProvider);
		providersChoice.setSelectionMode(SelectionMode.NONE);

		providersChoice.addColumn(AuthNTileProvider::getImage, new ImageRenderer<>());
		Column<AuthNTileProvider, NameWithTags> namColumn = providersChoice
				.addColumn(AuthNTileProvider::getNameWithTags);
		providersChoice.sort(namColumn);

		providersChoice.addStyleName(Styles.idpTile.toString());
		providersChoice.setHeaderVisible(false);
		providersChoice.setSizeFull();
		providersChoice.setStyleGenerator(item -> "idpentry_" + item.getId());

		providersChoice.addItemClickListener(event -> {

			String globalId = event.getItem().getId();
			listener.selectionChanged(authenticatorById.get(globalId),
					authNOptionsById.get(globalId), globalId);

		});

		providersChoice.setWidth(600, Unit.PIXELS);
		
		reloadContents(filter);
		tilePanel.setContent(providersChoice);
		setCompositionRoot(tilePanel);
		setSizeUndefined();
	}

	@Override
	public void setCaption(String caption)
	{
		tilePanel.setCaption(caption);
	}
	
	/**
	 * Shows all authN UIs of all enabled authN options. The options not matching the given filter are 
	 * added too, but at the end and are hidden. This trick guarantees that the containing box 
	 * stays with a fixed size, while the user only sees the matching options at the top.
	 * @param filter
	 */
	@SuppressWarnings("unchecked")
	private void reloadContents(String filter)
	{
		providers.clear();
		authNOptionsById = new HashMap<>();
		authenticatorById = new HashMap<>();
		firstOptionId = null;

		for (final AuthenticationOption set : authenticators)
		{
			VaadinAuthentication firstAuthenticator = (VaadinAuthentication) set
					.getPrimaryAuthenticator();

			Collection<VaadinAuthenticationUI> uiInstances = firstAuthenticator
					.createUIInstance();
			for (final VaadinAuthenticationUI vaadinAuthenticationUI : uiInstances)
			{
				String name = vaadinAuthenticationUI.getLabel();
				Resource logo = vaadinAuthenticationUI.getImage();
				String id = vaadinAuthenticationUI.getId();
				final String globalId = AuthenticationOptionKeyUtils
						.encode(set.getId(), id);
				if (firstOptionId == null)
					firstOptionId = globalId;
				authNOptionsById.put(globalId, set);
				authenticatorById.put(globalId, vaadinAuthenticationUI);

				NameWithTags nameWithTags = new NameWithTags(name,
						vaadinAuthenticationUI.getTags(), collator);
				AuthNTileProvider providerEntry = new AuthNTileProvider(globalId, nameWithTags,
						logo == null ? Images.empty.getResource() : logo);
				providers.add(providerEntry);

			}
		}

		dataProvider.refreshAll();
		setVisible(size() != 0);
	}
	
	@Override
	public AuthenticationOption getAuthenticationOptionById(String id)
	{
		return authNOptionsById.get(id);
	}

	@Override
	public VaadinAuthenticationUI getAuthenticatorById(String id)
	{
		return authenticatorById.get(id);
	}

	@Override
	public int size()
	{
		return authenticatorById.size();
	}
	
	@Override
	public Map<String, VaadinAuthenticationUI> getAuthenticators()
	{
		return authenticatorById;
	}

	@Override
	public String getFirstOptionId()
	{
		return firstOptionId;
	}

	@Override
	public void filter(String filter)
	{
		dataProvider.clearFilters();
		dataProvider.addFilter(v -> v.getNameWithTags().contains(filter));
	
	}

	@Override
	public Component getComponent()
	{
		return this;
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
			if (name.toLowerCase().contains(what))
				return true;
			for (String tag : tags)
				if (tag.toLowerCase().contains(what))
					return true;
			return false;
		}

		@Override
		public int compareTo(Object o)
		{
			return collator.compare(toString(), o.toString());
		}
	}

	private class AuthNTileProvider
	{
		private String id;
		private NameWithTags nameWithTags;
		private Resource image;

		public AuthNTileProvider(String id, NameWithTags nameWithTags, Resource image)
		{
			this.id = id;
			this.nameWithTags = nameWithTags;
			this.image = image;
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
	}
}
