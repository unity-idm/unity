/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.webui.VaadinEndpointProperties.ScaleMode;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Panel;

/**
 * Component showing a group of {@link VaadinAuthenticationUI}s. All of them are presented by means of small 
 * {@link IdPComponent} (logo/label).
 * 
 *   
 * @author K. Benedyczak
 */
public class AuthNTileSimple extends CustomComponent implements AuthNTile
{
	private List<AuthenticationOption> authenticators;
	private ScaleMode scaleMode;
	private int perRow;
	private SelectionChangedListener listener;
	private Map<String, AuthenticationOption> authNOptionsById;
	private Map<String, VaadinAuthenticationUI> authenticatorById;
	private GridLayout providersChoice;
	private String name;
	private Panel tilePanel;
	private String firstOptionId;
	
	public AuthNTileSimple(List<AuthenticationOption> authenticators,
			ScaleMode scaleMode, int perRow, SelectionChangedListener listener, String name)
	{
		this.authenticators = authenticators;
		this.scaleMode = scaleMode;
		this.perRow = perRow;
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
		
		providersChoice = new GridLayout(perRow, 1);
		providersChoice.setSpacing(true);
		
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
	private void reloadContents(String filter)
	{
		providersChoice.removeAllComponents();
		authNOptionsById = new HashMap<>();
		authenticatorById = new HashMap<>();
		firstOptionId = null;
		
		List<IdPComponent> filteredOut = new ArrayList<>();
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
				IdPComponent entry = new IdPComponent(globalId, logo, name, scaleMode);
				authNOptionsById.put(globalId, set);
				authenticatorById.put(globalId, vaadinAuthenticationUI);
				if (filter != null && !name.toLowerCase().contains(filter))
				{
					entry.addStyleName(Styles.hidden.toString());
					filteredOut.add(entry);
				} else
				{
					providersChoice.addComponent(entry);
					providersChoice.setComponentAlignment(entry, Alignment.MIDDLE_LEFT);
					entry.addClickListener(new ClickListener()
					{
						@Override
						public void buttonClick(ClickEvent event)
						{
							listener.selectionChanged(vaadinAuthenticationUI, set, globalId);
						}
					});
				}
			}
		}
		
		for (IdPComponent hidden: filteredOut)
		{
			providersChoice.addComponent(hidden);			
		}
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
		if (filter != null && filter.trim().equals(""))
			filter = null;
		if (filter != null)
			filter = filter.toLowerCase();
		
		reloadContents(filter);
	}
	
	@Override
	public Component getComponent()
	{
		return this;
	}
}
