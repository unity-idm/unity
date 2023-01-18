/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.integration;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.StatusLine;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.Binder;
import com.vaadin.server.ErrorMessage;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.integration.IntegrationEvent.EventType;
import pl.edu.icm.unity.engine.api.integration.IntegrationEventConfiguration;
import pl.edu.icm.unity.engine.api.integration.Webhook;
import pl.edu.icm.unity.engine.api.integration.Webhook.WebhookHttpMethod;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.webhook.WebhookProcessor;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Integration event webhook configuration editor
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class WebhookEditor extends CustomField<IntegrationEventConfiguration>
		implements IntegrationEventConfigurationEditor
{
	private Binder<WebhookVaadinBean> binder;
	private MessageSource msg;
	private PKIManagement pkiMan;
	private WebhookProcessor webhookProcessor;

	@Autowired
	public WebhookEditor(MessageSource msg, PKIManagement pkiMan, WebhookProcessor webhookProcessor)
	{
		this.msg = msg;
		this.pkiMan = pkiMan;
		this.webhookProcessor = webhookProcessor;
		this.binder = new Binder<>(WebhookVaadinBean.class);
		binder.setBean(new WebhookVaadinBean());
		binder.addValueChangeListener(e -> {
			if (binder.isValid())
			{
				fireEvent(new ValueChangeEvent<IntegrationEventConfiguration>(this, getValue(), true));
			}
		});
	}

	@Override
	public IntegrationEventConfiguration getValue()
	{
		if (hasErrors())
		{
			return null;
		}

		WebhookVaadinBean bean = binder.getBean();
		return new Webhook(bean.getUrl(), bean.getHttpMethod(), bean.getTruststore(), bean.getSecret());
	}

	@Override
	public void setComponentError(ErrorMessage componentError)
	{
		binder.validate();
	}

	@Override
	public void setRequiredIndicatorVisible(boolean visible)
	{
		super.setRequiredIndicatorVisible(false);
	}

	@Override
	protected Component initContent()
	{
		FormLayout main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);
		EnumComboBox<WebhookHttpMethod> httpMethod = new EnumComboBox<Webhook.WebhookHttpMethod>(msg,
				"WebhookHttpMethod.", WebhookHttpMethod.class, WebhookHttpMethod.GET);
		httpMethod.setCaption(msg.getMessage("WebhookEditor.httpMethod"));
		binder.forField(httpMethod).asRequired().bind("httpMethod");
		main.addComponent(httpMethod);
		
		ComboBox<String> httpsTruststore = new ComboBox<>(msg.getMessage("WebhookEditor.httpsTruststore"));
		httpsTruststore.setItems(getTrustostores());
		binder.forField(httpsTruststore).bind("truststore");
		main.addComponent(httpsTruststore);
		
		TextField secret =  new TextField();
		secret.setCaption(msg.getMessage("WebhookEditor.secret"));
		secret.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		binder.forField(secret).bind("secret");
		main.addComponent(secret);
		
		TextField url = new TextField();
		url.setCaption(msg.getMessage("WebhookEditor.url"));
		url.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		binder.forField(url).asRequired().bind("url");
		main.addComponent(url);
		return main;
	}

	@Override
	protected void doSetValue(IntegrationEventConfiguration value)
	{
		if (value == null)
			return;
		Webhook webhook = (Webhook) value;
		binder.setBean(new WebhookVaadinBean(webhook));
	}

	public boolean hasErrors()
	{
		return binder.validate().hasErrors();
	}

	private Set<String> getTrustostores()
	{
		try
		{
			return pkiMan.getValidatorNames();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("WebhookEditor.getTruststoresError"), e);
			return Collections.emptySet();
		}
	}

	@org.springframework.stereotype.Component
	public static class WebhookIntegrationEventEditorFactory implements IntegrationEventConfigurationEditorFactory
	{

		private ObjectFactory<WebhookEditor> factory;

		public WebhookIntegrationEventEditorFactory(ObjectFactory<WebhookEditor> factory)
		{
			this.factory = factory;
		}

		@Override
		public String supportedType()
		{
			return EventType.WEBHOOK.toString();
		}

		@Override
		public IntegrationEventConfigurationEditor getEditor(String trigger)
		{
			return factory.getObject();
		}
	}

	@Override
	public void setTrigger(String trigger)
	{

	}
	
	@Override
	public Component test(Map<String, String> params) throws EngineException
	{
		if (getValue() == null)
			return null;
		Webhook webhook = (Webhook) getValue();
		VerticalLayout mainLayout = new VerticalLayout();
		ClassicHttpResponse resp = webhookProcessor.trigger(webhook, params);
		Label statusCode = new Label();
		statusCode.setCaption(msg.getMessage("WebhookEditor.statusCode"));
		statusCode.setValue(String.valueOf(resp.getCode()));
		mainLayout.addComponent(statusCode);
		
		Label statusLine = new Label();
		statusLine.setCaption(msg.getMessage("WebhookEditor.statusLine"));
		statusLine.setValue(new StatusLine(resp).toString());
		mainLayout.addComponent(statusLine);
		
		Label body = new Label();
		body.setCaption(msg.getMessage("WebhookEditor.responseBody"));
		HttpEntity entity = resp.getEntity();

		try
		{
			String responseString = EntityUtils.toString(entity);
			body.setValue(responseString);
			mainLayout.addComponent(body);
			
		} catch (Exception e)
		{
			throw new EngineException(e);
		}
			
		return mainLayout;		
	}
	

	public static class WebhookVaadinBean
	{

		private String url;
		private WebhookHttpMethod httpMethod;
		private String truststore;
		private String secret;

		public WebhookVaadinBean()
		{
			httpMethod = WebhookHttpMethod.GET;
		}

		public WebhookVaadinBean(Webhook webhook)
		{

			this.truststore = webhook.truststore;
			this.url = webhook.url;
			this.httpMethod = webhook.httpMethod;
			this.secret = webhook.secret;
		}

		public String getUrl()
		{
			return url;
		}

		public void setUrl(String url)
		{
			this.url = url;
		}

		public WebhookHttpMethod getHttpMethod()
		{
			return httpMethod;
		}

		public void setHttpMethod(WebhookHttpMethod httpMethod)
		{
			this.httpMethod = httpMethod;
		}

		public String getTruststore()
		{
			return truststore;
		}

		public void setTruststore(String httpsTruststore)
		{
			this.truststore = httpsTruststore;
		}

		public String getSecret()
		{
			return secret;
		}

		public void setSecret(String secret)
		{
			this.secret = secret;
		}

	}
}
