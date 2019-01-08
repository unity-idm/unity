/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmation.facilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.confirmation.states.BaseEmailConfirmationState;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.confirmation.EmailConfirmationFacility;
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
public abstract class BaseEmailFacility<T extends BaseEmailConfirmationState> implements EmailConfirmationFacility<T>
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
	 */
	protected <K extends Attribute> Collection<K> confirmAttributes(Collection<K> attrs,
			String attrName, String group, String value, AttributeTypeHelper atHelper) 
	{
		List<K> confirmed = new ArrayList<>();
		for (K attr : attrs)
		{
			if (attr == null)
				 continue;
			AttributeValueSyntax<?> syntax = atHelper.getUnconfiguredSyntax(attr.getValueSyntax());
			if (attr.getName().equals(attrName) && attr.getGroupPath().equals(group)
					&& attr.getValues() != null && syntax.isEmailVerifiable())
			{
				List<String> updatedValues = new ArrayList<>();
				boolean attrConfirmed = confirmAttributeValues(attr, syntax, updatedValues, value);
				if (attrConfirmed)
				{
					attr.setValues(updatedValues);
					confirmed.add(attr);
				}
			}
		}
		return confirmed;
	}

	private <K> boolean confirmAttributeValues(Attribute attr, AttributeValueSyntax<K> syntax, 
			List<String> updatedValues, String value)
	{
		boolean attrConfirmed = false;
		for (String el : attr.getValues())
		{
			K verifiable = syntax.convertFromString(el);
			if (confirmSingleElement((VerifiableElement) verifiable, value))
			{
				attrConfirmed = true;
				updatedValues.add(syntax.convertToString(verifiable));
			} else
			{
				updatedValues.add(el);
			}
		}
		return attrConfirmed;
	}
	
	protected <K> void updateConfirmationForAttributeValues(List<String> values, AttributeValueSyntax<K> syntax,
			String confirmedValue)
	{
		for (int i=0; i<values.size(); i++)
		{
			K domainVal = syntax.convertFromString(values.get(i));
			updateConfirmationInfo((VerifiableElement) domainVal, confirmedValue);
			values.set(i, syntax.convertToString(domainVal));
		}
	}
	

	
	/**
	 * Check which identities in collection have given type and value and, if yes set identity as confirmed
	 * @param identities
	 * @param type
	 * @param value
	 * @return
	 */
	protected <K extends IdentityParam> Collection<K> confirmIdentity(Collection<K> identities,
			String type, String value)
	{
		ArrayList<K> confirmed = new ArrayList<>();
		for (K id : identities)
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
