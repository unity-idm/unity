/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.idpselector.IdPComponent;
import pl.edu.icm.unity.webui.common.idpselector.IdpSelectorComponent.ScaleMode;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.GridLayout;

/**
 * Component showing a group of {@link VaadinAuthenticationUI}s. All of them are presented by means of small 
 * {@link IdPComponent} (logo/label).
 * 
 *   
 * @author K. Benedyczak
 */
public class AuthNTile extends CustomComponent
{
	private List<Map<String, VaadinAuthentication>> authenticators;
	private ScaleMode scaleMode;
	private int perRow;
	private SelectionChangedListener listener;
	private Map<String, VaadinAuthenticationUI> authNOptionsById = new HashMap<>();
	
	public AuthNTile(List<Map<String, VaadinAuthentication>> authenticators,
			ScaleMode scaleMode, int perRow, SelectionChangedListener listener)
	{
		this.authenticators = authenticators;
		this.scaleMode = scaleMode;
		this.perRow = perRow;
		this.listener = listener;
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
		
		GridLayout providersChoice = new GridLayout(perRow, 1);
		providersChoice.setSpacing(true);
		providersChoice.addStyleName(Styles.maxHeight300.toString());
		
		for (Map<String, VaadinAuthentication> set: authenticators)
		{
			Map.Entry<String, VaadinAuthentication> firstAuthenticator = 
					set.entrySet().iterator().next();
			
			Collection<VaadinAuthenticationUI> uiInstances = 
					firstAuthenticator.getValue().createUIInstance();
			for (final VaadinAuthenticationUI vaadinAuthenticationUI : uiInstances)
			{
				String name = vaadinAuthenticationUI.getLabel();
				if (filter != null && !name.toLowerCase().contains(filter))
					continue;
				String logoUrl = vaadinAuthenticationUI.getImageURL();
				String id = vaadinAuthenticationUI.getId();
				final String globalId = firstAuthenticator.getKey() + "_" + id;
				IdPComponent entry = new IdPComponent(globalId, logoUrl, name, scaleMode);
				providersChoice.addComponent(entry);
				providersChoice.setComponentAlignment(entry, Alignment.MIDDLE_LEFT);
				authNOptionsById.put(globalId, vaadinAuthenticationUI);

				entry.addClickListener(new ClickListener()
				{
					@Override
					public void buttonClick(ClickEvent event)
					{
						listener.selectionChanged(vaadinAuthenticationUI, globalId);
					}
				});
			}
		}
		
		setCompositionRoot(providersChoice);
	}

	public VaadinAuthenticationUI getById(String id)
	{
		return authNOptionsById.get(id);
	}

	public interface SelectionChangedListener
	{
		void selectionChanged(VaadinAuthenticationUI selectedOption, String optionKey);
	}
}
