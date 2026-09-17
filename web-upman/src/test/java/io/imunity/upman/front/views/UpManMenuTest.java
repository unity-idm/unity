/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.upman.utils.ProjectService;
import pl.edu.icm.unity.base.message.MessageSource;

class UpManMenuTest
{
	@Test
	void shouldClearSelectedProjectWhenProjectsBecomeEmpty()
	{
		// given
		ProjectService projectService = mock(ProjectService.class);
		ProjectGroup project = new ProjectGroup("/project", "Project", "registrationForm", "signupEnquiryForm");
		when(projectService.getProjectLogoFallbackToEmptyImage(project)).thenReturn(new Image());
		HorizontalLayout imageLayout = new HorizontalLayout();
		UpManMenu.ProjectsLayout projectsLayout = new UpManMenu.ProjectsLayout(mock(MessageSource.class),
				List.of(project), imageLayout, projectService, () -> { });

		assertThat(projectsLayout.getSelectedProject()).isEqualTo(project);
		assertThat(imageLayout.getComponentCount()).isOne();

		// when
		projectsLayout.load(List.of());

		// then
		assertThat(projectsLayout.getSelectedProject()).isNull();
		assertThat(projectsLayout.getComponentCount()).isZero();
		assertThat(imageLayout.getComponentCount()).isZero();
	}
}
