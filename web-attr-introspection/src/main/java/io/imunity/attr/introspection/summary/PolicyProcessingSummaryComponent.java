/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.summary;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.attr.introspection.config.AttrIntrospectionAttributePoliciesConfiguration;
import io.imunity.attr.introspection.config.Attribute;
import io.imunity.tooltip.TooltipExtension;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlSimplifiedLabelWithLinks;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

@PrototypeComponent
public class PolicyProcessingSummaryComponent extends CustomComponent
{
	private final MessageSource msg;
	private final AuthenticatorSupportService authenticatorSupport;

	private VerticalLayout main;
	private AttributePolicyProcessor policyResolver;

	PolicyProcessingSummaryComponent(MessageSource msg, AuthenticatorSupportService authenticatorSupport)
	{
		this.msg = msg;
		this.authenticatorSupport = authenticatorSupport;
		initUI();
	}

	PolicyProcessingSummaryComponent configure(AttrIntrospectionAttributePoliciesConfiguration config)
	{
		policyResolver = new AttributePolicyProcessor(config, authenticatorSupport);
		return this;
	}

	public void setPolicyProcessingResultForUser(RemotelyAuthenticatedPrincipal remotelyAuthenticatedPrincipal)
	{
		Optional<PolicyProcessingResult> policyProcessingResult = getPolicyProcessingResult(
				remotelyAuthenticatedPrincipal);
		if (policyProcessingResult.isEmpty())
			return;
		setResult(policyProcessingResult.get());
	}

	private Optional<PolicyProcessingResult> getPolicyProcessingResult(
			RemotelyAuthenticatedPrincipal remotelyAuthenticatedPrincipal)
	{
		try
		{
			return Optional.of(policyResolver.applyPolicyForRemoteUser(remotelyAuthenticatedPrincipal));
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("PolicyProcessingSummaryComponent.getPolicyProcessingResultError"), e);
			return Optional.empty();
		}

	}

	private void setResult(PolicyProcessingResult result)
	{

		main.removeAllComponents();

		Label title = new Label();
		title.setValue(msg.getMessage("PolicyProcessingSummaryComponent.title"));
		title.addStyleName(Styles.textTitle.toString());
		title.addStyleName(Styles.bold.toString());
		main.addComponent(title);
		main.setComponentAlignment(title, Alignment.TOP_CENTER);

		HorizontalLayout summaryTitle = getSummaryLabel(result);
		main.addComponent(summaryTitle);
		main.setComponentAlignment(summaryTitle, Alignment.TOP_CENTER);

		int mandatoryAttributeSize = result.policy.getMandatoryAttributes().size();
		if (mandatoryAttributeSize > 0)
		{
			Label mandatorySummary = getSummaryLine(mandatoryAttributeSize, result.missingMandatory.size(),
					msg.getMessage("PolicyProcessingSummaryComponent.MandatoryAttributeProvided"));
			main.addComponent(mandatorySummary);
			main.setComponentAlignment(mandatorySummary, Alignment.TOP_CENTER);
		}
		int optionalAttributeSize = result.policy.getOptionalAttributes().size();
		if (optionalAttributeSize > 0)
		{
			Label optionalSummary = getSummaryLine(optionalAttributeSize, result.missingOptional.size(),
					msg.getMessage("PolicyProcessingSummaryComponent.OptionalAttributeProvided"));
			main.addComponent(optionalSummary);
			main.setComponentAlignment(optionalSummary, Alignment.TOP_CENTER);
		}

		if (!result.missingMandatory.isEmpty())
		{
			main.addComponent(getAttributesComponent(msg.getMessage("PolicyProcessingSummaryComponent.missingMandatory"), Styles.labelError,
					result.missingMandatory));
		}

		if (!result.missingOptional.isEmpty())
		{
			main.addComponent(getAttributesComponent(msg.getMessage("PolicyProcessingSummaryComponent.missingOptional"), Styles.labelWarn,
					result.missingOptional));
		}

		main.addComponent(getReceivedAttributeComponent(result.allReceivedAttributes));

	}

	private void initUI()
	{
		this.main = new VerticalLayout();
		setCompositionRoot(main);
		setWidth(50, Unit.EM);
		main.setWidth(50, Unit.EM);
	}

	private VerticalLayout getAttributesComponent(String title, Styles style, List<Attribute> attributes)
	{
		VerticalLayout attrPart = new VerticalLayout();
		attrPart.addComponent(getTitleLabel(title));
		attrPart.addComponent(HtmlTag.horizontalLine());
		attrPart.addComponent(getAttributeList(attributes, style));
		return attrPart;
	}

	private FormLayoutWithFixedCaptionWidth getAttributeList(List<Attribute> attributes, Styles style)
	{
		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);
		for (Attribute attr : attributes)
		{
			HtmlSimplifiedLabelWithLinks attributeLabel = new HtmlSimplifiedLabelWithLinks();
			attributeLabel.setCaption(attr.name);
			attributeLabel.setValue(attr.description);
			attributeLabel.setContentMode(ContentMode.HTML);
			attributeLabel.setIcon(Images.error.getResource());
			attributeLabel.setStyleName(style.toString());
			attributeLabel.addStyleName(Styles.wordWrap.toString());
			main.addComponent(attributeLabel);
		}
		return main;
	}

	private Label getSummaryLine(long allAttributeSize, long missingAttributeSize, String summaryText)
	{
		Label summaryLine = new Label();
		summaryLine.addStyleName(Styles.textLarge.toString());
		summaryLine.addStyleName(Styles.bold.toString());

		if (missingAttributeSize == 0)
		{
			summaryLine.setValue(msg.getMessage("PolicyProcessingSummaryComponent.all") + " " + summaryText);
		} else
		{
			summaryLine.setValue(
					String.format("%.0f", (100.0 / allAttributeSize * (allAttributeSize - missingAttributeSize)))
							+ "% (" + (allAttributeSize - missingAttributeSize) + "/" + allAttributeSize + ") "
							+ summaryText);
		}
		return summaryLine;
	}

	private Label getTitleLabel(String value)
	{
		Label summaryTitle = new Label();
		summaryTitle.setValue(value);
		summaryTitle.addStyleName(Styles.textLarge.toString());
		summaryTitle.addStyleName(Styles.bold.toString());
		return summaryTitle;
	}

	private HorizontalLayout getSummaryLabel(PolicyProcessingResult result)
	{
		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setWidth(10, Unit.EM);

		Label summaryTitle = new Label();
		summaryTitle.addStyleName(Styles.textXLarge.toString());
		summaryTitle.addStyleName(Styles.bold.toString());
		wrapper.addComponent(summaryTitle);
		wrapper.setComponentAlignment(summaryTitle, Alignment.MIDDLE_CENTER);

		if (!result.missingMandatory.isEmpty())
		{
			summaryTitle.setValue(msg.getMessage("PolicyProcessingSummaryComponent.insufficient"));
			wrapper.addStyleName(Styles.errorBackground.toString());
			return wrapper;
		}
		if (!result.missingOptional.isEmpty())
		{
			summaryTitle.setValue(msg.getMessage("PolicyProcessingSummaryComponent.good"));
			wrapper.addStyleName(Styles.warnBackground.toString());
			return wrapper;
		}

		summaryTitle.setValue(msg.getMessage("PolicyProcessingSummaryComponent.excellent"));
		wrapper.addStyleName(Styles.successBackground.toString());
		return wrapper;
	}

	private VerticalLayout getReceivedAttributeComponent(List<ReceivedAttribute> attributes)
	{
		VerticalLayout receivedAttributesLayout = new VerticalLayout();
		receivedAttributesLayout.addComponent(getTitleLabel(msg.getMessage("PolicyProcessingSummaryComponent.allReceived")));
		receivedAttributesLayout.addComponent(HtmlTag.horizontalLine());
		FormLayoutWithFixedCaptionWidth attributeListLayout = new FormLayoutWithFixedCaptionWidth();
		attributeListLayout.setMargin(false);
		for (ReceivedAttribute attr : attributes)
		{
			Label attributeLabel = new Label();
			if (!attr.description.isEmpty())
				TooltipExtension.tooltip(attributeLabel, attr.description.get());
			attributeLabel.setCaption(attr.name);
			attributeLabel.setValue(attr.values == null ? ""
					: String.join(", ", attr.values.stream().map(o -> (String) o).collect(Collectors.toList())));
			attributeLabel.addStyleName(Styles.wordWrap.toString());
			attributeListLayout.addComponent(attributeLabel);
		}
		receivedAttributesLayout.addComponent(attributeListLayout);
		return receivedAttributesLayout;
	}

	@Component
	public static class PolicyProcessingSummaryComponentFactory
	{
		private ObjectFactory<PolicyProcessingSummaryComponent> factory;

		@Autowired
		public PolicyProcessingSummaryComponentFactory(ObjectFactory<PolicyProcessingSummaryComponent> factory)
		{
			this.factory = factory;
		}

		public PolicyProcessingSummaryComponent getInstance(AttrIntrospectionAttributePoliciesConfiguration config)
		{
			return factory.getObject().configure(config);
		}
	}

}
