/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.idpcommon;

import java.util.List;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.webui.common.Label100;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Either shows an identity which will be sent to the SP or allows to choose an identity out from 
 * several valid.
 *  
 * @author K. Benedyczak
 */
public class IdentitySelectorComponent extends CustomComponent
{
	private MessageSource msg;
	private IdentityTypeSupport idTypeSupport;
	private List<IdentityParam> validIdentities;
	
	protected IdentityParam selectedIdentity;
	protected ComboBox<IdentityParam> identitiesCB;
	
	public IdentitySelectorComponent(MessageSource msg, IdentityTypeSupport idTypeSupport,
			List<IdentityParam> validIdentities)
	{
		this.msg = msg;
		this.validIdentities = validIdentities;
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
			contents.addComponent(help);
		} else
		{
			Label identitiesL = new Label100(msg.getMessage("IdentitySelectorComponent.identities")); 
			identitiesCB = new ComboBox<>();
			identitiesCB.setItems(validIdentities);
			identitiesCB.setItemCaptionGenerator(ip -> ip.getValue());
			identitiesCB.setEmptySelectionAllowed(false);
			identitiesCB.setValue(selectedIdentity);
			identitiesCB.addSelectionListener(event -> selectedIdentity = event.getValue());
			
			contents.addComponents(identitiesL, identitiesCB);
		}

		setCompositionRoot(contents);
	}
	
	private Component getIdentityHelp(IdentityParam identity)
	{
		try
		{
			VerticalLayout ret = new VerticalLayout();
			ret.setMargin(false);
			Label identityL = new Label100(msg.getMessage("IdentitySelectorComponent.identity"));
			ret.addComponent(identityL);
			
			Label identityValue = new Label100(getIdentityVisualValue(selectedIdentity));
			identityValue.addStyleName(Styles.emphasized.toString());
			ret.addComponent(identityValue);

			IdentityTypeDefinition idTypeDef = idTypeSupport.getTypeDefinition(identity.getTypeId());
			String displayedValue = idTypeDef.toHumanFriendlyString(msg, identity);
			if (!displayedValue.equals(identity.getValue()))
			{
				ret.addComponent(new Label100(msg.getMessage(
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
