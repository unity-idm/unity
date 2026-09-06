/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.invitations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import pl.edu.icm.unity.base.message.MessageSource;

@ExtendWith(MockitoExtension.class)
public class InvitationGridTest
{
	private static final String WARNING_COLOR = "var(--unity-warning-badge-font-color)";

	@Mock
	private MessageSource msg;

	@BeforeEach
	public void setUp()
	{
		when(msg.getMessage(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	public void shouldRenderExpirationInWarningColorWhenInvitationIsAboutToExpire()
	{
		InvitationModel invitation = invitationExpiringAt(Instant.now().plusSeconds(7 * 60 * 60));

		Span expiration = renderExpiration(invitation);

		assertThat(expiration.getStyle().get("color")).isEqualTo(WARNING_COLOR);
	}

	@Test
	public void shouldRenderExpirationInDefaultColorWhenResendIsAllowed()
	{
		InvitationModel invitation = invitationExpiringAt(Instant.now().plusSeconds(9 * 60 * 60));

		Span expiration = renderExpiration(invitation);

		assertThat(expiration.getStyle().get("color")).isBlank();
	}

	@Test
	public void shouldRenderExpiredInvitationExpirationInDefaultColor()
	{
		InvitationModel invitation = invitationExpiringAt(Instant.now().minusSeconds(1));

		Span expiration = renderExpiration(invitation);

		assertThat(expiration.getStyle().get("color")).isBlank();
	}

	private InvitationModel invitationExpiringAt(Instant expiration)
	{
		return new InvitationModel("code", "user@example.com", List.of(), null, expiration, "link");
	}

	private Span renderExpiration(InvitationModel invitation)
	{
		InvitationGrid grid = new InvitationGrid(msg, ignored -> new Span());
		Column<InvitationModel> expirationColumn = grid.getColumnByKey("expiration");
		ComponentRenderer<Span, InvitationModel> renderer =
				(ComponentRenderer<Span, InvitationModel>) expirationColumn.getRenderer();
		return renderer.createComponent(invitation);
	}
}
