/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.enquiry;

import pl.edu.icm.unity.types.registration.BaseForm;

import java.util.Objects;

class RewriteComboToEnquiryRequest
{
	public final String invitationCode;
	public final Long entity;
	public final BaseForm form;
	
	RewriteComboToEnquiryRequest(String invitationCode, Long entity, BaseForm form)
	{
		this.invitationCode = invitationCode;
		this.entity = entity;
		this.form = form;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(entity, form, invitationCode);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RewriteComboToEnquiryRequest other = (RewriteComboToEnquiryRequest) obj;
		return Objects.equals(entity, other.entity) && Objects.equals(form, other.form)
				&& Objects.equals(invitationCode, other.invitationCode);
	}
	
	
	
}
