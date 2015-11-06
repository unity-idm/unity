/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import pl.edu.icm.unity.confirmations.states.BaseConfirmationState;
import pl.edu.icm.unity.engine.confirmations.ConfirmationFacility;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;

/**
 * Base for all facilities
 * 
 * @author P. Piernik
 * 
 */
public abstract class BaseFacility<T extends BaseConfirmationState> implements ConfirmationFacility<T>
{
	/**
	 * Check if verifiable element has given value, if yes set element 
	 * as unconfirmed and increases amount of sent request
	 * @param verifiableElement
	 * @param value
	 */
	protected void updateConfirmationInfo(VerifiableElement verifiableElement, String value)
	{
		if (verifiableElement.getValue().equals(value))
		{
			if (verifiableElement.getConfirmationInfo() != null)
			{
				int amount = verifiableElement.getConfirmationInfo().getSentRequestAmount();
				verifiableElement.getConfirmationInfo().setSentRequestAmount(amount + 1);
				verifiableElement.getConfirmationInfo().setConfirmed(false);
				verifiableElement.getConfirmationInfo().setConfirmationDate(0);
			} else
			{
				verifiableElement.setConfirmationInfo(new ConfirmationInfo(1));
			}
		}
	}

	/**
	 * Check if verifiable element has given value, if yes set element as confirmed
	 * @param verifiableElement
	 * @param value
	 * @return
	 */
	private boolean confirmSingleElement(VerifiableElement verifiableElement, String value)
	{
		if (verifiableElement.getValue().equals(value))
		{
			ConfirmationInfo confirmationData = verifiableElement.getConfirmationInfo();
			confirmationData.setConfirmed(true);
			Date today = new Date();
			confirmationData.setConfirmationDate(today.getTime());
			confirmationData.setSentRequestAmount(0);
			return true;
		}
		return false;
	}

	/**
	 * Check which attributes in collection have given group, name, value and is verifiable, if yes set attribute 
	 * as confirmed
	 * @param attrs
	 * @param attrName
	 * @param group
	 * @param value
	 * @return
	 * @throws EngineException
	 */
	protected Collection<Attribute<?>> confirmAttributes(Collection<Attribute<?>> attrs,
			String attrName, String group, String value) throws EngineException
	{
		List<Attribute<?>> confirmed = new ArrayList<Attribute<?>>();
		for (Attribute<?> attr : attrs)
		{
			if (attr != null && attr.getName().equals(attrName) && attr.getGroupPath().equals(group)
					&& attr.getValues() != null
					&& attr.getAttributeSyntax().isVerifiable())
			{
				for (Object el : attr.getValues())
				{
					VerifiableElement verifiable = (VerifiableElement) el;
					if (confirmSingleElement(verifiable, value))
						confirmed.add(attr);
				}

			}
		}
		return confirmed;
	}

	/**
	 * Check which identities in collection have given type and value and, if yes set identity as confirmed
	 * @param identities
	 * @param type
	 * @param value
	 * @return
	 * @throws EngineException
	 */
	protected Collection<IdentityParam> confirmIdentity(Collection<IdentityParam> identities,
			String type, String value) throws EngineException
	{
		ArrayList<IdentityParam> confirmed = new ArrayList<IdentityParam>();
		for (IdentityParam id : identities)
		{
			if (id != null && id.getTypeId().equals(type))
			{
				if (confirmSingleElement(id, value))
					confirmed.add(id);
			}
		}
		return confirmed;
	}

}
