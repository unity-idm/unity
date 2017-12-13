/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import java.util.Optional;

import pl.edu.icm.unity.engine.api.authn.AbstractVerificator;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.engine.api.utils.LogRecorder;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Base class that is nearly mandatory for all remote verificators. The remote verificator should extend it 
 * by implementing a {@link CredentialExchange} of choice. The implementation should obtain the 
 * {@link RemotelyAuthenticatedInput} (the actual coding should be done here) and before returning it should
 * be processed by {@link #getResult(RemotelyAuthenticatedInput)} to obtain the final authentication result.
 * <p>
 * Additionally (to enable compatibility with sandbox authN facility) the extension must call 
 * {@link #startAuthnResponseProcessing(String...)} at the beginning of authN response verification and
 * {@link #finishAuthnResponseProcessing(RemoteAuthnState, AuthenticationException, RemotelyAuthenticatedInput)} in case of any
 * exception produced during verification.
 * 
 * @author K. Benedyczak
 */
public abstract class AbstractRemoteVerificator extends AbstractVerificator
{
	private RemoteAuthnResultProcessor processor;
	
	public AbstractRemoteVerificator(String name, String description, String exchangeId, 
			RemoteAuthnResultProcessor processor)
	{
		super(name, description, exchangeId);
		this.processor = processor;
	}

	/**
	 * This method is calling {@link #processRemoteInput(RemotelyAuthenticatedInput)} and then
	 * {@link #assembleAuthenticationResult(RemotelyAuthenticatedContext)}.
	 * Usually it is the only one that is used in subclasses, when {@link RemotelyAuthenticatedInput} 
	 * is obtained in an implementation specific way.
	 * 
	 * @param input
	 * @return
	 * @throws EngineException 
	 */
	protected AuthenticationResult getResult(RemotelyAuthenticatedInput input, String profile,
			RemoteAuthnState state) throws AuthenticationException
	{
		RemoteAuthnStateImpl stateCasted = (RemoteAuthnStateImpl)state;
		stateCasted.remoteInput = input;
		
		AuthenticationResult result = processor.getResult(input, profile, 
				stateCasted.isInSandboxMode(), Optional.empty());
		finishAuthnResponseProcessing(state, result.getRemoteAuthnContext());
		return result;
	}
	
	/**
	 * Should be called at the beginning of authN response verification
	 * @param loggingFacilities logging facilities relevant for the verification process
	 * @return
	 */
	protected RemoteAuthnState startAuthnResponseProcessing(SandboxAuthnResultCallback callback,
			String... loggingFacilities)
	{
		RemoteAuthnStateImpl ret = new RemoteAuthnStateImpl(loggingFacilities, callback);
		return ret;
	}
	
	/**
	 * Should be called at the end of properly finished verification. Handled internally.
	 * @param state
	 * @param context
	 */
	private void finishAuthnResponseProcessing(RemoteAuthnState state, RemotelyAuthenticatedContext context)
	{
		RemoteAuthnStateImpl stateCasted = (RemoteAuthnStateImpl)state;
		if (stateCasted.isInSandboxMode())
		{
			LogRecorder recorder = stateCasted.logRecorder;
			recorder.stopLogRecording();
			stateCasted.sandboxCallback.sandboxedAuthenticationDone(
					new RemoteSandboxAuthnContext(context, 
							recorder.getCapturedLogs().toString()));
		}
	}
	
	/**
	 * Should be called at the end of failed verification. 
	 * @param state
	 * @param error
	 * @param remoteInput can be null if failure was upon input assembly.
	 */
	protected void finishAuthnResponseProcessing(RemoteAuthnState state, Exception error)
	{
		RemoteAuthnStateImpl stateCasted = (RemoteAuthnStateImpl)state;
		
		if (stateCasted.isInSandboxMode())
		{
			LogRecorder recorder = stateCasted.logRecorder;
			recorder.stopLogRecording();
			stateCasted.sandboxCallback.sandboxedAuthenticationDone(
					new RemoteSandboxAuthnContext(error, recorder.getCapturedLogs().toString(), 
							stateCasted.remoteInput));
		}
	}

	private class RemoteAuthnStateImpl implements RemoteAuthnState
	{
		private LogRecorder logRecorder;
		private RemotelyAuthenticatedInput remoteInput;
		private SandboxAuthnResultCallback sandboxCallback;
		
		public RemoteAuthnStateImpl(String[] facilities, SandboxAuthnResultCallback sandboxCallback)
		{
			this.sandboxCallback = sandboxCallback;
			logRecorder = new LogRecorder(facilities);
			if (isInSandboxMode())
				logRecorder.startLogRecording();
		}
		
		boolean isInSandboxMode()
		{
			return sandboxCallback != null;
		}
	}
	
	/**
	 * Marker interface only. Implementation is an object holding a state of remote authentication. Currently used
	 * merely in sandbox mode.
	 * @author K. Benedyczak
	 */
	public interface RemoteAuthnState
	{
	}
}
