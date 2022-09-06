/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.model;

import com.vaadin.flow.component.icon.VaadinIcon;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class EmailModel
{
	public final String value;
	public final VaadinIcon icon;
	public final Optional<Instant> zonedDateTime;

	public EmailModel(String value, boolean confirmed, long confirmationDate)
	{
		this.value = value;
		if(confirmed)
		{
			icon = VaadinIcon.CHECK_CIRCLE_O;
			zonedDateTime = Optional.of(Instant.ofEpochMilli(confirmationDate));
		}
		else
		{
			icon = VaadinIcon.EXCLAMATION_CIRCLE_O;
			zonedDateTime = Optional.empty();
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EmailModel emailModel = (EmailModel) o;
		return Objects.equals(value, emailModel.value) && icon == emailModel.icon && Objects.equals(zonedDateTime, emailModel.zonedDateTime);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(value, icon, zonedDateTime);
	}

	@Override
	public String toString()
	{
		return "GridEmail{" +
				"text='" + value + '\'' +
				", icon=" + icon +
				", zonedDateTime=" + zonedDateTime +
				'}';
	}
}
