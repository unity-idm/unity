/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.text.Collator;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.Resource;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.CellReference;
import com.vaadin.ui.Grid.CellStyleGenerator;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Panel;
import com.vaadin.ui.renderers.ImageRenderer;

import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
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
	private static final String COLUMN_NAME = "name";
	private static final String COLUMN_IMG = "image";
	private List<AuthenticationOption> authenticators;
	private SelectionChangedListener listener;
	private Map<String, AuthenticationOption> authNOptionsById;
	private Map<String, VaadinAuthenticationUI> authenticatorById;
	private Grid providersChoice;
	private IndexedContainer dataSource;
	private String name;
	private Panel tilePanel;
	private String firstOptionId;
	private Collator collator;
	
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
		
		dataSource = new IndexedContainer();
		dataSource.addContainerProperty(COLUMN_IMG, Resource.class, Images.empty.getResource());
		dataSource.addContainerProperty(COLUMN_NAME, NameWithTags.class, new NameWithTags("", 
				Collections.emptySet(), collator));
		
		providersChoice = new Grid(dataSource);
		providersChoice.setSelectionMode(SelectionMode.NONE);
		providersChoice.addStyleName(Styles.idpTile.toString());
		providersChoice.removeHeaderRow(0);
		providersChoice.getColumn(COLUMN_IMG).setRenderer(new ImageRenderer());
		providersChoice.setCellStyleGenerator(new CellStyleGenerator()
		{
			@Override
			public String getStyle(CellReference cellReference)
			{
				return "idpentry_" + cellReference.getItemId();
			}
		});
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
		dataSource.removeAllItems();
		authNOptionsById = new HashMap<>();
		authenticatorById = new HashMap<>();
		firstOptionId = null;

		for (final AuthenticationOption set: authenticators)
		{
			VaadinAuthentication firstAuthenticator = (VaadinAuthentication) set.getPrimaryAuthenticator();
			
			Collection<VaadinAuthenticationUI> uiInstances = 
					firstAuthenticator.createUIInstance();
			for (final VaadinAuthenticationUI vaadinAuthenticationUI : uiInstances)
			{
				String name = vaadinAuthenticationUI.getLabel();
				Resource logo = vaadinAuthenticationUI.getImage();
				String id = vaadinAuthenticationUI.getId();
				final String globalId = set.getId() + "_" + id;
				if (firstOptionId == null)
					firstOptionId = globalId;
				authNOptionsById.put(globalId, set);
				authenticatorById.put(globalId, vaadinAuthenticationUI);

				NameWithTags nameWithTags = new NameWithTags(name, vaadinAuthenticationUI.getTags(),
						collator);
				Item item = dataSource.addItem(globalId);
				item.getItemProperty(COLUMN_NAME).setValue(nameWithTags);
				item.getItemProperty(COLUMN_IMG).setValue(logo == null ? 
						Images.empty.getResource() : logo);
			}
		}
		providersChoice.addItemClickListener(new ItemClickListener()
		{
			@Override
			public void itemClick(ItemClickEvent event)
			{
				String globalId = (String) event.getItemId();
				
				listener.selectionChanged(authenticatorById.get(globalId), 
						authNOptionsById.get(globalId), globalId);
			}
		});
		providersChoice.sort(COLUMN_NAME);
		providersChoice.setWidth(600, Unit.PIXELS);
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
		dataSource.removeAllContainerFilters();
		dataSource.addContainerFilter(new TagAwareStringFilter(filter));
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
			for (String tag: tags)
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
	
	public static class TagAwareStringFilter implements Filter
	{
		private String filter;
		
		public TagAwareStringFilter(String filter)
		{
			this.filter = filter.toLowerCase();
		}

		@Override
		public boolean passesFilter(Object itemId, Item item)
				throws UnsupportedOperationException
		{
			final Property<?> p = item.getItemProperty(COLUMN_NAME);
			if (p == null)
				return false;
			NameWithTags propertyValue = (NameWithTags) p.getValue();
			if (propertyValue == null)
				return false;
			return propertyValue.contains(filter);
		}

		@Override
		public boolean appliesToProperty(Object propertyId)
		{
		        return COLUMN_NAME.equals(propertyId);
		}
	}
}
