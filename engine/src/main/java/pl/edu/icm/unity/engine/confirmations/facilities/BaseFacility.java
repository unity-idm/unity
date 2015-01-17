/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.VerifiableElement;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.ConfirmationInfo;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Contains methods used in all facilities
 * 
 * @author P. Piernik
 * 
 */
public abstract class BaseFacility
{
	protected void updateConfirmationInfo(VerifiableElement el, String value)
	{
		if (el.getValue().equals(value))
		{
			if (el.getConfirmationInfo() != null)
			{
				int amount = el.getConfirmationInfo().getSentRequestAmount();
				el.getConfirmationInfo().setSentRequestAmount(amount + 1);
				el.getConfirmationInfo().setConfirmed(false);
				el.getConfirmationInfo().setConfirmationDate(0);
			} else
			{
				el.setConfirmationInfo(new ConfirmationInfo(1));
			}
		}
	}

	private boolean confirmSingleElement(VerifiableElement verifiable, String value)
	{
		if (verifiable.getValue().equals(value))
		{
			ConfirmationInfo confirmationData = verifiable.getConfirmationInfo();
			confirmationData.setConfirmed(true);
			Date today = new Date();
			confirmationData.setConfirmationDate(today.getTime());
			confirmationData.setSentRequestAmount(0);
			return true;
		}
		return false;
	}

	protected Collection<Attribute<?>> confirmAttributes(Collection<Attribute<?>> attrs,
			String attrName, String group, String value) throws EngineException
	{
		List<Attribute<?>> confirmed = new ArrayList<Attribute<?>>();
		for (Attribute<?> attr : attrs)
		{
			if (attr.getName().equals(attrName) && attr.getGroupPath().equals(group)
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

	protected Collection<IdentityParam> confirmIdentity(Collection<IdentityParam> identities,
			String type, String value) throws EngineException
	{
		ArrayList<IdentityParam> confirmed = new ArrayList<IdentityParam>();
		for (IdentityParam id : identities)
		{
			if (id.getTypeId().equals(type))
			{
				if (confirmSingleElement(id, value))
					confirmed.add(id);
			}
		}
		return confirmed;
	}

}
