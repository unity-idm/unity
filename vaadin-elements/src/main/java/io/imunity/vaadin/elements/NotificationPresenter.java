/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import static com.vaadin.flow.component.notification.Notification.Position.MIDDLE;
import static com.vaadin.flow.component.notification.Notification.Position.TOP_CENTER;

public class NotificationPresenter
{

	private static final int DEFAULT_NOTIFICATION_DURATION = 5000;

	public static void showCriticalError(Runnable logout, String header, String description)
	{
		ErrorNotification errorNotification = new ErrorNotification(header, description);
		errorNotification.closeButton.addClickListener(event -> logout.run());
		errorNotification.setPosition(MIDDLE);
		errorNotification.open();
	}

	public void showSuccess(String txt)
	{
		Notification notification = new Notification(txt, DEFAULT_NOTIFICATION_DURATION, TOP_CENTER);
		notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
		notification.setDuration(DEFAULT_NOTIFICATION_DURATION);
		notification.open();
	}

	public void showSuccess(String caption, String description)
	{
		SuccessNotification successNotification = new SuccessNotification(caption, description);
		successNotification.setDuration(DEFAULT_NOTIFICATION_DURATION);
		successNotification.open();
	}

	public void showError(String caption, String description)
	{
		ErrorNotification errorNotification = new ErrorNotification(caption, description);
		errorNotification.setDuration(DEFAULT_NOTIFICATION_DURATION);
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
					new VerticalLayout(label, new Text(description)),
					closeButton
			);
			layout.setAlignItems(FlexComponent.Alignment.CENTER);

			add(layout);
			setPosition(TOP_CENTER);
		}
	}

	private static class SuccessNotification extends Notification
	{
		private SuccessNotification(String header, String description)
		{
			addThemeVariants(NotificationVariant.LUMO_SUCCESS);

			Button closeButton = new Button(new Icon("lumo", "cross"));
			closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
			closeButton.getElement().setAttribute("aria-label", "Close");
			closeButton.addClickListener(event -> close());

			Label label = new Label(header);
			label.getStyle().set("font-weight", "bold");
			HorizontalLayout layout = new HorizontalLayout(
					new VerticalLayout(label, new Text(description)),
					closeButton
			);
			layout.setAlignItems(FlexComponent.Alignment.CENTER);

			add(layout);
			setPosition(TOP_CENTER);
		}
	}
}
