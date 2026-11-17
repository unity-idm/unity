package io.imunity.test.ui.flows;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

import io.imunity.test.ui.accounts.UserAccountTestBase;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.i18n.I18nMessage;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.msg_template.MessageType;
import pl.edu.icm.unity.base.notifications.NotificationChannel;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

@ContextConfiguration(initializers = NotificationCaptureConfiguration.Initializer.class)
public abstract class EndUserTestBase extends UserAccountTestBase
{
	private final List<String> deployedEndpoints = new ArrayList<>();

	@Autowired
	protected CapturedNotifications notifications;
	@Autowired
	@Qualifier("insecure")
	protected EndpointManagement endpoints;
	@Autowired
	@Qualifier("insecure")
	private NotificationsManagement channels;
	@Autowired
	@Qualifier("insecure")
	private MessageTemplateManagement templates;

	@BeforeEach
	void configureNotificationCapture() throws Exception
	{
		configureChannel(UnityServerConfiguration.DEFAULT_EMAIL_CHANNEL, "ui-email");
		configureChannel(UnityServerConfiguration.DEFAULT_SMS_CHANNEL, "ui-sms");
	}

	@AfterEach
	void removeDeployedEndpoints() throws Exception
	{
		for (String endpoint : deployedEndpoints.reversed())
			endpoints.removeEndpoint(endpoint);
	}

	protected final void messageTemplate(String name, String consumer, String body, String channel) throws Exception
	{
		templates.addTemplate(new MessageTemplate(name, "Selenium workflow", new I18nMessage(
				new I18nString(name), new I18nString(body)), consumer, MessageType.PLAIN, channel));
	}

	protected final CapturedNotifications.Message notificationTo(String recipient)
	{
		waitFor(() -> !notifications.sentTo(recipient).isEmpty());
		return notifications.sentTo(recipient).getLast();
	}

	protected final String deployHomeAt(String path, String settings) throws Exception
	{
		return deployAt("UserHomeUI", path, settings);
	}

	protected final String deployAt(String type, String path, String settings) throws Exception
	{
		EndpointConfiguration template = endpoints.getEndpoint("Selenium Home").getConfiguration();
		return deployAt(type, path, settings, template.getAuthenticationOptions());
	}

	protected final String deployAt(String type, String path, String settings, List<String> authentication)
			throws Exception
	{
		return deployAt(type, path, settings, authentication,
				endpoints.getEndpoint("Selenium Home").getConfiguration().getRealm());
	}

	protected final String deployAt(String type, String path, String settings,
			List<String> authentication, String realm)
			throws Exception
	{
		EndpointConfiguration template = endpoints.getEndpoint("Selenium Home").getConfiguration();
		String name = "End-user fixture " + UUID.randomUUID();
		endpoints.deploy(type, name, path, new EndpointConfiguration(new I18nString(name),
				"Selenium end-user workflow", authentication,
				template.getConfiguration() + "\n" + settings, realm));
		deployedEndpoints.add(name);
		return name;
	}

	private void configureChannel(String name, String facility) throws Exception
	{
		if (channels.getNotificationChannels().containsKey(name))
			channels.removeNotificationChannel(name);
		channels.addNotificationChannel(new NotificationChannel(name, "Selenium capture", "", facility));
	}
}
