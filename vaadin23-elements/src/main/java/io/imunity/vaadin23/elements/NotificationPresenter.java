/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.elements;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import static com.vaadin.flow.component.notification.Notification.Position.MIDDLE;
import static com.vaadin.flow.component.notification.Notification.Position.TOP_END;

public class NotificationPresenter
{
	public static void showCriticalError(Runnable logout, String header, String description)
	{
		ErrorNotification errorNotification = new ErrorNotification(header, description);
		errorNotification.closeButton.addClickListener(event -> logout.run());
		errorNotification.open();
	}

	public void showSuccess(String txt)
	{
		Notification notification = new Notification(txt, 5000, TOP_END);
		notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
		notification.open();
	}

	public void showError(String caption, String description)
	{
		new ErrorNotification(caption, description).open();
	}

	public void showWarning(String caption, String description)
	{
		ErrorNotification errorNotification = new ErrorNotification(caption, description);
		errorNotification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
		errorNotification.open();
	}

	private static class ErrorNotification extends Notification
	{
		private final Button closeButton;

		private ErrorNotification(String header, String description)
		{
			addThemeVariants(NotificationVariant.LUMO_ERROR);

			closeButton = new Button(new Icon("lumo", "cross"));
			closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
			closeButton.getElement().setAttribute("aria-label", "Close");
			closeButton.addClickListener(event -> close());

			Label label = new Label(header);
			label.getStyle().set("font-weight", "bold");
			HorizontalLayout layout = new HorizontalLayout(
					VaadinIcon.EXCLAMATION_CIRCLE.create(),
					new VerticalLayout(label, new Text(description)),
					closeButton
			);
			layout.setAlignItems(FlexComponent.Alignment.CENTER);

			add(layout);
			setPosition(MIDDLE);
		}
	}
}
