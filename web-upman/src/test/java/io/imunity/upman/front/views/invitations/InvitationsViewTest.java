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
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;

import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.upman.utils.ProjectService;
import pl.edu.icm.unity.base.message.MessageSource;

@ExtendWith(MockitoExtension.class)
public class InvitationsViewTest
{
	@Mock
	private MessageSource msg;
	@Mock
	private ProjectInvitationsService invitationsService;
	@Mock
	private ProjectService projectService;

	private ProjectGroup projectGroup;

	@BeforeEach
	public void setUp()
	{
		when(msg.getMessage(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		projectGroup = new ProjectGroup("/project", "project", "regForm", "signupForm");
		UI.setCurrent(new UI());
		ComponentUtil.setData(UI.getCurrent(), ProjectGroup.class, projectGroup);
	}

	@AfterEach
	public void cleanUp()
	{
		UI.setCurrent(null);
	}

	@Test
	public void shouldClearStaleSelectionWhenInvitationsAreReloaded()
	{
		InvitationModel oldInvitation = new InvitationModel("old-code", "user@example.com", List.of(), null,
				Instant.now().plusSeconds(60), "old-link");
		InvitationModel replacementInvitation = new InvitationModel("new-code", "user@example.com", List.of(), null,
				Instant.now().plusSeconds(24 * 60 * 60), "new-link");
		when(invitationsService.getInvitations(projectGroup))
				.thenReturn(List.of(oldInvitation))
				.thenReturn(List.of(replacementInvitation));
		InvitationsView view = new InvitationsView(msg, invitationsService, projectService);

		view.loadData();
		Grid<InvitationModel> grid = getGrid(view);
		grid.select(oldInvitation);

		view.loadData();

		assertThat(grid.getSelectedItems()).isEmpty();
	}

	@SuppressWarnings("unchecked")
	private Grid<InvitationModel> getGrid(Component component)
	{
		return (Grid<InvitationModel>) getComponentTree(component)
				.filter(Grid.class::isInstance)
				.findFirst()
				.orElseThrow();
	}

	private Stream<Component> getComponentTree(Component component)
	{
		return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::getComponentTree));
	}
}
