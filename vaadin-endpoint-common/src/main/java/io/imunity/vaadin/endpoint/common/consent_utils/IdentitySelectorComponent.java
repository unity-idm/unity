/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.consent_utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

import java.util.List;

/**
 * Either shows an identity which will be sent to the SP or allows to choose an identity out from 
 * several valid.
 *  
 * @author K. Benedyczak
 */
public class IdentitySelectorComponent extends VerticalLayout
{
	private final MessageSource msg;
	private final IdentityTypeSupport idTypeSupport;
	private final List<IdentityParam> validIdentities;

	private IdentityParam selectedIdentity;
	private ComboBox<IdentityParam> identitiesCB;
	
	public IdentitySelectorComponent(MessageSource msg, IdentityTypeSupport idTypeSupport,
	                                 List<IdentityParam> validIdentities)
	{
		this.msg = msg;
		this.validIdentities = List.copyOf(validIdentities);
		this.idTypeSupport = idTypeSupport;
		initUI();
	}
	
	/**
	 * Tries to find an identity matching the one in argument. Nop when there is only one valid identity.
	 * Identities are compared with specialized comparison method (type dependent) if possible.  
	 * 
	 * @param selId identity to select
	 */
	public void setSelected(String selId)
	{
		if (validIdentities.size() > 0 && selId != null)
		{
			for (IdentityParam id: validIdentities)
			{
				if (id instanceof Identity)
					
				{
					if (((Identity)id).getComparableValue().equals(selId))
					{
						if (identitiesCB != null)
							identitiesCB.setValue(id);
						selectedIdentity = id;
						break;
					}
				} else if (id.getValue().equals(selId))
				{
					if (identitiesCB != null)
						identitiesCB.setValue(id);
					selectedIdentity = id;
					break;
				}
			}
		}
	}
	
	public IdentityParam getSelectedIdentity()
	{
		return selectedIdentity;
	}
	
	/**
	 * @return identity value which should be stored in preferences or null if the selected
	 * identity should not be stored in preferences (e.g. it is a dynamic identity).
	 */
	public String getSelectedIdentityForPreferences()
	{
		String identityValue = selectedIdentity.getValue();
		if (selectedIdentity instanceof Identity)
		{
			Identity casted = (Identity) selectedIdentity;
			identityValue = casted.getComparableValue();
			IdentityTypeDefinition idType = idTypeSupport.getTypeDefinition(casted.getTypeId());
			if (idType.isDynamic() || idType.isTargeted())
				return null;
		}
		return identityValue;
	}
	
	private void initUI()
	{
		selectedIdentity = validIdentities.get(0);
		VerticalLayout contents = new VerticalLayout();
		contents.setMargin(false);
		
		if (validIdentities.size() == 1)
		{
			Component help = getIdentityHelp(selectedIdentity);
			contents.add(help);
		} else
		{
			Label identitiesL = new Label(msg.getMessage("IdentitySelectorComponent.identities"));
			identitiesCB = new ComboBox<>();
			identitiesCB.setItems(validIdentities);
			identitiesCB.setItemLabelGenerator(IdentityTaV::getValue);
			identitiesCB.setRequired(false);
			identitiesCB.setValue(selectedIdentity);
			identitiesCB.addValueChangeListener(event -> selectedIdentity = event.getValue());
			
			contents.add(identitiesL, identitiesCB);
		}

		add(contents);
	}
	
	private Component getIdentityHelp(IdentityParam identity)
	{
		try
		{
			VerticalLayout ret = new VerticalLayout();
			ret.setMargin(false);
			Label identityL = new Label(msg.getMessage("IdentitySelectorComponent.identity"));
			ret.add(identityL);
			
			Label identityValue = new Label(getIdentityVisualValue(selectedIdentity));
			ret.add(identityValue);

			IdentityTypeDefinition idTypeDef = idTypeSupport.getTypeDefinition(identity.getTypeId());
			String displayedValue = idTypeDef.toHumanFriendlyString(msg, identity);
			if (!displayedValue.equals(identity.getValue()))
			{
				ret.add(new Label(msg.getMessage(
						"IdentitySelectorComponent.fullValue", identity.getValue())));
			}
			return ret;
		} catch (IllegalArgumentException e)
		{
			return new Label(msg.getMessage(
					"IdentitySelectorComponent.identityType", identity.getTypeId()));
		}
	}

	private String getIdentityVisualValue(IdentityParam identity)
	{
		try
		{
			IdentityTypeDefinition idTypeDef = idTypeSupport.getTypeDefinition(identity.getTypeId());
			return idTypeDef.toHumanFriendlyString(msg, identity);
		} catch (IllegalArgumentException e)
		{
			return identity.getValue();
		}
	}
}
