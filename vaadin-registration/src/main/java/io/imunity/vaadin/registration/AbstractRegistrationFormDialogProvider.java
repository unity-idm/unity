/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.registration;

import com.vaadin.flow.component.dialog.Dialog;
import io.imunity.vaadin.endpoint.common.api.RegistrationFormDialogProvider;
import io.imunity.vaadin.endpoint.common.forms.RegCodeException;
import org.springframework.beans.factory.ObjectFactory;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.layout.FormLayoutElement;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;

import java.util.Map;
import java.util.function.Function;

/**
 * Common boiler plate of registration form dialog provider which 
 * handles the case where registration form contains the local sign up button
 * element {@link FormLayoutElement#LOCAL_SIGNUP}
 */
public abstract class AbstractRegistrationFormDialogProvider implements RegistrationFormDialogProvider
{
	protected final MessageSource msg;
	protected final ObjectFactory<RequestEditorCreator> requestEditorCreatorFactory;
	
	public AbstractRegistrationFormDialogProvider(MessageSource msg,
	                                              ObjectFactory<RequestEditorCreator> requestEditorCreatorFactory)
	{
		this.msg = msg;
		this.requestEditorCreatorFactory = requestEditorCreatorFactory;
	}

	protected abstract Dialog createDialog(RegistrationForm form, RegistrationRequestEditor editor,
	                                       TriggeringMode mode);
	
	@Override
	public void showRegistrationDialog(RegistrationForm form, RemotelyAuthenticatedPrincipal remoteContext,
			TriggeringMode mode, AsyncErrorHandler errorHandler)
	{
		if (isRemoteLoginWhenUnknownUser(mode))
		{
			showSecondStageDialog(form, remoteContext, mode, null, errorHandler);
		} else
		{
			showFirstStageDialog(form, remoteContext, mode, errorHandler);
		}
	}

	protected boolean isRemoteLoginWhenUnknownUser(TriggeringMode mode)
	{
		return mode == TriggeringMode.afterRemoteLoginWhenUnknownUser;
	}

	private EditorCreatedCallbackImpl showFirstStageDialog(final RegistrationForm form, 
			RemotelyAuthenticatedPrincipal remoteContext, TriggeringMode mode,
			AsyncErrorHandler errorHandler)
	{
		RequestEditorCreator editorCreator = requestEditorCreatorFactory.getObject();
		editorCreator.init(form, remoteContext, null);
		EditorCreatedCallbackImpl callback = new EditorCreatedCallbackImpl(
				errorHandler, (editor) -> createDialog(form, editor, mode));
		RequestEditorCreator.InvitationCodeConsumer localSignupHandler = ivitationCode ->
		{
			callback.getDialog().close();
			showSecondStageDialog(form, remoteContext, mode, ivitationCode, errorHandler);
		};
		
		editorCreator.createFirstStage(callback, localSignupHandler);
		return callback;
	}
	
	private EditorCreatedCallbackImpl showSecondStageDialog(final RegistrationForm form, 
			RemotelyAuthenticatedPrincipal remoteContext, TriggeringMode mode,
			String registrationCode, 
			AsyncErrorHandler errorHandler)
	{
		RequestEditorCreator editorCreator = requestEditorCreatorFactory.getObject();
		editorCreator.init(form, false, remoteContext, registrationCode, null, Map.of());
		EditorCreatedCallbackImpl callback = new EditorCreatedCallbackImpl(
				errorHandler, (editor) -> createDialog(form, editor, mode));
		editorCreator.createSecondStage(callback, true);
		return callback;
	}

	
	static class EditorCreatedCallbackImpl implements RequestEditorCreator.RequestEditorCreatedCallback
	{
		private final AsyncErrorHandler errorHandler;
		private final Function<RegistrationRequestEditor, Dialog> dialogCreator;
		private Dialog dialog;

		public EditorCreatedCallbackImpl(AsyncErrorHandler errorHandler, 
				Function<RegistrationRequestEditor, Dialog> dialogCreator)
		{
			this.errorHandler = errorHandler;
			this.dialogCreator = dialogCreator;
		}
		
		@Override
		public void onCreated(RegistrationRequestEditor editor)
		{
			dialog = dialogCreator.apply(editor);
			dialog.open();
		}

		@Override
		public void onCreationError(Exception e, RegCodeException.ErrorCause cause)
		{
			errorHandler.onError(e);
		}

		@Override
		public void onCancel()
		{
			//nop
		}

		public Dialog getDialog()
		{
			return dialog;
		}

	}

}
