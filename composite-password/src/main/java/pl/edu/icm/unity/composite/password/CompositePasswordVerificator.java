/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.composite.password;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.AbstractVerificator;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.ldap.client.LdapPasswordVerificator;
import pl.edu.icm.unity.pam.PAMVerificator;
import pl.edu.icm.unity.stdext.credential.NoCredentialResetImpl;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.PasswordEncodingPoolProvider;
import pl.edu.icm.unity.stdext.credential.pass.PasswordEngine;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExchange;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;

/**
 * Composite password verificator is only a relay to other local or remote
 * password verificators. Supports all settings from
 * {@link CompositePasswordProperties}. It is configured with a list of local
 * passwords and list of remote verificators. The behaviour of this verificator
 * is as follows: 1. check if authenticated user is a local user and has any of
 * the local credentials set. If yes verify using this credential (selecting the
 * first one available from the list). 2. otherwise try authenticating one by
 * one with configured remote verificators.
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class CompositePasswordVerificator extends AbstractVerificator implements PasswordExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN,
			CompositePasswordVerificator.class);

	public static final String NAME = "composite-password";
	public static final String DESC = "Verifies local or remote password";
	
	private Map<String, CredentialVerificatorFactory> credentialVerificatorFactories;
	private CredentialHelper credentialHelper;
	
	private List<LocalCredentialVerificator> localVerificators;
	private List<CredentialVerificator> remoteVerificators;
	private CompositePasswordProperties compositePasswordProperties;
	private NotificationProducer notificationProducer;
	private PasswordEngine passwordEngine;
	
	@Autowired
	public CompositePasswordVerificator(
			pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator.Factory passwordVerificator,
			pl.edu.icm.unity.pam.PAMVerificator.Factory pamVerificator,
			pl.edu.icm.unity.ldap.client.LdapPasswordVerificator.Factory ldapVerificator,
			CredentialHelper credentialHelper, NotificationProducer notificationProducer,
			Optional<PasswordEncodingPoolProvider> threadPoolProvider)
	{
		super(NAME, DESC, PasswordExchange.ID);
		this.credentialHelper = credentialHelper;
		this.notificationProducer = notificationProducer;
		credentialVerificatorFactories = new HashMap<>();
		credentialVerificatorFactories.put(PasswordVerificator.NAME, passwordVerificator);
		credentialVerificatorFactories.put(PAMVerificator.NAME, pamVerificator);
		credentialVerificatorFactories.put(LdapPasswordVerificator.NAME, ldapVerificator);
		localVerificators = new ArrayList<>();
		remoteVerificators = new ArrayList<>();
		this.passwordEngine = new PasswordEngine(threadPoolProvider
				.map(pp->pp.pool)
				.orElse(ForkJoinPool.commonPool()));
	}

	@Override
	public String getSerializedConfiguration()
	{
		StringWriter sbw = new StringWriter();
		try
		{
			compositePasswordProperties.getProperties().store(sbw, "");
		} catch (IOException e)
		{
			throw new InternalException(
					"Can't serialize composite-password verificator configuration",
					e);
		}
		return sbw.toString();
	}

	private LocalCredentialVerificator getLocalVerificator(CredentialVerificator verificator,
			String credential)
	{
		Optional<CredentialDefinition> credDef = CompositePasswordHelper
				.getCredentialDefinition(credentialHelper, credential);
		if (!credDef.isPresent())
		{
			throw new InternalException(
					"Invalid configuration of the verificator, local credential "
							+ credential + " is undefined");
		}
		verificator.setSerializedConfiguration(credDef.get().getConfiguration());
		verificator.setIdentityResolver(identityResolver);
		LocalCredentialVerificator localVerificator = (LocalCredentialVerificator) verificator;
		localVerificator.setCredentialName(credential);
		return localVerificator;
	}
	
	@Override
	public void setSerializedConfiguration(String config)
	{
		localVerificators.clear();
		remoteVerificators.clear();

		Properties properties = new Properties();
		try
		{
			properties.load(new StringReader(config));
		} catch (IOException e)
		{
			throw new InternalException(
					"Invalid configuration of the composite-password verificator", e);
		}
		compositePasswordProperties = new CompositePasswordProperties(properties);

		Set<String> verificatorList = compositePasswordProperties
				.getStructuredListKeys(CompositePasswordProperties.VERIFICATORS);

		for (String verificatorKey : verificatorList)
		{
			String type = compositePasswordProperties.getValue(verificatorKey
					+ CompositePasswordProperties.VERIFICATOR_TYPE);
			CredentialVerificatorFactory credentialVerificatorFactory = credentialVerificatorFactories
					.get(type);
			CredentialVerificator verificator = credentialVerificatorFactory.newInstance();
			verificator.setIdentityResolver(identityResolver);
			verificator.setInstanceName(NAME);

			if (credentialVerificatorFactory instanceof LocalCredentialVerificatorFactory)
			{

				String credential = compositePasswordProperties.getValue(
						verificatorKey + CompositePasswordProperties.VERIFICATOR_CREDENTIAL);
				localVerificators.add(getLocalVerificator(verificator, credential));

			} else
			{
				verificator.setSerializedConfiguration(getRemoteAuthenticatorConfig(verificatorKey));
				remoteVerificators.add(verificator);
			}
		}

	}
	
	private String getRemoteAuthenticatorConfig(String verificatorKey)
	{

		if (!compositePasswordProperties
				.isSet(verificatorKey + CompositePasswordProperties.VERIFICATOR_CONFIG_EMBEDDED)
				&& !compositePasswordProperties
						.isSet(verificatorKey + CompositePasswordProperties.VERIFICATOR_CONFIG))
		{
			throw new InternalException(
					"Misconfigured composite-password verificator, remote verificator has no defined configuration");
		}

		if (!compositePasswordProperties.isSet(verificatorKey + CompositePasswordProperties.VERIFICATOR_CONFIG))
		{
			return compositePasswordProperties.getValue(
					verificatorKey + CompositePasswordProperties.VERIFICATOR_CONFIG_EMBEDDED);
		} else
		{
			try
			{
				return FileUtils.readFileToString(compositePasswordProperties.getFileValue(
						verificatorKey + CompositePasswordProperties.VERIFICATOR_CONFIG, false),
						StandardCharsets.UTF_8);

			} catch (IOException e)
			{
				throw new InternalException(
						"Misconfigured composite-password verificator composite-password, remote verificator config file is not available",
						e);
			}
		}
	}

	@Override
	public AuthenticationResult checkPassword(String username, String password,
			String formForUnknown, boolean enableAssociation, 
			AuthenticationTriggeringContext triggeringContext) throws AuthenticationException
	{
		Optional<EntityWithCredential> resolveIdentity = CompositePasswordHelper.getLocalEntity(identityResolver, 
				AuthenticationSubject.identityBased(username));
		if (resolveIdentity.isPresent())
		{
			for (LocalCredentialVerificator localVerificator : localVerificators)
			{

				boolean isCredSet = CompositePasswordHelper.checkIfUserHasCredential(
						localVerificator,
						resolveIdentity.get().getEntityId());
				if (!isCredSet)
					continue;

				log.debug("Checking >{}< password using verificator with local credential >{}<",
						username, localVerificator.getCredentialName());
				PasswordExchange passExchange = (PasswordExchange) localVerificator;
				return passExchange.checkPassword(username, password,
						formForUnknown, enableAssociation, triggeringContext);

			}
		}

		for (CredentialVerificator remoteVerificator : remoteVerificators)
		{
			log.debug("Checking >{}< password using remote verificator >{}<", username, 
					remoteVerificator.getName());
			PasswordExchange passExchange = (PasswordExchange) remoteVerificator;
			AuthenticationResult result = passExchange.checkPassword(username, password,
					formForUnknown, enableAssociation, triggeringContext);
			if (result.getStatus().equals(Status.deny)
					|| result.getStatus().equals(Status.notApplicable))
				continue;

			return result;
		}

		log.info("Password provided by {} is invalid", username);
		return LocalAuthenticationResult.failed(new ResolvableError("WebPasswordRetrieval.wrongPassword"));
	}

	private List<LocalCredentialVerificator> getVerificatorsWithCredentialResetSupport()
	{
		List<LocalCredentialVerificator> ret = new ArrayList<>();
		for (LocalCredentialVerificator localVerificator : localVerificators)
		{

			Optional<CredentialDefinition> credDef = CompositePasswordHelper
					.getCredentialDefinition(credentialHelper,
							localVerificator.getCredentialName());
			if (!credDef.isPresent())
				continue;

			PasswordCredential passCred = new PasswordCredential();
			passCred.setSerializedConfiguration(
					JsonUtil.parse(credDef.get().getConfiguration()));
			if (passCred.getPasswordResetSettings().isEnabled())
			{
				ret.add(localVerificator);
			}

		}
		return ret;
	}

	@Override
	public CredentialReset getCredentialResetBackend()
	{
		List<LocalCredentialVerificator> localVerificatorWithReset = getVerificatorsWithCredentialResetSupport();
		if (localVerificatorWithReset.isEmpty())
			return new NoCredentialResetImpl();

		return new CompositePasswordResetImpl(credentialHelper, localVerificatorWithReset,
				identityResolver, notificationProducer, passwordEngine);
	}
	
	@Override
	public VerificatorType getType()
	{
		return VerificatorType.Mixed;
	}
	
	@Override
	public AuthenticationMethod getAuthenticationMethod()
	{
		return AuthenticationMethod.pwd;
	}

	@Component
	public static class Factory extends AbstractCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<CompositePasswordVerificator> factory)
		{
			super(NAME, DESC, factory);
		}
	}
}
