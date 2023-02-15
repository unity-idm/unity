/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.function.Function;

import org.springframework.beans.factory.ObjectFactory;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.layout.FormLayoutElement;
import pl.edu.icm.unity.webui.AsyncErrorHandler;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.forms.RegCodeException.ErrorCause;
import pl.edu.icm.unity.webui.forms.reg.RequestEditorCreatorV8.InvitationCodeConsumer;
import pl.edu.icm.unity.webui.forms.reg.RequestEditorCreatorV8.RequestEditorCreatedCallback;

/**
 * Common boiler plate of registration form dialog provider which 
 * handles the case where registration form contains the local sign up button
 * element {@link FormLayoutElement#LOCAL_SIGNUP}
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public abstract class AbstractRegistrationFormDialogProvider implements RegistrationFormDialogProvider
{
	protected final MessageSource msg;
	protected final ObjectFactory<RequestEditorCreatorV8> requestEditorCreatorFactory;
	
	public AbstractRegistrationFormDialogProvider(MessageSource msg,
			ObjectFactory<RequestEditorCreatorV8> requestEditorCreatorFactory)
	{
		this.msg = msg;
		this.requestEditorCreatorFactory = requestEditorCreatorFactory;
	}

	protected abstract AbstractDialog createDialog(RegistrationForm form, RegistrationRequestEditor editor, 
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
		RequestEditorCreatorV8 editorCreator = requestEditorCreatorFactory.getObject();
		editorCreator.init(form, remoteContext, null);
		EditorCreatedCallbackImpl callback = new EditorCreatedCallbackImpl(
				errorHandler, (editor) -> createDialog(form, editor, mode));
		InvitationCodeConsumer localSignupHandler = ivitationCode -> 
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
		RequestEditorCreatorV8 editorCreator = requestEditorCreatorFactory.getObject();
		editorCreator.init(form, false, remoteContext, registrationCode, null);
		EditorCreatedCallbackImpl callback = new EditorCreatedCallbackImpl(
				errorHandler, (editor) -> createDialog(form, editor, mode));
		editorCreator.createSecondStage(callback, true);
		return callback;
	}

	
	class EditorCreatedCallbackImpl implements RequestEditorCreatedCallback
	{
		private final AsyncErrorHandler errorHandler;
		private final Function<RegistrationRequestEditor, AbstractDialog> dialogCreator;
		private AbstractDialog dialog;

		public EditorCreatedCallbackImpl(AsyncErrorHandler errorHandler, 
				Function<RegistrationRequestEditor, AbstractDialog> dialogCreator)
		{
			this.errorHandler = errorHandler;
			this.dialogCreator = dialogCreator;
		}
		
		@Override
		public void onCreationError(Exception e, ErrorCause cause)
		{
			errorHandler.onError(e);
		}
		
		@Override
		public void onCreated(RegistrationRequestEditor editor)
		{
			dialog = dialogCreator.apply(editor);
			dialog.show();
		}

		@Override
		public void onCancel()
		{
			//nop
		}

		public AbstractDialog getDialog()
		{
			return dialog;
		}

	}

}
