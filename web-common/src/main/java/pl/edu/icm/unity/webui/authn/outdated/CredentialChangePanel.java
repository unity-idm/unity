/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.outdated;

import org.apache.logging.log4j.util.Strings;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationMisconfiguredException;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationRequiredException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.authn.additional.AdditionalAuthnHandler;
import pl.edu.icm.unity.webui.authn.additional.AdditionalAuthnHandler.AuthnResult;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.credentials.MissingCredentialException;

/**
 * Panel allowing to set a credential.
 */
class CredentialChangePanel extends CustomComponent
{
	private EntityCredentialManagement ecredMan;
	private EntityManagement entityMan;
	private CredentialEditorRegistry credEditorReg;
	private UnityMessageSource msg;
	private boolean changed = false;
	private Entity entity;
	private final long entityId;
	private CredentialEditor credEditor;
	private ComponentsContainer credEditorComp;
	private CredentialDefinition toEdit;
	private final AdditionalAuthnHandler additionalAuthnHandler;
	
	CredentialChangePanel(UnityMessageSource msg, long entityId,
			EntityCredentialManagement ecredMan, 
			EntityManagement entityMan, CredentialEditorRegistry credEditorReg,
			CredentialDefinition toEdit, AdditionalAuthnHandler additionalAuthnHandler,
			CredentialChangeConfiguration uiConfig, Runnable updatedCallback, Runnable cancelHandler)
	{
		this.msg = msg;
		this.ecredMan = ecredMan;
		this.entityId = entityId;
		this.entityMan = entityMan;
		this.credEditorReg = credEditorReg;
		this.toEdit = toEdit;
		this.additionalAuthnHandler = additionalAuthnHandler;
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
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setWidthUndefined();
		wrapper.setMargin(false);
		
		if (!Strings.isEmpty(uiConfig.logoURL))
		{
			Resource logoResource = ImageUtils.getConfiguredImageResource(uiConfig.logoURL);
			Image image = new Image(null, logoResource);
			image.addStyleName("u-authn-logo");
			wrapper.addComponent(image);
			wrapper.setComponentAlignment(image, Alignment.TOP_CENTER);
		}
		
		Label info = new Label(msg.getMessage("OutdatedCredentialDialog.info"));
		info.addStyleName("u-outdatedcred-info");
		info.addStyleName(Styles.error.toString());
		info.addStyleName(Styles.textCenter.toString());
		info.setWidth(uiConfig.width * 2, Unit.EM);
		wrapper.addComponent(info);
		wrapper.setComponentAlignment(info, Alignment.TOP_CENTER);
		
		VerticalLayout fixedWidthCol = new VerticalLayout();
		fixedWidthCol.addStyleName("u-outdatedcred-fixedWidthCol");
		fixedWidthCol.setMargin(false);
		fixedWidthCol.setWidth(uiConfig.width, Unit.EM);
		Component editor = getEditorAsSingle(uiConfig);
		fixedWidthCol.addComponent(editor);
		fixedWidthCol.setComponentAlignment(editor, Alignment.MIDDLE_CENTER);
		
		fixedWidthCol.addComponent(new Label("&nbsp;", ContentMode.HTML));
		
		Button update = new Button(msg.getMessage("OutdatedCredentialDialog.update"));
		update.setWidth(100, Unit.PERCENTAGE);
		update.addStyleName("u-outdatedcred-update");
		update.addClickListener(event -> {
			if (updateCredential(false))
				updatedCallback.run();
		});
		update.setClickShortcut(KeyCode.ENTER);
		fixedWidthCol.addComponent(update);
		fixedWidthCol.setComponentAlignment(update, Alignment.MIDDLE_CENTER);
		
		Button cancel = new Button(msg.getMessage("cancel"));
		cancel.setStyleName(Styles.vButtonLink.toString());
		cancel.addStyleName("u-outdatedcred-cancel");
		cancel.addClickListener(e -> {
			cancelHandler.run();
		});
		fixedWidthCol.addComponent(cancel);
		fixedWidthCol.setComponentAlignment(cancel, Alignment.TOP_RIGHT);
		
		
		wrapper.addComponent(fixedWidthCol);
		wrapper.setComponentAlignment(fixedWidthCol, Alignment.TOP_CENTER);
		
		setCompositionRoot(wrapper);
		setWidthUndefined();
		addStyleName("u-outdatedcred-panel");
	}

	private Component getEditorAsSingle(CredentialChangeConfiguration uiConfig)
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(false);
		wrapper.addComponents(credEditorComp.getComponents());
		wrapper.setWidth(uiConfig.width, Unit.EM);
		return wrapper;
	}
	
	Component.Focusable getFocussedComponent()
	{
		for (Component component: credEditorComp.getComponents())
			if (component instanceof Component.Focusable)
				return (Focusable) component;
		return null;
	}
	
	public boolean isChanged()
	{
		return changed;
	}

	public boolean isEmptyEditor()
	{
		return credEditorComp.getComponents().length == 0;
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
			NotificationPopup.showError(e.getMessage(), "");
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
			NotificationPopup.showError(msg.getMessage("CredentialChangeDialog.credentialUpdateError"), 
					msg.getMessage("AdditionalAuthenticationMisconfiguredError"));
			return false;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"CredentialChangeDialog.credentialUpdateError"), e);
			return false;
		}
		credEditor.setCredentialError(null);
		if (showSuccess)
			NotificationPopup.showSuccess(msg.getMessage("CredentialChangeDialog.credentialUpdated"), "");
		changed = true;
		loadEntity(entityP);
		return true;
	}
	
	private void onAdditionalAuthnForUpdateCredential(AuthnResult result)
	{
		if (result == AuthnResult.SUCCESS)
		{
			updateCredential(true);
		} else if (result == AuthnResult.ERROR)
		{
			NotificationPopup.showError(msg.getMessage("CredentialChangeDialog.credentialUpdateError"), 
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
			NotificationPopup.showError(msg,
					msg.getMessage("CredentialChangeDialog.entityRefreshError"),
					e);
		}
	}
}
