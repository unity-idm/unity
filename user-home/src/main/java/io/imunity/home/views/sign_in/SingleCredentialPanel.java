/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.home.views.sign_in;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.auth.additional.AdditionalAuthnHandler;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorContext;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationMisconfiguredException;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationRequiredException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.CredentialType;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Single credential editor with credential extra info
 */
class SingleCredentialPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SingleCredentialPanel.class);
	private final EntityCredentialManagement ecredMan;
	private final EntityManagement entityMan;
	private final CredentialEditorRegistry credEditorReg;
	private final CredentialManagement credMan;
	private final MessageSource msg;
	private Entity entity;
	private final long entityId;
	private final boolean enableAdminOptions;
	private VerticalLayout credentialExtraInfo;
	private LinkButton edit;
	private LinkButton clear;
	private LinkButton invalidate;
	private CredentialEditor credEditor;
	private SingleCredentialEditComponent credEditorPanel;
	private final CredentialDefinition toEdit;
	private final AdditionalAuthnHandler additionalAuthnHandler;
	private final NotificationPresenter notificationPresenter;
	private final Runnable refresh;
	private Component actionsBar;
	
	
	SingleCredentialPanel(AdditionalAuthnHandler additionalAuthnHandler, MessageSource msg, long entityId,
								 EntityCredentialManagement ecredMan, CredentialManagement credMan,
								 EntityManagement entityMan, CredentialEditorRegistry credEditorReg,
								 CredentialDefinition toEdit, boolean enableAdminActions,
								 NotificationPresenter notificationPresenter, Runnable refresh)
	{
		this.additionalAuthnHandler = additionalAuthnHandler;
		this.msg = msg;
		this.ecredMan = ecredMan;
		this.credMan = credMan;
		this.entityId = entityId;
		this.entityMan = entityMan;
		this.credEditorReg = credEditorReg;
		this.enableAdminOptions = enableAdminActions;
		this.toEdit = toEdit;
		this.notificationPresenter = notificationPresenter;
		this.refresh = refresh;
		loadEntity(new EntityParam(entityId));
		init();
	}

	private void init()
	{
		setPadding(false);
		credentialExtraInfo = new VerticalLayout();
		credentialExtraInfo.setPadding(false);
		
		credEditor = credEditorReg.getEditor(toEdit.getTypeId());
		
		ComponentsContainer editorComponents = credEditor.getEditor(CredentialEditorContext.builder()
				.withConfiguration(toEdit.getConfiguration())
				.withRequired(true)
				.withEntityId(entityId)
				.withAdminMode(enableAdminOptions)
				.withCustomWidth(SingleCredentialEditComponent.WIDTH)
				.withCustomWidthUnit(Unit.EM)
				.withCredentialName(toEdit.getName())
				.build());
		credEditorPanel = new SingleCredentialEditComponent(msg, editorComponents, this::onCredentialUpdate, 
				this::hideEditor);
		credEditorPanel.setVisible(false);

		add(new Label(toEdit.getTypeId()));
		add(credentialExtraInfo, credEditorPanel);
		actionsBar = createActionsBar();
		add(actionsBar);

		updateCredentialStatus();
	}
	
	private Component createActionsBar()
	{
		clear = new LinkButton(msg.getMessage("CredentialChangeDialog.clear"), e -> clearCredential());
		invalidate = new LinkButton(msg.getMessage("CredentialChangeDialog.invalidate"),
				ne -> changeCredentialStatusWithConfirm(LocalCredentialState.outdated));
		edit = new LinkButton(msg.getMessage("CredentialChangeDialog.setup"), e -> switchEditorVisibility(true));
		
		HorizontalLayout buttonsBar = new HorizontalLayout();
		buttonsBar.setPadding(false);
		buttonsBar.add(edit);
		if (enableAdminOptions)
		{
			buttonsBar.add(clear);
			if (isInvalidationSupported(toEdit.getTypeId()))
				buttonsBar.add(invalidate);
		}
		return buttonsBar;
	}
	
	private boolean isInvalidationSupported(String credType)
	{
		try
		{
			Collection<CredentialType> credentialTypes = credMan.getCredentialTypes();
			for (CredentialType type : credentialTypes)
			{
				if (type.getName().equals(credType))
				{
					return type.isSupportingInvalidation();
				}
			}
		} catch (EngineException e)
		{
			log.warn("Can not get credential type " + credType, e);
		}
		return false;
	}

	private void updateCredentialStatus()
	{
		Map<String, CredentialPublicInformation> s = entity.getCredentialInfo()
				.getCredentialsState();
		CredentialPublicInformation credPublicInfo = s.get(toEdit.getName());

		Optional<Component> viewer = credEditor.getViewer(credPublicInfo.getExtraInformation());
		credentialExtraInfo.removeAll();
		if (viewer.isPresent())
		{
			credentialExtraInfo.add(viewer.get());
			credentialExtraInfo.setVisible(true);
		} else
		{
			credentialExtraInfo.setVisible(false);
		}
		
		updateEditCaption(credPublicInfo.getState());
		if (enableAdminOptions)
			updateAdminButtonsState(credPublicInfo.getState());
	}

	private void updateEditCaption(LocalCredentialState state)
	{
		String captionKey = state == LocalCredentialState.notSet ? "CredentialChangeDialog.setup" 
				: "CredentialChangeDialog.change";
		edit.setText(msg.getMessage(captionKey));
	}
	
	private void updateAdminButtonsState(LocalCredentialState state)
	{
		if (state == LocalCredentialState.notSet)
		{
			clear.setVisible(false);
			invalidate.setVisible(false);
		} else if (state == LocalCredentialState.outdated)
		{
			clear.setVisible(true);
			invalidate.setVisible(false);
		} else
		{
			clear.setVisible(true);
			invalidate.setVisible(true);
		}
	}

	private void hideEditor()
	{
		switchEditorVisibility(false);
	}

	private void switchEditorVisibility(boolean editorVisible)
	{
		if (credentialExtraInfo.getComponentCount() > 0)
			credentialExtraInfo.setVisible(!editorVisible);
		actionsBar.setVisible(!editorVisible);
		credEditorPanel.setVisible(editorVisible);
		if (editorVisible)
			credEditorPanel.focusEditor();
	}

	private void clearCredential()
	{
		changeCredentialStatusWithConfirm(LocalCredentialState.notSet);
	}
	
	private void onCredentialUpdate()
	{
		boolean updated = updateCredential();
		if (updated)
			refresh.run();
	}
	
	private boolean updateCredential()
	{
		if (credEditor.isCredentialCleared()){
			clearCredential();
			return true;
		}

		String secrets;
		try
		{
			secrets = credEditor.getValue();
		} catch (IllegalCredentialException e)
		{
			notificationPresenter.showError(msg
					.getMessage("CredentialChangeDialog.credentialUpdateError"),
					e.getMessage());
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
		notificationPresenter.showSuccess(msg.getMessage("CredentialChangeDialog.credentialUpdated"), "");
		loadEntity(entityP);
		updateCredentialStatus();
		return true;
	}

	private void onAdditionalAuthnForUpdateCredential(AdditionalAuthnHandler.AuthnResult result)
	{
		if (result == AdditionalAuthnHandler.AuthnResult.SUCCESS)
		{
			updateCredential();
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

	
	private void changeCredentialStatusWithConfirm(LocalCredentialState desiredState)
	{
		ConfirmDialog confirmDialog = new ConfirmDialog();
		confirmDialog.setCancelable(true);
		confirmDialog.setText(msg.getMessage("CredentialChangeDialog.confirmStateChangeTo." + desiredState, toEdit.getName()));
		confirmDialog.addConfirmListener(e ->
		{
			changeCredentialStatus(desiredState);
			refresh.run();
		});
		confirmDialog.open();
	}

	private void changeCredentialStatus(LocalCredentialState desiredState)
	{
		EntityParam entityP = new EntityParam(entity.getId());
		try
		{
			ecredMan.setEntityCredentialStatus(entityP, toEdit.getName(), desiredState);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("CredentialChangeDialog.credentialUpdateError"), e.getMessage());
			return;
		}
		loadEntity(entityP);
		updateCredentialStatus();
	}
	
	private void loadEntity(EntityParam entityP)
	{
		try
		{
			entity = entityMan.getEntity(entityP);
		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("CredentialChangeDialog.entityRefreshError"),
					e.getMessage());
		}
	}

}
