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
import pl.edu.icm.unity.webui.VaadinEndpointProperties.ScaleMode;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
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
public class AuthNTile extends CustomComponent
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
	
	public AuthNTile(List<AuthenticationOption> authenticators,
			ScaleMode scaleMode, int perRow, SelectionChangedListener listener, String name)
	{
		this.authenticators = authenticators;
		this.scaleMode = scaleMode;
		this.perRow = perRow;
		this.listener = listener;
		this.name = name;
		initUI(null);
	}

	public void setFilter(String filter)
	{
		initUI(filter);
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
	
	private void reloadContents(String filter)
	{
		providersChoice.removeAllComponents();
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
				if (filter != null && !name.toLowerCase().contains(filter))
					continue;
				Resource logo = vaadinAuthenticationUI.getImage();
				String id = vaadinAuthenticationUI.getId();
				final String globalId = set.getId() + "_" + id;
				if (firstOptionId == null)
					firstOptionId = globalId;
				IdPComponent entry = new IdPComponent(globalId, logo, name, scaleMode);
				providersChoice.addComponent(entry);
				providersChoice.setComponentAlignment(entry, Alignment.MIDDLE_LEFT);
				authNOptionsById.put(globalId, set);
				authenticatorById.put(globalId, vaadinAuthenticationUI);

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
		
		setVisible(size() != 0);
	}
	
	public AuthenticationOption getAuthenticationOptionById(String id)
	{
		return authNOptionsById.get(id);
	}

	public VaadinAuthenticationUI getAuthenticatorById(String id)
	{
		return authenticatorById.get(id);
	}

	public int size()
	{
		return authenticatorById.size();
	}
	
	public Map<String, VaadinAuthenticationUI> getAuthenticators()
	{
		return authenticatorById;
	}

	public interface SelectionChangedListener
	{
		void selectionChanged(VaadinAuthenticationUI selectedAuthnUI, 
				AuthenticationOption selectedOption, String optionKey);
	}

	public String getFirstOptionId()
	{
		return firstOptionId;
	}

	public void filter(String filter)
	{
		if (filter != null && filter.trim().equals(""))
			filter = null;
		if (filter != null)
			filter = filter.toLowerCase();
		
		reloadContents(filter);
		
	}
}
