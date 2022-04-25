/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;

import edu.vt.middleware.password.AlphabeticalSequenceRule;
import edu.vt.middleware.password.CharacterCharacteristicsRule;
import edu.vt.middleware.password.CharacterRule;
import edu.vt.middleware.password.DigitCharacterRule;
import edu.vt.middleware.password.LengthRule;
import edu.vt.middleware.password.LowercaseCharacterRule;
import edu.vt.middleware.password.NonAlphanumericCharacterRule;
import edu.vt.middleware.password.NumericalSequenceRule;
import edu.vt.middleware.password.Password;
import edu.vt.middleware.password.PasswordData;
import edu.vt.middleware.password.PasswordValidator;
import edu.vt.middleware.password.QwertySequenceRule;
import edu.vt.middleware.password.RepeatCharacterRegexRule;
import edu.vt.middleware.password.Rule;
import edu.vt.middleware.password.RuleResult;
import edu.vt.middleware.password.UppercaseCharacterRule;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.DenyReason;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalVerificator;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.CredentialRecentlyUsedException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Ordinary password credential verificator. Supports all settings from {@link PasswordCredential}.  
 * <p>
 * Additionally configuration of the credential may allow for password reset feature. Then are stored
 * email verification settings, security question settings and confirmation code length.
 * 
 * @see PasswordCredential
 * @author K. Benedyczak
 */
@PrototypeComponent
public class PasswordVerificator extends AbstractLocalVerificator implements PasswordExchange
{ 	
	private static final ResolvableError GENERIC_ERROR = new ResolvableError("WebPasswordRetrieval.wrongPassword");
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, PasswordVerificator.class);
	public static final String NAME = "password";
	public static final String DESC = "Verifies passwords";
	public static final String[] IDENTITY_TYPES = {UsernameIdentity.ID, EmailIdentity.ID};

	private NotificationProducer notificationProducer;
	private CredentialHelper credentialHelper;
	private PasswordEngine passwordEngine;
	
	private PasswordCredential credential = new PasswordCredential();

	@Autowired
	public PasswordVerificator(NotificationProducer notificationProducer, CredentialHelper credentialHelper,
			Optional<PasswordEncodingPoolProvider> threadPoolProvider)
	{
		super(NAME, DESC, PasswordExchange.ID, true);
		this.notificationProducer = notificationProducer;
		this.credentialHelper = credentialHelper;
		this.passwordEngine = new PasswordEngine(threadPoolProvider
				.map(pp->pp.pool)
				.orElse(ForkJoinPool.commonPool()));
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		return JsonUtil.serialize(credential.getSerializedConfiguration());
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
		credential.setSerializedConfiguration(JsonUtil.parse(json));
	}

	/**
	 * The rawCredential must be in JSON format, see {@link PasswordToken} for details.
	 */
	@Override
	public String prepareCredential(String rawCredential,
			String currentCredential, boolean verify)
			throws IllegalCredentialException, InternalException
	{
		Deque<PasswordInfo> currentPasswords = PasswordCredentialDBState.fromJson(currentCredential).
				getPasswords();
		
		PasswordToken pToken = PasswordToken.loadFromJson(rawCredential);
		
		if (verify)
			verifyNewPassword(pToken.getPassword(), 
					currentPasswords, credential.getHistorySize());
		
		return prepareCredentialForStorage(currentPasswords, pToken);
	}

	private String prepareCredentialForStorage(Deque<PasswordInfo> currentPasswords, PasswordToken pToken)
			throws IllegalCredentialException, InternalException
	{
		if (credential.getPasswordResetSettings().isEnabled() && 
				credential.getPasswordResetSettings().isRequireSecurityQuestion())
		{
			if (pToken.getAnswer() == null || pToken.getQuestion() == -1)
				throw new IllegalCredentialException("The credential must select a security question " +
						"and provide an answer for it");
			if (pToken.getQuestion() < 0 || pToken.getQuestion() >= 
					credential.getPasswordResetSettings().getQuestions().size())
				throw new IllegalCredentialException("The chosen answer for security question is invalid");
		}
		
		PasswordInfo currentPassword = passwordEngine.prepareForStore(credential, pToken.getPassword());
		if (credential.getHistorySize() <= currentPasswords.size() && !currentPasswords.isEmpty())
			currentPasswords.removeLast();
		currentPasswords.addFirst(currentPassword);
		
		PasswordInfo questionAnswer = pToken.getAnswer() != null ? 
				passwordEngine.prepareForStore(credential, pToken.getAnswer()) :
				null;

		return PasswordCredentialDBState.toJson(credential, currentPasswords, 
				pToken.getQuestion(), questionAnswer);
	}
	
	
	@Override
	public String invalidate(String currentCredential)
	{
		ObjectNode root;
		try
		{
			root = (ObjectNode) Constants.MAPPER.readTree(currentCredential);
			root.put("outdated", true);
			return Constants.MAPPER.writeValueAsString(root);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize password credential from JSON", e);
		}
	}

	@Override
	public CredentialPublicInformation checkCredentialState(String currentCredential) throws InternalException
	{
		PasswordCredentialDBState parsedCred = PasswordCredentialDBState.fromJson(currentCredential);
		Deque<PasswordInfo> currentPasswords = parsedCred.getPasswords();
		if (currentPasswords.isEmpty())
			return new CredentialPublicInformation(LocalCredentialState.notSet, "");
		
		PasswordInfo currentPassword = currentPasswords.getFirst();
		PasswordExtraInfo pei = new PasswordExtraInfo(currentPassword.getTime(), 
				parsedCred.getSecurityQuestion());
		String extraInfo = pei.toJson();

		PasswordStatus upToDateStatus = checkIfCredentialIsOutdated(parsedCred);
		if (upToDateStatus.outdated)
			return new CredentialPublicInformation(LocalCredentialState.outdated, 
					upToDateStatus.reason, extraInfo);
		if (storedPasswordRequiresRehash(parsedCred)) 
			return new CredentialPublicInformation(LocalCredentialState.outdated, "rehash required", extraInfo);
		return new CredentialPublicInformation(LocalCredentialState.correct, extraInfo);
	}

	@Override
	public Optional<String> updateCredentialAfterConfigurationChange(String currentCredential)
	{
		PasswordCredentialDBState parsedCred = PasswordCredentialDBState.fromJson(currentCredential);
		Deque<PasswordInfo> passwords = parsedCred.getPasswords();
		
		boolean changed = removeHistoricalPasswordsStoredWithOutdatedMechanism(passwords);
		changed |= removeExcessHistoricalPasswords(passwords, credential.getHistorySize());
		
		if (changed)
		{
			String updatedCredential = JsonUtil.toJsonString(parsedCred);
			return Optional.of(updatedCredential);
		} else
		{
			return Optional.empty();
		}
		
	}

	private boolean removeHistoricalPasswordsStoredWithOutdatedMechanism(Deque<PasswordInfo> passwords)
	{
		boolean changed = false;

		Iterator<PasswordInfo> iterator = passwords.iterator();
		iterator.next();
		while (iterator.hasNext())
		{
			PasswordInfo passwordInfo = iterator.next();
			if (!passwordEngine.checkParamsUpToDate(credential, passwordInfo))
			{
				iterator.remove();
				changed = true;
			}
		}
		return changed;
	}
	
	private boolean removeExcessHistoricalPasswords(Deque<PasswordInfo> passwords, int historySize)
	{
		boolean changed = false;
		while ((historySize == 0 && passwords.size() > 1) || (historySize > 0 && passwords.size() > historySize))
		{
			passwords.removeLast();
			changed = true;
		}
		return changed;
	}
	
	/**
	 * Checks if the provided password is valid. If it is then it is checked if it is still 
	 * fulfilling all actual rules of the credential's configuration. If not then it is returned that 
	 * credential state is outdated. 
	 */
	@Override
	public AuthenticationResult checkPassword(String username, String password, 
			String formForUnknown, boolean enableAssociation, 
			AuthenticationTriggeringContext triggeringContext)
	{
		return checkPasswordInternal(username, password);
	}

	private AuthenticationResult checkPasswordInternal(String username, String password)
	{
		EntityWithCredential resolved;
		try
		{
			resolved = identityResolver.resolveIdentity(username, 
					IDENTITY_TYPES, credentialName);
		} catch (Exception e)
		{
			log.info("The user for password authN can not be found: " + username, e);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}
		
		try
		{
			String dbCredential = resolved.getCredentialValue();
			PasswordCredentialDBState credState = PasswordCredentialDBState.fromJson(dbCredential);
			Deque<PasswordInfo> credentials = credState.getPasswords();
			if (credentials.isEmpty())
			{
				log.info("The user has no password set: {}", username);
				return LocalAuthenticationResult.failed(GENERIC_ERROR, DenyReason.notDefinedCredential);
			}
			PasswordInfo current = credentials.getFirst();
			if (!passwordEngine.verify(current, password))
			{
				log.info("Password provided by {} is invalid", username);
				return LocalAuthenticationResult.failed(GENERIC_ERROR);
			}
			boolean isOutdated = isCurrentPasswordOutdated(password, credState, resolved);
			AuthenticatedEntity ae = new AuthenticatedEntity(resolved.getEntityId(), username, 
					isOutdated ? resolved.getCredentialName() : null);
			return LocalAuthenticationResult.successful(ae);
		} catch (Exception e)
		{
			log.warn("Error during password verification for " + username, e);
			return LocalAuthenticationResult.failed(GENERIC_ERROR);
		}
	}

	@Override
	public CredentialReset getCredentialResetBackend()
	{
		return new PasswordCredentialResetImpl(notificationProducer, identityResolver, 
				this, credentialHelper,
				credentialName, credential.getSerializedConfiguration(), 
				credential.getPasswordResetSettings(),
				passwordEngine);
	}

	/**
	 * As {@link #isCurrentCredentialOutdated(PasswordCredentialDBState)} but also
	 * checks if the provided password is fulfilling the actual rules of the credential
	 * (what can not be checked with the hashed in-db version of the credential).
	 * <p>
	 * Additionally, if it is detected that the credential is outdated by checking the password, 
	 * this newly received information is stored in DB: credential is updated to be manually outdated.
	 */
	private boolean isCurrentPasswordOutdated(String password, PasswordCredentialDBState credState, 
			EntityWithCredential resolved) throws AuthenticationException
	{
		if (isCurrentCredentialOutdated(credState))
			return true;

		try
		{
			verifyPasswordStrength(password);
		} catch (IllegalCredentialException e)
		{
			log.info("User with id {} logged in with password not matching current credential requirements, "
					+ "invalidating the password", resolved.getEntityId());
			String invalidated = invalidate(resolved.getCredentialValue());
			try
			{
				credentialHelper.updateCredential(resolved.getEntityId(), resolved.getCredentialName(), 
							invalidated);
			} catch (EngineException ee)
			{
				throw new AuthenticationException("Problem invalidating outdated credential", ee);
			}
			return true;
		}
		
		if (storedPasswordRequiresRehash(credState))
		{
			log.debug("Password hash of user {} is outdated: hashing parameters were changed", resolved.getEntityId());
			rehashPassword(password, credState, resolved);
		}
		return false;
	}

	
	private void rehashPassword(String password, PasswordCredentialDBState credState, 
			EntityWithCredential resolved) throws AuthenticationException
	{
		try
		{
			log.info("Re-hashing password of entity {} to match updated security requirements", resolved.getEntityId());
			PasswordToken passwordToken = new PasswordToken(password);
			String rehashed = prepareCredentialForStorage(credState.getPasswords(), passwordToken);
			credentialHelper.updateCredential(resolved.getEntityId(), resolved.getCredentialName(), 
					rehashed);
		} catch (Exception e)
		{
			throw new AuthenticationException("Problem rehasing password", e);
		}
	}
	
	/**
	 * Checks if the provided credential state is fulfilling the rules of the credential,
	 * if it was expired manually or if password did expire on its own.  
	 *  
	 * @param password
	 * @return true if credential is invalid
	 */
	private boolean isCurrentCredentialOutdated(PasswordCredentialDBState credState)
	{
		return checkIfCredentialIsOutdated(credState).outdated;
	}

	private PasswordStatus checkIfCredentialIsOutdated(PasswordCredentialDBState credState)
	{
		if (credState.isOutdated())
		{
			return new PasswordStatus(true, "set as");
		}
		if (credState.getSecurityQuestion() == null && 
				credential.getPasswordResetSettings().isEnabled() && 
				credential.getPasswordResetSettings().isRequireSecurityQuestion())
		{
			return new PasswordStatus(true, "no question");
		}
		PasswordInfo current = credState.getPasswords().getFirst();
		Date validityEnd = new Date(current.getTime().getTime() + credential.getMaxAge());
		if (new Date().after(validityEnd))
		{
			return new PasswordStatus(true, "expired");
		}
		if (credential.getPasswordResetSettings().isEnabled() && 
				credential.getPasswordResetSettings().isRequireSecurityQuestion() &&
				!passwordEngine.checkParamsUpToDate(credential, credState.getAnswer()))
		{
			return new PasswordStatus(true, "question outdated");
		}
		return new PasswordStatus(false, null);
	}

	/**
	 * @return true if the password as stored in DB was hashed using outdated credential settings 
	 * (e.g. other number of iterations, some of legacy key derivation functions).
	 */
	private boolean storedPasswordRequiresRehash(PasswordCredentialDBState credState)
	{
		PasswordInfo password = credState.getPasswords().getFirst();
		return !passwordEngine.checkParamsUpToDate(credential, password);
	}
	
	private void verifyNewPassword(String password, Deque<PasswordInfo> currentCredentials, int historyLookback) 
			throws IllegalCredentialException
	{
		verifyPasswordStrength(password);
		
		verifyPasswordNotReused(password, currentCredentials, historyLookback);
	}

	private void verifyPasswordStrength(String password) throws IllegalCredentialException
	{
		Zxcvbn zxcvbn = new Zxcvbn();
		Strength strength = zxcvbn.measure(password);
		if (strength.getGuessesLog10() < credential.getMinScore())
			throw new IllegalCredentialException("Password has too low score " 
					+ strength.getGuessesLog10() + "/" + credential.getMinScore());
		
		PasswordValidator validator = getPasswordValidator();
		RuleResult result = validator.validate(new PasswordData(new Password(password)));
		if (!result.isValid())
			throw new IllegalCredentialException("Password is too weak");
	}

	private void verifyPasswordNotReused(String password, Deque<PasswordInfo> currentCredentials, int historyLookback) 
			throws IllegalCredentialException
	{
		Iterator<PasswordInfo> iterator = currentCredentials.iterator();
		for (int i=0; i<historyLookback && iterator.hasNext(); i++)
		{
			PasswordInfo pi = iterator.next();
			if (passwordEngine.verify(pi, password))
				throw new CredentialRecentlyUsedException("The same password was recently used");
		}
	}

	
	private PasswordValidator getPasswordValidator()
	{
		List<Rule> ruleList = new ArrayList<Rule>();
		ruleList.add(new LengthRule(credential.getMinLength(), 512));
		CharacterCharacteristicsRule charRule = new CharacterCharacteristicsRule();
		charRule.setRules(getCharacteristicsRules());
		charRule.setNumberOfCharacteristics(credential.getMinClassesNum());
		ruleList.add(charRule);
		if (credential.isDenySequences())
			ruleList.addAll(getSequencesRules());
		return new PasswordValidator(ruleList);
	}


	@Override
	public boolean isCredentialDefinitionChagneOutdatingCredentials(String newCredentialDefinition)
	{
		PasswordCredential updated = new PasswordCredential();
		updated.setSerializedConfiguration(JsonUtil.parse(newCredentialDefinition));
		return updated.hasStrongerRequirementsThen(credential);
	}
	
	public static List<CharacterRule> getCharacteristicsRules()
	{
		return Lists.newArrayList(
				new DigitCharacterRule(1),
				new NonAlphanumericCharacterRule(1),
				new UppercaseCharacterRule(1),
				new LowercaseCharacterRule(1));
	}

	public static List<Rule> getSequencesRules()
	{
		return Lists.newArrayList(
				new AlphabeticalSequenceRule(),
				new NumericalSequenceRule(3, true),
				new QwertySequenceRule(),
				new RepeatCharacterRegexRule(4));
	}
	
	@Override
	public boolean isCredentialSet(EntityParam entity) throws EngineException
	{
		return credentialHelper.isCredentialSet(entity, credentialName);
	}
	
	private static class PasswordStatus
	{
		boolean outdated;
		String reason;

		PasswordStatus(boolean outdated, String reason)
		{
			this.outdated = outdated;
			this.reason = reason;
		}
	}
	
	@Component
	public static class Factory extends AbstractLocalCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<PasswordVerificator> factory)
		{
			super(NAME, DESC, true, factory);
		}
	}
}





