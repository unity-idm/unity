/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.imunity.upman.av23.components.ProjectController23;
import io.imunity.upman.av23.components.Vaddin23WebLogoutHandler;
import io.imunity.upman.av23.front.UnityAppLayout;
import io.imunity.upman.av23.front.components.MenuComponent;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.util.Map;
import java.util.stream.Stream;

import static com.vaadin.flow.component.icon.VaadinIcon.*;
import static com.vaadin.flow.component.notification.Notification.Position.MIDDLE;
import static java.util.stream.Collectors.toList;

public class UpManMenu extends UnityAppLayout
{
	@Autowired
	public UpManMenu(Vaddin23WebLogoutHandler standardWebLogoutHandler, ProjectController23 projectController, MessageSource msg) {
		super(Stream.of(
						MenuComponent.builder(MembersView.class).tabName(msg.getMessage("UpManMenu.members"))
								.icon(FAMILY).build(),
						MenuComponent.builder(GroupsView.class).tabName(msg.getMessage("UpManMenu.groups"))
								.icon(ENVELOPE_OPEN_O).build(),
						MenuComponent.builder(InvitationsView.class).tabName(msg.getMessage("UpManMenu.invitations"))
								.icon(FILE_TREE).build(),
						MenuComponent.builder(UserUpdatesView.class).tabName(msg.getMessage("UpManMenu.userUpdates"))
								.icon(USER_CHECK).build()
						)
						.collect(toList()), standardWebLogoutHandler
		);
		HorizontalLayout imageLayout = new HorizontalLayout();
		imageLayout.getStyle().set("margin-top", "1.5em");
		imageLayout.getStyle().set("margin-bottom", "1.5em");


		Map<String, String> projectIdToProjectName;
		try
		{
			projectIdToProjectName = projectController.getProjectForUser(InvocationContext.getCurrent().getLoginSession().getEntityId());
		} catch (ControllerException e)
		{
			handleNotification(standardWebLogoutHandler, e);
			return;
		}

		super.initView();

		HorizontalLayout comboBoxLayout = createComboBoxLayout(projectController, msg, projectIdToProjectName, imageLayout);

		addToLeftContainerAsFirst(comboBoxLayout);
		addToLeftContainerAsFirst(imageLayout);
	}

	private void handleNotification(Vaddin23WebLogoutHandler standardWebLogoutHandler, ControllerException e)
	{
		Notification notification = new Notification();
		notification.addThemeVariants(NotificationVariant.LUMO_ERROR);

		Div text = new Div(new Text(e.getCaption()));

		Button closeButton = new Button(new Icon("lumo", "cross"));
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
		closeButton.getElement().setAttribute("aria-label", "Close");
		closeButton.addClickListener(event -> standardWebLogoutHandler.logout());

		HorizontalLayout layout = new HorizontalLayout(VaadinIcon.EXCLAMATION_CIRCLE.create(), text, closeButton);
		layout.setAlignItems(Alignment.CENTER);

		notification.add(layout);
		notification.setPosition(MIDDLE);
		notification.open();
	}

	public HorizontalLayout createComboBoxLayout(ProjectController23 projectController, MessageSource msg, Map<String, String> projectIdToProjectName, HorizontalLayout imageLayout)
	{
		ComboBox<String> comboBox = new ComboBox<>();
		comboBox.setLabel(msg.getMessage("UpManMenu.projectNameCaption"));

		HorizontalLayout comboBoxLayout = new HorizontalLayout(comboBox);
		comboBoxLayout.setAlignItems(Alignment.CENTER);
		comboBoxLayout.setJustifyContentMode(JustifyContentMode.CENTER);
		comboBoxLayout.getStyle().set("margin-bottom", "1.5em");

		comboBox.addValueChangeListener(event ->
			{
				if(event.getValue() == null)
				{
					comboBox.setValue(event.getOldValue());
					return;
				}
				Image image = new Image(projectController.getProjectLogo(event.getValue()), "");
				image.setId("unity-logo-image");
				imageLayout.removeAll();
				imageLayout.add(image);
			});

		comboBox.setItemLabelGenerator(projectIdToProjectName::get);
		comboBox.setItems(projectIdToProjectName.keySet());
		comboBox.setClearButtonVisible(false);
		String key = projectIdToProjectName.entrySet().iterator().next().getKey();
		comboBox.setValue(key);

		return comboBoxLayout;
	}
}
