/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.function.Function;

import org.springframework.beans.factory.ObjectFactory;

import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormLayoutType;
import pl.edu.icm.unity.webui.AsyncErrorHandler;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.forms.reg.RequestEditorCreator.RequestEditorCreatedCallback;

/**
 * Common boiler plate of registration form dialog provider which basically
 * handles the case where registration form contains the local sign up button
 * element {@link FormLayoutType#LOCAL_SIGNUP}
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public abstract class AbstraceRegistrationFormDialogProvider implements RegistrationFormDialogProvider
{
	protected final UnityMessageSource msg;
	protected final ObjectFactory<RequestEditorCreator> requestEditorCreatorFactory;
	
	public AbstraceRegistrationFormDialogProvider(UnityMessageSource msg,
			ObjectFactory<RequestEditorCreator> requestEditorCreatorFactory)
	{
		this.msg = msg;
		this.requestEditorCreatorFactory = requestEditorCreatorFactory;
	}

	protected abstract AbstractDialog createDialog(RegistrationForm form, RegistrationRequestEditor editor, 
			TriggeringMode mode);
	
	@Override
	public void showRegistrationDialog(RegistrationForm form, RemotelyAuthenticatedContext remoteContext,
			TriggeringMode mode, AsyncErrorHandler errorHandler)
	{
		RequestEditorCreator requestEditorCreator = requestEditorCreatorFactory.getObject();
		OnLocalSignupClickHandler localSignupHandler = new OnLocalSignupClickHandler();
		EditorCreatedCallbackImpl editorCallback = showRegistrationDialog(form, remoteContext, 
				mode, errorHandler, localSignupHandler, form.getEffectivePrimaryFormLayout(msg),
				requestEditorCreator);
		localSignupHandler.onClick(() -> 
		{
			editorCallback.getDialog().close();
			showRegistrationDialog(form, remoteContext, mode, errorHandler, localSignupHandler, 
					form.getEffectiveSecondaryFormLayout(msg), requestEditorCreator);
		});
	}
	
	private EditorCreatedCallbackImpl showRegistrationDialog(final RegistrationForm form, 
			RemotelyAuthenticatedContext remoteContext, TriggeringMode mode,
			AsyncErrorHandler errorHandler, Runnable onLocalSignupHandler, FormLayout layout,
			RequestEditorCreator editorCreator)
	{
		editorCreator.init(form, onLocalSignupHandler, layout, remoteContext);
		EditorCreatedCallbackImpl callback = new EditorCreatedCallbackImpl(
				errorHandler, (editor) -> createDialog(form, editor, mode));
		editorCreator.invoke(callback);
		return callback;
	}
	
	class OnLocalSignupClickHandler implements Runnable
	{
		private Runnable onClick;

		@Override
		public void run()
		{
			onClick.run();
		}

		public void onClick(Runnable onClick)
		{
			this.onClick = onClick;
		}
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
		public void onCreationError(Exception e)
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
