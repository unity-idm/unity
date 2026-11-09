package io.imunity.test.ui.flows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.notifications.CommunicationTechnology;
import pl.edu.icm.unity.base.registration.UserRequestState;
import pl.edu.icm.unity.engine.api.notification.NotificationStatus;
import pl.edu.icm.unity.engine.notifications.MessageTemplateParams;
import pl.edu.icm.unity.engine.notifications.NotificationChannelInstance;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.engine.notifications.email.EmailFacility;
import pl.edu.icm.unity.engine.notifications.sms.SMSFacility;

@Configuration(proxyBeanMethods = false)
class NotificationCaptureConfiguration
{
	@Bean
	CapturedNotifications capturedNotifications()
	{
		return new CapturedNotifications();
	}

	@Bean
	NotificationFacility capturedEmail(EmailFacility addresses, CapturedNotifications notifications)
	{
		return new CapturingFacility("ui-email", addresses, notifications);
	}

	@Bean
	NotificationFacility capturedSms(SMSFacility addresses, CapturedNotifications notifications)
	{
		return new CapturingFacility("ui-sms", addresses, notifications);
	}

	public static final class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext>
	{
		@Override
		public void initialize(ConfigurableApplicationContext context)
		{
			new AnnotatedBeanDefinitionReader((BeanDefinitionRegistry) context)
					.register(NotificationCaptureConfiguration.class);
		}
	}

	private record CapturingFacility(String name, NotificationFacility addresses, CapturedNotifications notifications)
			implements NotificationFacility
	{
		@Override
		public String getName()
		{
			return name;
		}

		@Override
		public String getDescription()
		{
			return "Local Selenium notification capture";
		}

		@Override
		public CommunicationTechnology getTechnology()
		{
			return addresses.getTechnology();
		}

		@Override
		public void validateConfiguration(String configuration)
		{
		}

		@Override
		public String getAddressForEntity(EntityParam recipient, String preferred, boolean confirmed)
				throws EngineException
		{
			return addresses.getAddressForEntity(recipient, preferred, confirmed);
		}

		@Override
		public String getAddressForUserRequest(UserRequestState<?> request) throws EngineException
		{
			return addresses.getAddressForUserRequest(request);
		}

		@Override
		public NotificationChannelInstance getChannel(String configuration)
		{
			return new NotificationChannelInstance()
			{
				@Override
				public String getFacilityId()
				{
					return name;
				}

				@Override
				public boolean providesMessageTemplatingFunctionality()
				{
					return false;
				}

				@Override
				public Future<NotificationStatus> sendNotification(String recipient, MessageTemplate.Message message)
				{
					notifications.add(getTechnology(), recipient, message);
					return CompletableFuture.completedFuture(new NotificationStatus());
				}

				@Override
				public Future<NotificationStatus> sendExternalTemplateMessage(String recipient,
						MessageTemplateParams params)
				{
					throw new UnsupportedOperationException("The fixture uses Unity message templates");
				}
			};
		}
	}
}
