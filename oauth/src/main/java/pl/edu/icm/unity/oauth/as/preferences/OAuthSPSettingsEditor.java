/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.preferences;

import java.util.Arrays;
import java.util.Set;

import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;

import com.vaadin.data.util.IndexedContainer;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;

/**
 * Allows to edit settings for a single OAuth Client.
 * <p>
 * TODO  Implementation note: currently this code is very similar to the SAML SP Settings Editor. 
 * The code should be better reused in future.
 * 
 * @author K. Benedyczak
 */
public class OAuthSPSettingsEditor extends FormLayout
{
	protected UnityMessageSource msg;
	protected Identity[] identities;
	
	protected ComboBox client;
	protected Label clientLabel;
	protected OptionGroup decision;
	protected OptionGroup identity;
	
	public OAuthSPSettingsEditor(UnityMessageSource msg, Identity[] identities, 
			String client, OAuthClientSettings initial)
	{
		this.msg = msg;
		this.identities = Arrays.copyOf(identities, identities.length);
		initUI(initial, client, null);
	}

	public OAuthSPSettingsEditor(UnityMessageSource msg, Identity[] identities, Set<String> allClients)
	{
		this.msg = msg;
		this.identities = Arrays.copyOf(identities, identities.length);
		initUI(null, null, allClients);
	}
	
	public OAuthClientSettings getClientSettings()
	{
		OAuthClientSettings ret = new OAuthClientSettings();
		IndexedContainer decContainer = ((IndexedContainer)decision.getContainerDataSource());
		int idx = decContainer.indexOfId(decision.getValue());
		if (idx == 0)
		{
			ret.setDefaultAccept(true);
			ret.setDoNotAsk(true);
		} else if (idx == 1)
		{
			ret.setDefaultAccept(false);
			ret.setDoNotAsk(true);			
		} else
		{
			ret.setDefaultAccept(false);
			ret.setDoNotAsk(false);
		}
		
		String identityV = (String) identity.getValue();
		if (identityV != null)
		{
			IndexedContainer idContainer = ((IndexedContainer)identity.getContainerDataSource());
			Identity id = identities[idContainer.indexOfId(identityV)];
			IdentityTypeDefinition idType = id.getType().getIdentityTypeProvider();
			if (!idType.isDynamic() && !idType.isTargeted())
				ret.setSelectedIdentity(id.getComparableValue());
		}
		
		return ret;
	}
	
	public String getClient()
	{
		return client == null ? clientLabel.getValue() : (String) client.getValue();
	}
	
	private void initUI(OAuthClientSettings initial, String initialSp, Set<String> allSps)
	{
		if (initial == null)
		{
			client = new ComboBox(msg.getMessage("OAuthPreferences.client"));
			client.setInputPrompt(msg.getMessage("OAuthPreferences.clientPrompt"));
			client.setDescription(msg.getMessage("OAuthPreferences.clientDesc"));
			client.setWidth(100, Unit.PERCENTAGE);
			client.setTextInputAllowed(true);
			client.setFilteringMode(FilteringMode.OFF);
			client.setNewItemsAllowed(true);
			client.setNullSelectionAllowed(true);
			client.setImmediate(true);
			for (String spName: allSps)
				client.addItem(spName);
			addComponent(client);
		} else
		{
			clientLabel = new Label(initialSp);
			clientLabel.setCaption(msg.getMessage("OAuthPreferences.client"));
			addComponent(clientLabel);
		}
		
		decision = new OptionGroup(msg.getMessage("OAuthPreferences.decision"));
		decision.setNullSelectionAllowed(false);
		decision.addItem(msg.getMessage("OAuthPreferences.autoAccept"));
		decision.addItem(msg.getMessage("OAuthPreferences.autoDeny"));
		decision.addItem(msg.getMessage("OAuthPreferences.noAuto"));

		identity = new OptionGroup(msg.getMessage("OAuthPreferences.identity"));
		identity.setNullSelectionAllowed(true);
		for (Identity id: identities)
			identity.addItem(id.toPrettyString());
		
		addComponents(decision, identity);
		
		if (initial != null)
			setValues(initial);
		else
			setDefaults();
	}
	
	private void setDefaults()
	{
		IndexedContainer decContainer = ((IndexedContainer)decision.getContainerDataSource());
		decision.select(decContainer.getIdByIndex(2));
		IndexedContainer idContainer = ((IndexedContainer)identity.getContainerDataSource());
		identity.select(idContainer.getIdByIndex(0));
	}
	
	private void setValues(OAuthClientSettings initial)
	{
		IndexedContainer decContainer = ((IndexedContainer)decision.getContainerDataSource());
		if (!initial.isDoNotAsk())
			decision.select(decContainer.getIdByIndex(2));
		else if (initial.isDefaultAccept())
			decision.select(decContainer.getIdByIndex(0));
		else
			decision.select(decContainer.getIdByIndex(1));
		
		String selId = initial.getSelectedIdentity();
		if (selId != null)
		{
			for (Identity i: identities)
			{
				if (i.getComparableValue().equals(selId))
				{
					identity.select(i.toPrettyString());
					break;
				}
			}
		}
	}
}
