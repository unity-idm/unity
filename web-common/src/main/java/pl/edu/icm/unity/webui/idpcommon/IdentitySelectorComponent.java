/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.idpcommon;

import java.util.List;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.common.ExpandCollapseButton;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Either shows an identity which will be sent to the SP or allows to choose an identity out from 
 * several valid.
 *  
 * @author K. Benedyczak
 */
public class IdentitySelectorComponent extends CustomComponent
{
	private UnityMessageSource msg;
	private IdentityTypeSupport idTypeSupport;
	private List<IdentityParam> validIdentities;
	
	protected IdentityParam selectedIdentity;
	protected ComboBox<IdentityParam> identitiesCB;
	
	public IdentitySelectorComponent(UnityMessageSource msg, IdentityTypeSupport idTypeSupport,
			List<IdentityParam> validIdentities)
	{
		super();
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
			HorizontalLayout header = new HorizontalLayout();
			header.setMargin(false);
			
			Component help = getIdentityHelp(selectedIdentity);
			ExpandCollapseButton expander = new ExpandCollapseButton(true, help);
			
			Label identityL = new Label(msg.getMessage("IdentitySelectorComponent.identity"));
			identityL.setStyleName(Styles.bold.toString());
			
			header.addComponents(identityL);
			
			Label identityValue = new Label(getIdentityVisualValue(selectedIdentity));
			identityValue.addStyleName(Styles.emphasized.toString());
			
			contents.addComponents(header, identityValue, expander, help);
		} else
		{
			Label identitiesL = new Label(msg.getMessage("IdentitySelectorComponent.identities")); 
			identitiesL.setStyleName(Styles.bold.toString());
			Label infoManyIds = new Label(msg.getMessage("IdentitySelectorComponent.infoManyIds"));
			infoManyIds.setStyleName(Styles.vLabelSmall.toString());
			identitiesCB = new ComboBox<IdentityParam>();
			identitiesCB.setItems(validIdentities);
			identitiesCB.setEmptySelectionAllowed(false);
			identitiesCB.setValue(selectedIdentity);
			identitiesCB.addSelectionListener(event -> selectedIdentity = event.getValue());
			
			contents.addComponents(identitiesL, infoManyIds, identitiesCB);
		}

		setCompositionRoot(contents);
	}
	
	private Component getIdentityHelp(IdentityParam identity)
	{
		try
		{
			VerticalLayout ret = new VerticalLayout();
			ret.setMargin(false);
			IdentityTypeDefinition idTypeDef = idTypeSupport.getTypeDefinition(identity.getTypeId());
			String displayedValue = idTypeDef.toHumanFriendlyString(msg, identity);
			if (!displayedValue.equals(identity.getValue()))
			{
				ret.addComponent(new Label(msg.getMessage(
						"IdentitySelectorComponent.fullValue", identity.getValue())));
			}
			Label typeDesc = new Label(idTypeDef.getHumanFriendlyDescription(msg));
			typeDesc.addStyleName(Styles.vLabelSmall.toString());
			ret.addComponent(typeDesc);
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
