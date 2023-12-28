/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities.credentials;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.auth.additional.AdditionalAuthnHandler;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorContext;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.authn.CredentialType;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationMisconfiguredException;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationRequiredException;

public class SingleCredentialPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SingleCredentialPanel.class);

	private final EntityCredentialManagement entityCredMan;
	private final EntityManagement entityMan;
	private final CredentialEditorRegistry credEditorReg;
	private final CredentialManagement credMan;
	private final MessageSource msg;
	private final CredentialDefinition toEdit;
	private final AdditionalAuthnHandler additionalAuthnHandler;
	private final NotificationPresenter notificationPresenter;

	private boolean changed = false;
	private Entity entity;
	private final long entityId;
	private final boolean enableAdminOptions;
	private Html credentialName;
	private Span credentialStatus;
	private VerticalLayout credentialExtraInfo;
	private LinkButton edit;
	private LinkButton clear;
	private LinkButton invalidate;
	private CredentialEditor credEditor;
	private SingleCredentialEditComponent credEditorPanel;
	private LocalCredentialState credentialState;
	private FormLayout.FormItem credentialExtraFormItem;
	private FormLayout.FormItem credEditorFormItem;
	private FormLayout.FormItem credentialStatusFormItem;
	private Component actionsBar;


	public SingleCredentialPanel(AdditionalAuthnHandler additionalAuthnHandler, MessageSource msg, long entityId,
			EntityCredentialManagement entityCredMan, CredentialManagement credMan,
			EntityManagement entityMan, CredentialEditorRegistry credEditorReg,
			CredentialDefinition toEdit, boolean enableAdminActions, NotificationPresenter notificationPresenter)
	{
		this.additionalAuthnHandler = additionalAuthnHandler;
		this.msg = msg;
		this.entityCredMan = entityCredMan;
		this.credMan = credMan;
		this.entityId = entityId;
		this.entityMan = entityMan;
		this.credEditorReg = credEditorReg;
		this.enableAdminOptions = enableAdminActions;
		this.toEdit = toEdit;
		this.notificationPresenter = notificationPresenter;
		loadEntity(new EntityParam(entityId));
		init();
	}

	private void init()
	{
		credentialName = new Html("<div></div>");
		credentialStatus = new Span();

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

		FormLayout fl = new FormLayout();
		fl.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		fl.addFormItem(credentialName, msg.getMessage("CredentialChangeDialog.credentialName"));
		credentialStatusFormItem = fl.addFormItem(credentialStatus,
				msg.getMessage("CredentialChangeDialog.credentialStateInfo"));
		credentialExtraFormItem = fl.addFormItem(credentialExtraInfo, "");
		credEditorFormItem = fl.addFormItem(credEditorPanel, "");
		actionsBar = createActionsBar();
		fl.addFormItem(actionsBar, "");
		add(fl);

		credEditorFormItem.setVisible(false);
		updateCredentialStatus();
	}

	private Component createActionsBar()
	{
		clear = new LinkButton(msg.getMessage("CredentialChangeDialog.clear"), e -> clearCredential());

		invalidate = new LinkButton(msg.getMessage("CredentialChangeDialog.invalidate"),
				ne -> changeCredentialStatusWithConfirm(LocalCredentialState.outdated));

		edit = new LinkButton(msg.getMessage("CredentialChangeDialog.setup"), e -> switchEditorVisibility(true));

		HorizontalLayout buttonsBar = new HorizontalLayout();
		buttonsBar.setSpacing(true);
		buttonsBar.setMargin(false);
		buttonsBar.add(edit);
		if (enableAdminOptions)
		{
			buttonsBar.add(clear);
			if (isInvalidationSupported(toEdit.getTypeId()))
				buttonsBar.add(invalidate);
		}
		return buttonsBar;
	}

	boolean isChanged()
	{
		return changed;
	}

	boolean isEmptyEditor()
	{
		return credEditorPanel.isEmpty();
	}

	private Component getStatusIcon()
	{
		if (credentialState.equals(LocalCredentialState.correct))
			return VaadinIcon.CHECK_CIRCLE_O.create();
		else if (credentialState.equals(LocalCredentialState.notSet))
			return VaadinIcon.BAN.create();
		else
			return VaadinIcon.EXCLAMATION_CIRCLE_O.create();
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
		String desc = toEdit.getDescription().getValue(msg);
		if (desc != null && !desc.isEmpty())
		{
			credentialName.setHtmlContent("<div>" + desc + "</div>");
		} else
		{
			credentialName.setHtmlContent("<div>" + toEdit.getName() + "</div>");
		}

		Map<String, CredentialPublicInformation> s = entity.getCredentialInfo()
				.getCredentialsState();
		CredentialPublicInformation credPublicInfo = s.get(toEdit.getName());
		credentialState = credPublicInfo.getState();
		credentialStatus.removeAll();
		credentialStatus.add(getStatusIcon());
		credentialStatus.add(" "
				+ msg.getMessage("CredentialStatus." + credPublicInfo.getState().toString()));

		Optional<Component> viewer = credEditor.getViewer(credPublicInfo.getExtraInformation());
		credentialExtraInfo.removeAll();
		if (viewer.isPresent())
		{
			credentialExtraInfo.add(viewer.get());
			credentialExtraFormItem.setVisible(true);
		} else
		{
			credentialExtraFormItem.setVisible(false);
		}

		updateEditCaption(credPublicInfo.getState());
		if (enableAdminOptions)
			updateAdminButtonsState(credPublicInfo.getState());
	}

	private void updateEditCaption(LocalCredentialState state)
	{
		String captionKey = state == LocalCredentialState.notSet ? "CredentialChangeDialog.setup"
				: "CredentialChangeDialog.change";
		edit.setLabel(msg.getMessage(captionKey));
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
			credentialExtraFormItem.setVisible(!editorVisible);
		credentialStatusFormItem.setVisible(!editorVisible);
		actionsBar.setVisible(!editorVisible);
		credEditorFormItem.setVisible(editorVisible);
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
			hideEditor();
	}

	private boolean updateCredential()
	{
		if (credEditor.isCredentialCleared())
		{
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
			entityCredMan.setEntityCredential(entityP, toEdit.getName(), secrets);
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
		changed = true;
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
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("CredentialChangeDialog.confirmStateChangeTo." + desiredState,
						credentialName.getInnerHtml()),
				msg.getMessage("ok"),
				e -> changeCredentialStatus(desiredState),
				msg.getMessage("cancel"),
				e ->
				{
				}
		).open();
	}

	private void changeCredentialStatus(LocalCredentialState desiredState)
	{
		EntityParam entityP = new EntityParam(entity.getId());
		try
		{
			entityCredMan.setEntityCredentialStatus(entityP, toEdit.getName(), desiredState);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("CredentialChangeDialog.credentialUpdateError"),
					e.getMessage());
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
