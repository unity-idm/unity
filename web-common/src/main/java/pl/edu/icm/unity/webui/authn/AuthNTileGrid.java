/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.filter.SimpleStringFilter;
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
	
	public AuthNTileGrid(List<AuthenticationOption> authenticators,
			SelectionChangedListener listener, String name)
	{
		this.authenticators = authenticators;
		this.listener = listener;
		this.name = name;
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
		dataSource.addContainerProperty(COLUMN_NAME, String.class, "");
		
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

				Item item = dataSource.addItem(globalId);
				item.getItemProperty(COLUMN_NAME).setValue(name);
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
		dataSource.addContainerFilter(new SimpleStringFilter(COLUMN_NAME, filter, true, false));
	}

	@Override
	public Component getComponent()
	{
		return this;
	}
}
