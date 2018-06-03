/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Panel;

import pl.edu.icm.unity.webui.VaadinEndpointProperties.ScaleMode;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Component showing a group of {@link VaadinAuthenticationUI}s. All of them are presented by means of small 
 * {@link IdPComponent} (logo/label).
 * 
 *   
 * @author K. Benedyczak
 */
public class AuthNTileSimple extends CustomComponent implements AuthNTile
{
	private ScaleMode scaleMode;
	private int perRow;
	private SelectionChangedListener listener;
	private Map<String, VaadinAuthenticationUI> authenticatorById;
	private GridLayout providersChoice;
	private String name;
	private Panel tilePanel;
	private String firstOptionId;
	
	public AuthNTileSimple(Map<String, VaadinAuthenticationUI> authenticatorById,
			ScaleMode scaleMode, int perRow, SelectionChangedListener listener, String name)
	{
		this.scaleMode = scaleMode;
		this.perRow = perRow;
		this.listener = listener;
		this.name = name;
		this.authenticatorById = authenticatorById;
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
	private void reloadContents(String filter)
	{
		providersChoice.removeAllComponents();
		firstOptionId = null;
		
		List<IdPComponent> filteredOut = new ArrayList<>();

		
		
		for (Map.Entry<String, VaadinAuthenticationUI> entry : authenticatorById.entrySet())
		{

			VaadinAuthenticationUI vaadinAuthenticationUI = entry.getValue();
			String globalId = entry.getKey();

			String name = vaadinAuthenticationUI.getLabel();
			Resource logo = vaadinAuthenticationUI.getImage();

			if (firstOptionId == null)
				firstOptionId = globalId;
			IdPComponent idpEntry = new IdPComponent(globalId, logo, name, scaleMode);

			if (filter != null && !name.toLowerCase().contains(filter))
			{
				idpEntry.addStyleName(Styles.hidden.toString());
				filteredOut.add(idpEntry);
			} else
			{
				providersChoice.addComponent(idpEntry);
				providersChoice.setComponentAlignment(idpEntry,
						Alignment.MIDDLE_LEFT);
				idpEntry.addClickListener(new ClickListener()
				{
					@Override
					public void buttonClick(ClickEvent event)
					{
						listener.selectionChanged(globalId);
					}
				});
			}
		}

		for (IdPComponent hidden : filteredOut)
		{
			providersChoice.addComponent(hidden);
		}
		setVisible(size() != 0);
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
