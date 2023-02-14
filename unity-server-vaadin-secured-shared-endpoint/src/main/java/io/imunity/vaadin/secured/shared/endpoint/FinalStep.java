/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.secured.shared.endpoint;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.QueryParameters;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.secured.shared.endpoint.wizard.WizardStep;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;

import static com.vaadin.flow.component.notification.Notification.Position.MIDDLE;

class FinalStep extends WizardStep
{
	private final InputTranslationEngine translationEngine;
	private final NotificationPresenter notificationPresenter;
	private final MessageSource msg;

	private RemotelyAuthenticatedPrincipal authnContext;

	public FinalStep(String label, Component component,
	                 InputTranslationEngine translationEngine, NotificationPresenter notificationPresenter, MessageSource msg)
	{
		super(label, component);
		this.translationEngine = translationEngine;
		this.notificationPresenter = notificationPresenter;
		this.msg = msg;
	}

	@Override
	protected void initialize()
	{
		if (authnContext == null)
			return;
			
		LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
		try
		{
			translationEngine.mergeWithExisting(authnContext.getMappingResult(), 
					new EntityParam(loginSession.getEntityId()));
			showSuccess(msg.getMessage("ConnectId.ConfirmStep.mergeSuccessfulCaption"),
					msg.getMessage("ConnectId.ConfirmStep.mergeSuccessful"));
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("ConnectId.ConfirmStep.mergeFailed"), e.getMessage());
		}
	}

	void prepareStep(RemotelyAuthenticatedPrincipal authnContext)
	{
		this.authnContext = authnContext;
	}

	private void showSuccess(String header, String description)
	{
		Notification notification = new Notification("", 5000, MIDDLE);
		Label label = new Label(header);
		label.getStyle().set("font-weight", "bold");
		HorizontalLayout layout = new HorizontalLayout(
				VaadinIcon.INFO_CIRCLE_O.create(),
				new VerticalLayout(label, new Text(description))
		);
		layout.setAlignItems(FlexComponent.Alignment.CENTER);
		notification.add(layout);
		notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
		notification.addOpenedChangeListener(e -> {
			if(!e.isOpened())
				UI.getCurrent().navigate(StatusView.class, QueryParameters.of("info", msg.getMessage("Wizard.finished")));
		});
		notification.open();
	}

}
