/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.devicesignin;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.endpoint.common.VaadinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.consent_utils.ExposedAttributesComponent;
import io.imunity.vaadin.endpoint.common.consent_utils.IdPButtonsBar;
import io.imunity.vaadin.endpoint.common.consent_utils.IdentitySelectorComponent;
import io.imunity.vaadin.endpoint.common.consent_utils.SPInfoComponent;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope;

/**
 * Consent screen shown on device sign-in after the resource owner has logged in. A deliberately
 * reduced variant of {@code OAuthConsentScreen}: no remembered-consent, no active value selection,
 * no policy agreements - RFC 8628 device pairing is always interactively confirmed.
 */
class DeviceSignInConsentScreen extends VerticalLayout
{
	private final IdentitySelectorComponent idSelector;
	private final ExposedAttributesComponent attrsPresenter;
	private final Runnable declineHandler;
	private final BiConsumer<IdentityParam, Collection<DynamicAttribute>> acceptHandler;

	DeviceSignInConsentScreen(MessageSource msg, AttributeHandlerRegistry handlersRegistry,
			VaadinWebLogoutHandler authnProcessor, IdentityTypeSupport idTypeSupport, String clientName,
			Image clientLogo, List<RequestedOAuthScope> effectiveScope, IdentityParam identity,
			Collection<DynamicAttribute> attributes, String logoutRedirectPath, Runnable declineHandler,
			BiConsumer<IdentityParam, Collection<DynamicAttribute>> acceptHandler)
	{
		this.declineHandler = declineHandler;
		this.acceptHandler = acceptHandler;

		setMargin(false);
		setSpacing(false);
		setAlignItems(Alignment.CENTER);

		VerticalLayout contents = new VerticalLayout();
		contents.addClassName("u-consentMainColumn");
		contents.setAlignItems(Alignment.CENTER);
		add(contents);

		SPInfoComponent spInfo = new SPInfoComponent(msg, clientLogo, clientName, null);
		contents.add(spInfo);

		Div exposedInfoPanel = new Div();
		exposedInfoPanel.setClassName("u-consent-screen");
		contents.add(exposedInfoPanel);
		VerticalLayout eiLayout = new VerticalLayout();
		eiLayout.setWidthFull();
		exposedInfoPanel.add(eiLayout);

		for (RequestedOAuthScope si : effectiveScope)
		{
			String label = Strings.isNullOrEmpty(si.scopeDefinition().description()) ? si.scope()
					: si.scopeDefinition().description();
			eiLayout.add(new Span("● " + label));
		}

		idSelector = new IdentitySelectorComponent(msg, idTypeSupport, Lists.newArrayList(identity));
		attrsPresenter = new ExposedAttributesComponent(msg, idTypeSupport, handlersRegistry, attributes,
				Optional.of(identity));
		eiLayout.add(attrsPresenter);

		IdPButtonsBar buttons = new IdPButtonsBar(msg, authnProcessor, logoutRedirectPath, action ->
		{
			if (IdPButtonsBar.Action.ACCEPT == action)
				confirm();
			else if (IdPButtonsBar.Action.DENY == action)
				decline();
		});
		contents.add(buttons);
		buttons.setClassName("u-consent-screen-buttons");
		buttons.setAlignItems(Alignment.CENTER);
	}

	private void decline()
	{
		declineHandler.run();
	}

	private void confirm()
	{
		Collection<DynamicAttribute> filtered = attrsPresenter.getUserFilteredAttributes();
		IdentityParam selected = idSelector.getSelectedIdentity();
		acceptHandler.accept(selected, filtered);
	}
}
