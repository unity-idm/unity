/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NavigationTrigger;
import io.imunity.upman.front.UpmanViewComponent;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.upman.front.views.groups.GroupsView;
import io.imunity.upman.front.views.invitations.InvitationsView;
import io.imunity.upman.front.views.members.MembersView;
import io.imunity.upman.front.views.user_updates.UserUpdatesView;
import io.imunity.upman.utils.HomeServiceLinkService;
import io.imunity.upman.utils.ProjectService;
import io.imunity.vaadin.elements.MenuComponent;
import io.imunity.vaadin.endpoint.common.VaddinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.layout.UnityAppLayout;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.vaadin.flow.component.icon.VaadinIcon.*;
import static io.imunity.vaadin.elements.CssClassNames.LOGO_IMAGE;
import static io.imunity.vaadin.elements.CssClassNames.POINTER;
import static java.util.stream.Collectors.toList;

public class UpManMenu extends UnityAppLayout implements BeforeEnterObserver
{
	private final ProjectService projectService;
	private final ProjectsLayout projectsLayout;
	private Optional<UpmanViewComponent> currentView = Optional.empty();

	@Autowired
	public UpManMenu(VaddinWebLogoutHandler standardWebLogoutHandler, ProjectService projectService, MessageSource msg,
	                 HomeServiceLinkService homeServiceLinkService, UnityServerConfiguration unityServerConfiguration)
	{
		super(Stream.of(
						MenuComponent.builder(MembersView.class).tabName(msg.getMessage("UpManMenu.members"))
								.icon(FAMILY).build(),
						MenuComponent.builder(GroupsView.class).tabName(msg.getMessage("UpManMenu.groups"))
								.icon(FILE_TREE).build(),
						MenuComponent.builder(InvitationsView.class).tabName(msg.getMessage("UpManMenu.invitations"))
								.icon(ENVELOPE_OPEN_O).build(),
						MenuComponent.builder(UserUpdatesView.class).tabName(msg.getMessage("UpManMenu.userUpdates"))
								.icon(USER_CHECK).build()
						)
						.collect(toList()), standardWebLogoutHandler, msg, createHomeIcon(homeServiceLinkService), unityServerConfiguration
		);
		this.projectService = projectService;

		HorizontalLayout imageLayout = new HorizontalLayout();
		imageLayout.getStyle().set("margin-top", "1.5em");
		imageLayout.getStyle().set("margin-bottom", "1.5em");


		List<ProjectGroup> projectGroups = projectService.getProjectForUser(InvocationContext.getCurrent().getLoginSession().getEntityId());

		super.initView();

		projectsLayout = new ProjectsLayout(msg, projectGroups, imageLayout);
		addToLeftContainerAsFirst(projectsLayout);
		addToLeftContainerAsFirst(imageLayout);
	}

	private static List<Component> createHomeIcon(HomeServiceLinkService homeServiceLinkService)
	{
		return homeServiceLinkService.getHomeLinkIfAvailable()
				.map(UpManMenu::createHomeIcon)
				.stream().collect(toList());
	}

	private static Component createHomeIcon(String url)
	{
		Icon home = VaadinIcon.HOME.create();
		home.addClassName(POINTER.getName());
		home.addClickListener(event -> UI.getCurrent().getPage().setLocation(url));
		return home;
	}

	@Override
	public void showRouterLayoutContent(HasElement content)
	{
		super.showRouterLayoutContent(content);
		currentView = Optional.of((UpmanViewComponent) content);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
	{
		if(beforeEnterEvent.getTrigger().equals(NavigationTrigger.PROGRAMMATIC))
		{
			projectsLayout.load(projectService.getProjectForUser(InvocationContext.getCurrent().getLoginSession().getEntityId()));
			ComponentUtil.setData(UI.getCurrent(), ProjectGroup.class, projectsLayout.selectedProject);
			currentView.ifPresent(UpmanViewComponent::loadData);
		}
		if(ComponentUtil.getData(UI.getCurrent(), ProjectGroup.class) == null)
		{
			if (projectsLayout.selectedProject == null)
			{
				beforeEnterEvent.rerouteToError(IllegalAccessException.class);
				return;
			}
			ComponentUtil.setData(UI.getCurrent(), ProjectGroup.class, projectsLayout.selectedProject);
		}
	}

	class ProjectsLayout extends HorizontalLayout
	{
		private final MessageSource msg;
		private final HorizontalLayout imageLayout;

		private ProjectGroup selectedProject;

		ProjectsLayout(MessageSource msg, List<ProjectGroup> projectGroups, HorizontalLayout imageLayout)
		{
			this.msg = msg;
			this.imageLayout = imageLayout;

			setAlignItems(Alignment.CENTER);
			setJustifyContentMode(JustifyContentMode.CENTER);
			getStyle().set("margin-bottom", "1.5em");

			load(projectGroups);
		}

		private void load(List<ProjectGroup> projectGroups)
		{
			removeAll();
			if(projectGroups.size() == 1)
				add(createLabel(projectGroups));
			else
				add(createComboBox(projectGroups));
		}

		private ComboBox<ProjectGroup> createComboBox(List<ProjectGroup> projectGroups)
		{
			ComboBox<ProjectGroup> comboBox = new ComboBox<>();
			comboBox.setClassName("project-combo-box");
			comboBox.setLabel(msg.getMessage("UpManMenu.projectNameCaption"));

			comboBox.addValueChangeListener(event ->
			{
				if(event.getValue() == null)
				{
					comboBox.setValue(event.getOldValue());
					return;
				}
				selectedProject = event.getValue();
				ComponentUtil.setData(UI.getCurrent(), ProjectGroup.class, event.getValue());
				currentView.ifPresent(UpmanViewComponent::loadData);
				setImage(event.getValue());
			});

			comboBox.setItemLabelGenerator(projectGroup -> projectGroup.displayedName);
			comboBox.setItems(projectGroups);
			comboBox.setClearButtonVisible(false);
			if(projectGroups.iterator().hasNext())
				comboBox.setValue(projectGroups.iterator().next());

			return comboBox;
		}

		private void setImage(ProjectGroup projectGroup)
		{
			Image image = projectService.getProjectLogoFallbackToEmptyImage(projectGroup);
			image.addClassName(LOGO_IMAGE.getName());
			imageLayout.removeAll();
			imageLayout.add(image);
		}

		private Span createLabel(List<ProjectGroup> projectGroups)
		{
			ProjectGroup projectGroup = projectGroups.iterator().next();
			selectedProject = projectGroup;
			Span label = new Span(msg.getMessage("UpManMenu.projectNameCaption") + " " + projectGroup.displayedName);
			label.getStyle().set("color", "white");
			setImage(selectedProject);
			return label;
		}
	}
}
