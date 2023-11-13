/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.outdated;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.auth.additional.AdditionalAuthnHandler;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorContext;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationMisconfiguredException;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationRequiredException;
import pl.edu.icm.unity.webui.common.credentials.MissingCredentialException;

import java.util.Optional;

import static io.imunity.vaadin.elements.VaadinClassNames.LOGO_IMAGE;

/**
 * Panel allowing to set a credential.
 */
class CredentialChangePanel extends VerticalLayout
{
	private final VaadinLogoImageLoader imageAccessService;
	private final EntityCredentialManagement ecredMan;
	private final EntityManagement entityMan;
	private final CredentialEditorRegistry credEditorReg;
	private final MessageSource msg;
	private boolean changed = false;
	private Entity entity;
	private final long entityId;
	private CredentialEditor credEditor;
	private ComponentsContainer credEditorComp;
	private final CredentialDefinition toEdit;
	private final AdditionalAuthnHandler additionalAuthnHandler;
	private final NotificationPresenter notificationPresenter;

	CredentialChangePanel(MessageSource msg, long entityId, VaadinLogoImageLoader imageAccessService,
	                      EntityCredentialManagement ecredMan,
	                      EntityManagement entityMan, CredentialEditorRegistry credEditorReg,
	                      CredentialDefinition toEdit, AdditionalAuthnHandler additionalAuthnHandler,
	                      CredentialChangeConfiguration uiConfig, Runnable updatedCallback, Runnable cancelHandler,
	                      NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.imageAccessService = imageAccessService;
		this.ecredMan = ecredMan;
		this.entityId = entityId;
		this.entityMan = entityMan;
		this.credEditorReg = credEditorReg;
		this.toEdit = toEdit;
		this.additionalAuthnHandler = additionalAuthnHandler;
		this.notificationPresenter = notificationPresenter;
		loadEntity(new EntityParam(entityId));
		init(uiConfig, updatedCallback, cancelHandler);
	}

	private void init(CredentialChangeConfiguration uiConfig, Runnable updatedCallback, Runnable cancelHandler)
	{
		credEditor = credEditorReg.getEditor(toEdit.getTypeId());
		credEditorComp = credEditor.getEditor(CredentialEditorContext.builder()
				.withConfiguration(toEdit.getConfiguration())
				.withRequired(true)
				.withEntityId(entityId)
				.withAdminMode(false)
				.withCustomWidth(uiConfig.width)
				.withCustomWidthUnit(Unit.EM)
				.withShowLabelInline(uiConfig.compactLayout)
				.build());
		setMargin(false);
		
		Optional<Image> logo = imageAccessService.loadImageFromUri(uiConfig.logoURL);
		logo.ifPresent(logoImg -> logoImg.addClassName(LOGO_IMAGE.getName()));
		logo.ifPresent(this::add);

		Span info = new Span(msg.getMessage("OutdatedCredentialDialog.info"));
		info.addClassName("u-outdatedcred-info");
		info.setWidth(uiConfig.width * 2, Unit.EM);
		info.getStyle().set("color", "red");
		info.getStyle().set("text-align", "center");
		add(info);
		setAlignItems(Alignment.CENTER);
		
		VerticalLayout fixedWidthCol = new VerticalLayout();
		fixedWidthCol.addClassName("u-outdatedcred-fixedWidthCol");
		fixedWidthCol.setPadding(false);
		fixedWidthCol.setWidth(uiConfig.width, Unit.EM);
		Component editor = getEditorAsSingle(uiConfig);
		fixedWidthCol.add(editor);
		fixedWidthCol.setAlignItems(Alignment.CENTER);

		Button update = new Button(msg.getMessage("OutdatedCredentialDialog.update"));
		update.setWidthFull();
		update.addClassName("u-outdatedcred-update");
		update.addClickListener(event -> {
			if (updateCredential(false))
				updatedCallback.run();
		});
		update.addClickShortcut(Key.ENTER);
		fixedWidthCol.add(update);

		LinkButton cancel = new LinkButton(msg.getMessage("cancel"), e -> cancelHandler.run());
		cancel.addClassName("u-outdatedcred-cancel");
		cancel.getStyle().set("align-self", "end");
		fixedWidthCol.add(cancel);
		
		add(fixedWidthCol);
		addClassName("u-outdatedcred-panel");
	}

	private Component getEditorAsSingle(CredentialChangeConfiguration uiConfig)
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setPadding(false);
		wrapper.add(credEditorComp.getComponents());
		wrapper.setWidth(uiConfig.width, Unit.EM);
		return wrapper;
	}
	
	Focusable getFocussedComponent()
	{
		for (Component component: credEditorComp.getComponents())
			if (component instanceof Focusable)
				return (Focusable) component;
		return null;
	}
	
	public boolean isChanged()
	{
		return changed;
	}
	
	public boolean updateCredential(boolean showSuccess)
	{
		String secrets;
		try
		{
			secrets = credEditor.getValue();
		} catch (MissingCredentialException mc)
		{
			return false;
		} catch (IllegalCredentialException e)
		{
			notificationPresenter.showError(e.getMessage(), "");
			return false;
		}
		EntityParam entityP = new EntityParam(entity.getId());
		try
		{
			ecredMan.setEntityCredential(entityP, toEdit.getName(), secrets);
		} catch (IllegalCredentialException e)
		{
			credEditor.setCredentialError(e);
			return false;
		} catch (AdditionalAuthenticationRequiredException additionalAuthn)
		{
			additionalAuthnHandler.handleAdditionalAuthenticationException(additionalAuthn, 
					msg.getMessage("CredentialChangeDialog.additionalAuthnRequired"), 
					msg.getMessage("CredentialChangeDialog.additionalAuthnRequiredInfo"),
					this::onAdditionalAuthnForUpdateCredential);
			return false;
		} catch (AdditionalAuthenticationMisconfiguredException misconfigured)
		{
			notificationPresenter.showError(msg.getMessage("CredentialChangeDialog.credentialUpdateError"),
					msg.getMessage("AdditionalAuthenticationMisconfiguredError"));
			return false;
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage(
					"CredentialChangeDialog.credentialUpdateError"), e.getMessage());
			return false;
		}
		credEditor.setCredentialError(null);
		if (showSuccess)
			notificationPresenter.showSuccess(msg.getMessage("CredentialChangeDialog.credentialUpdated"));
		changed = true;
		loadEntity(entityP);
		return true;
	}
	
	private void onAdditionalAuthnForUpdateCredential(AdditionalAuthnHandler.AuthnResult result)
	{
		if (result == AdditionalAuthnHandler.AuthnResult.SUCCESS)
		{
			updateCredential(true);
		} else if (result == AdditionalAuthnHandler.AuthnResult.ERROR)
		{
			notificationPresenter.showError(msg.getMessage("CredentialChangeDialog.credentialUpdateError"),
					msg.getMessage("CredentialChangeDialog.additionalAuthnFailed"));
			credEditor.setCredentialError(null);
		} else 
		{
			credEditor.setCredentialError(null);
		}
	}

	
	private void loadEntity(EntityParam entityP)
	{
		try
		{
			entity = entityMan.getEntity(entityP);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("CredentialChangeDialog.entityRefreshError"),
					e.getMessage());
		}
	}
}
