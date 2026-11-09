package io.imunity.test.ui.flows;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import io.imunity.vaadin.endpoint.common.forms.components.CaptchaImageFactory;
import nl.captcha.Captcha;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.stdext.credential.CredentialResetBase;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetImpl;
import pl.edu.icm.unity.stdext.credential.pass.PasswordEncodingPoolProvider;
import pl.edu.icm.unity.stdext.credential.pass.PasswordEngine;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;

@Configuration(proxyBeanMethods = false)
class PasswordRecoveryConfiguration
{
	@Bean
	@Primary
	CapturedCaptcha capturedCaptcha()
	{
		return new CapturedCaptcha();
	}

	@Bean
	RecoveryCodeValidity recoveryCodeValidity()
	{
		return new RecoveryCodeValidity();
	}

	@Bean
	@Primary
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	PasswordVerificator recoveryPasswordVerificator(NotificationProducer notifications, CredentialHelper credentials,
			Optional<PasswordEncodingPoolProvider> pools, RecoveryCodeValidity validity)
	{
		return new PasswordVerificator(notifications, credentials, pools)
		{
			@Override
			public CredentialReset getCredentialResetBackend()
			{
				if (validity.duration.equals(CredentialResetBase.DEFAULT_MAX_CODE_VALIDITY))
					return super.getCredentialResetBackend();
				PasswordCredential configuration = new PasswordCredential();
				configuration.setSerializedConfiguration(JsonUtil.parse(getSerializedConfiguration()));
				return new PasswordCredentialResetImpl(notifications, identityResolver, this, credentials,
						credentialName, configuration.getSerializedConfiguration(),
						configuration.getPasswordResetSettings(),
						new PasswordEngine(pools.map(provider -> provider.pool).orElse(ForkJoinPool.commonPool())),
						validity.duration);
			}
		};
	}

	public static final class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext>
	{
		@Override
		public void initialize(ConfigurableApplicationContext context)
		{
			new AnnotatedBeanDefinitionReader((BeanDefinitionRegistry) context)
					.register(PasswordRecoveryConfiguration.class);
		}
	}

	static class RecoveryCodeValidity
	{
		private volatile Duration duration = CredentialResetBase.DEFAULT_MAX_CODE_VALIDITY;

		void expireImmediately()
		{
			duration = Duration.ZERO;
		}
	}

	static class CapturedCaptcha extends CaptchaImageFactory
	{
		private volatile Captcha latest;

		@Override
		public Captcha create(int length)
		{
			Captcha captcha = super.create(length);
			latest = captcha;
			return captcha;
		}

		Captcha latest()
		{
			return latest;
		}
	}
}
