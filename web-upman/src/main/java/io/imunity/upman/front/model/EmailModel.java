/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.model;

import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.vaadin.elements.TooltipAttacher;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.verifiable.VerifiableElementBase;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

public class EmailModel
{
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	public final String value;
	public final VaadinIcon icon;
	public final Optional<Instant> confirmedAt;

	public EmailModel(String value, boolean confirmed, long confirmationDate)
	{
		this.value = value;
		if(confirmed)
		{
			icon = VaadinIcon.CHECK_CIRCLE_O;
			confirmedAt = Optional.of(Instant.ofEpochMilli(confirmationDate));
		}
		else
		{
			icon = VaadinIcon.EXCLAMATION_CIRCLE_O;
			confirmedAt = Optional.empty();
		}
	}

	public Div generateAsComponent(MessageSource msg, HtmlContainer container)
	{
		if(value == null)
			return new Div();
		Icon iconInstance = icon.create();
		confirmedAt.ifPresentOrElse(
				time -> TooltipAttacher.attachTooltip(msg.getMessage("SimpleConfirmationInfo.confirmed", formatter.format(time)), iconInstance, container),
				() -> TooltipAttacher.attachTooltip(msg.getMessage("SimpleConfirmationInfo.unconfirmed"), iconInstance, container)
		);
		return new Div(iconInstance, new Label(" " + value));
	}

	public static EmailModel of(VerifiableElementBase email)
	{
		if(email == null)
			return EmailModel.empty();
		else
			return new EmailModel(email.getValue(), email.isConfirmed(), email.getConfirmationInfo().getConfirmationDate());
	}
	public static EmailModel empty()
	{
		return new EmailModel(null, false, 0);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EmailModel emailModel = (EmailModel) o;
		return Objects.equals(value, emailModel.value) && icon == emailModel.icon && Objects.equals(confirmedAt, emailModel.confirmedAt);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(value, icon, confirmedAt);
	}

	@Override
	public String toString()
	{
		return "GridEmail{" +
				"text='" + value + '\'' +
				", icon=" + icon +
				", zonedDateTime=" + confirmedAt +
				'}';
	}
}
