/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Lists;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.RadioButtonGroup;

import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Allows to edit settings for a single OAuth Client.
 * <p>
 * TODO  Implementation note: currently this code is very similar to the OAuth SP Settings Editor. 
 * The code should be better reused in future.
 * 
 * @author K. Benedyczak
 */
public class OAuthSPSettingsEditor extends FormLayout
{
	protected UnityMessageSource msg;
	private IdentityTypeSupport idTypeSupport;
	protected List<Identity> identities;
	
	protected ComboBox<String> client;
	protected Label clientLabel;
	protected RadioButtonGroup<Decision> decision;
	protected RadioButtonGroup<Identity> identity;
	
	public OAuthSPSettingsEditor(UnityMessageSource msg, IdentityTypeSupport idTypeSupport,
			List<Identity> identities, 
			String client, OAuthClientSettings initial)
	{
		this.msg = msg;
		this.idTypeSupport = idTypeSupport;
		this.identities = new ArrayList<>(identities);
		initUI(initial, client, null);
	}

	public OAuthSPSettingsEditor(UnityMessageSource msg, IdentityTypeSupport idTypeSupport, 
			List<Identity> identities, Set<String> allClients)
	{
		this.msg = msg;
		this.idTypeSupport = idTypeSupport;
		this.identities = new ArrayList<>(identities);
		initUI(null, null, allClients);
	}
	
	public OAuthClientSettings getClientSettings()
	{
		OAuthClientSettings ret = new OAuthClientSettings();
		Decision selDecision = decision.getSelectedItem().get();
		if (selDecision == Decision.AUTO_ACCEPT)
		{
			ret.setDefaultAccept(true);
			ret.setDoNotAsk(true);
		} else if (selDecision == Decision.AUTO_DENY)
		{
			ret.setDefaultAccept(false);
			ret.setDoNotAsk(true);			
		} else
		{
			ret.setDefaultAccept(false);
			ret.setDoNotAsk(false);
		}
		
		Identity id = identity.getValue();
		if (id != null)
		{
			IdentityTypeDefinition idType = idTypeSupport.getTypeDefinition(id.getTypeId());
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
			client = new ComboBox<>(msg.getMessage("OAuthPreferences.client"));
			client.setTextInputAllowed(true);
			client.setDescription(msg.getMessage("OAuthPreferences.clientDesc"));
			client.setWidth(100, Unit.PERCENTAGE);
			client.setTextInputAllowed(true);
			client.setNewItemProvider(s ->
			{
				List<String> items = Lists.newArrayList(s);
				items.addAll(allSps);
				client.setItems(items);
				return Optional.of(s);
			});
			client.setEmptySelectionAllowed(true);
			client.setItems(allSps);
			addComponent(client);
		} else
		{
			clientLabel = new Label(initialSp);
			clientLabel.setCaption(msg.getMessage("OAuthPreferences.client"));
			addComponent(clientLabel);
		}
		
		decision = new RadioButtonGroup<>(msg.getMessage("OAuthPreferences.decision"));
		decision.setItemCaptionGenerator(this::getDecisionCaption);
		decision.setItems(Decision.AUTO_ACCEPT, Decision.AUTO_DENY, Decision.NO_AUTO);

		identity = new RadioButtonGroup<>(msg.getMessage("OAuthPreferences.identity"));
		identity.setItems(identities);
		identity.setItemCaptionGenerator(id -> 
			idTypeSupport.getTypeDefinition(id.getTypeId()).toPrettyString(id));
		
		addComponents(decision, identity);
		
		if (initial != null)
			setValues(initial);
		else
			setDefaults();
	}
	
	private void setDefaults()
	{
		decision.setSelectedItem(Decision.NO_AUTO);
		identity.setSelectedItem(identities.get(0));
	}
	
	private void setValues(OAuthClientSettings initial)
	{
		if (!initial.isDoNotAsk())
			decision.setSelectedItem(Decision.NO_AUTO);
		else if (initial.isDefaultAccept())
			decision.setSelectedItem(Decision.AUTO_ACCEPT);
		else
			decision.setSelectedItem(Decision.AUTO_DENY);
		
		String selId = initial.getSelectedIdentity();
		if (selId != null)
		{
			for (Identity i: identities)
			{
				if (i.getComparableValue().equals(selId))
				{
					identity.setSelectedItem(i);
					break;
				}
			}
		}
	}
	
	
	private String getDecisionCaption(Decision dec)
	{
		switch (dec)
		{
		case AUTO_ACCEPT:
			return msg.getMessage("OAuthPreferences.autoAccept");
		case AUTO_DENY:
			return 	msg.getMessage("OAuthPreferences.autoDeny");
		case NO_AUTO:
			return msg.getMessage("OAuthPreferences.noAuto");
		default:
			throw new IllegalArgumentException("Unknown decision");
		}
	}
	
	private enum Decision
	{
		AUTO_ACCEPT,
		AUTO_DENY,
		NO_AUTO
	}
}
