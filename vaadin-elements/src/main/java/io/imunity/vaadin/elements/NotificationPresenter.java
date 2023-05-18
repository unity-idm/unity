/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.dom.DomListenerRegistration;

import static com.vaadin.flow.component.notification.Notification.Position.MIDDLE;
import static com.vaadin.flow.component.notification.Notification.Position.TOP_END;

public class NotificationPresenter
{

	private static final int DEFAULT_NOTIFICATION_DURATION = 5000;

	public static void showCriticalError(Runnable logout, String header, String description)
	{
		ErrorNotification errorNotification = new ErrorNotification(header, description);
		errorNotification.closeButton.addClickListener(event -> logout.run());
		errorNotification.open();
	}

	public void showSuccess(String txt)
	{
		Notification notification = new Notification(txt, DEFAULT_NOTIFICATION_DURATION, TOP_END);
		notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
		notification.open();
	}

	public void showSuccess(String caption, String description)
	{
		SuccessNotification successNotification = new SuccessNotification(caption, description);
		successNotification.open();
	}

	public void showSuccessAutoClosing(String caption, String description)
	{
		SuccessNotification successNotification = new SuccessNotification(caption, description);
		successNotification.setDuration(DEFAULT_NOTIFICATION_DURATION);
		successNotification.setPosition(TOP_END);
		successNotification.open();
	}

	public void showError(String caption, String description)
	{
		ErrorNotification errorNotification = new ErrorNotification(caption, description);
		errorNotification.open();
		DomListenerRegistration clickListener = UI.getCurrent().getElement()
				.addEventListener("click", e -> errorNotification.close());
		errorNotification.addOpenedChangeListener(e ->
		{
			if(!e.isOpened())
				clickListener.remove();
		});
	}

	public void showErrorAutoClosing(String caption, String description)
	{
		ErrorNotification errorNotification = new ErrorNotification(caption, description);
		errorNotification.setDuration(DEFAULT_NOTIFICATION_DURATION);
		errorNotification.open();
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
					VaadinIcon.CHECK.create(),
					new VerticalLayout(label, new Text(description)),
					closeButton
			);
			layout.setAlignItems(FlexComponent.Alignment.CENTER);

			add(layout);
			setPosition(MIDDLE);
		}
	}
}
