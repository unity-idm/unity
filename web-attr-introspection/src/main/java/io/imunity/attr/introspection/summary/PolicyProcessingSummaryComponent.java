/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.summary;

import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.attr.introspection.config.AttrIntrospectionAttributePoliciesConfiguration;
import io.imunity.attr.introspection.config.Attribute;
import io.imunity.vaadin.elements.NotificationPresenter;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@PrototypeComponent
public class PolicyProcessingSummaryComponent extends VerticalLayout
{
	private final MessageSource msg;
	private final AuthenticatorSupportService authenticatorSupport;
	private final NotificationPresenter notificationPresenter;

	private AttributePolicyProcessor policyResolver;
	private Runnable resetUI;
	
	PolicyProcessingSummaryComponent(MessageSource msg, AuthenticatorSupportService authenticatorSupport, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.authenticatorSupport = authenticatorSupport;
		this.notificationPresenter = notificationPresenter;
		initUI();
	}

	PolicyProcessingSummaryComponent configure(AttrIntrospectionAttributePoliciesConfiguration config, Runnable resetUI)
	{
		this.policyResolver = new AttributePolicyProcessor(config, authenticatorSupport);
		this.resetUI = resetUI;
		return this;
	}

	public void setPolicyProcessingResultForUser(RemotelyAuthenticatedPrincipal remotelyAuthenticatedPrincipal, HtmlContainer container)
	{
		Optional<PolicyProcessingResult> policyProcessingResult = getPolicyProcessingResult(
				remotelyAuthenticatedPrincipal);
		if (policyProcessingResult.isEmpty())
			return;
		setResult(policyProcessingResult.get(), container);
	}

	private Optional<PolicyProcessingResult> getPolicyProcessingResult(
			RemotelyAuthenticatedPrincipal remotelyAuthenticatedPrincipal)
	{
		try
		{
			return Optional.of(policyResolver.applyPolicyForRemoteUser(remotelyAuthenticatedPrincipal));
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("PolicyProcessingSummaryComponent.getPolicyProcessingResultError"), e.getMessage());
			return Optional.empty();
		}

	}

	private void setResult(PolicyProcessingResult result, HtmlContainer container)
	{

		removeAll();
		setAlignItems(Alignment.CENTER);

		H2 title = new H2();
		title.setText(msg.getMessage("PolicyProcessingSummaryComponent.title"));
		add(title);

		HorizontalLayout summaryTitle = getSummaryLabel(result);
		add(summaryTitle);

		int mandatoryAttributeSize = result.policy.getMandatoryAttributes().size();
		int optionalAttributeSize = result.policy.getOptionalAttributes().size();
		
		if (mandatoryAttributeSize > 0)
		{
			Span mandatorySummary = getSummaryLine(mandatoryAttributeSize, result.missingMandatory.size(),
					msg.getMessage("PolicyProcessingSummaryComponent.MandatoryAttributeProvided"));
			add(mandatorySummary);
		}
	
		if (optionalAttributeSize > 0)
		{
			Span optionalSummary = getSummaryLine(optionalAttributeSize, result.missingOptional.size(),
					msg.getMessage("PolicyProcessingSummaryComponent.OptionalAttributeProvided"));
			add(optionalSummary);
		}

		if (!result.missingMandatory.isEmpty())
		{
			add(getAttributesComponent(msg.getMessage("PolicyProcessingSummaryComponent.missingMandatory"),
					"badge small error",
					result.missingMandatory));
		}

		if (!result.missingOptional.isEmpty())
		{
			add(getAttributesComponent(msg.getMessage("PolicyProcessingSummaryComponent.missingOptional"),
					"badge small contrast",
					result.missingOptional));
		}

		add(getReceivedAttributeComponent(result.allReceivedAttributes, container));

		Button tryAgain = new Button(msg.getMessage("PolicyProcessingSummaryComponent.tryAgain"));
		tryAgain.setId("PolicyProcessingSummaryComponent.TryAgain");
		tryAgain.addClickListener(e ->
		{
			removeAll();
			resetUI.run();
		});
		add(tryAgain);
	}

	private void initUI()
	{
		removeAll();
		setWidth(55, Unit.EM);
		setAlignItems(Alignment.CENTER);
	}

	private VerticalLayout getAttributesComponent(String title, String theme, List<Attribute> attributes)
	{
		VerticalLayout attrPart = new VerticalLayout();
		attrPart.add(getTitleLabel(title));
		attrPart.add(new Hr());
		attrPart.add(getAttributeList(attributes, theme));
		return attrPart;
	}

	private FormLayout getAttributeList(List<Attribute> attributes, String theme)
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new ResponsiveStep("0", 1));

		for (Attribute attr : attributes)
		{
			Span attributeLabel = new Span();
			attributeLabel.setText(attr.name);
			Icon icon = VaadinIcon.EXCLAMATION_CIRCLE.create();
			icon.getElement().getThemeList().add(theme);
			icon.getStyle().set("background-color", "transparent");
			icon.getStyle().set("margin", "0.5em");
			icon.getElement().getThemeList().add(theme);
			attributeLabel.addComponentAsFirst(icon);
			main.addFormItem(new Span(attr.description), attributeLabel)
					.getStyle().set("gap", "2em");
		}
		return main;
	}

	private Span getSummaryLine(long allAttributeSize, long missingAttributeSize, String summaryText)
	{
		Span summaryLine = new Span();
		if (missingAttributeSize == 0)
		{
			summaryLine.setText(msg.getMessage("PolicyProcessingSummaryComponent.all") + " " + summaryText);
		} else
		{
			summaryLine.setText(
					String.format("%.0f", (100.0 / allAttributeSize * (allAttributeSize - missingAttributeSize)))
							+ "% (" + (allAttributeSize - missingAttributeSize) + "/" + allAttributeSize + ") "
							+ summaryText);
		}
		return summaryLine;
	}

	private H5 getTitleLabel(String value)
	{
		H5 summaryTitle = new H5();
		summaryTitle.setText(value);
		return summaryTitle;
	}

	private HorizontalLayout getSummaryLabel(PolicyProcessingResult result)
	{
		HorizontalLayout wrapper = new HorizontalLayout();

		Span summaryTitle = new Span();
		summaryTitle.setWidth("10em");
		summaryTitle.setHeight("3em");
		summaryTitle.getStyle().set("font-size", "1.5em");

		wrapper.add(summaryTitle);
		wrapper.setAlignItems(Alignment.CENTER);

		if (!result.missingMandatory.isEmpty())
		{
			summaryTitle.setText(msg.getMessage("PolicyProcessingSummaryComponent.insufficient"));
			summaryTitle.getElement().getThemeList().add("badge error");
			return wrapper;
		}
		if (!result.missingOptional.isEmpty())
		{
			summaryTitle.setText(msg.getMessage("PolicyProcessingSummaryComponent.good"));
			summaryTitle.getElement().getThemeList().add("badge contrast");
			summaryTitle.setClassName("u-badge-warning");
			return wrapper;
		}

		summaryTitle.setText(msg.getMessage("PolicyProcessingSummaryComponent.excellent"));
		summaryTitle.getElement().getThemeList().add("badge success");
		return wrapper;
	}

	private VerticalLayout getReceivedAttributeComponent(List<ReceivedAttribute> attributes, HtmlContainer container)
	{
		VerticalLayout receivedAttributesLayout = new VerticalLayout();
		receivedAttributesLayout.add(getTitleLabel(msg.getMessage("PolicyProcessingSummaryComponent.allReceived")));
		receivedAttributesLayout.add(new Hr());
		FormLayout attributeListLayout = new FormLayout();
		attributeListLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
		for (ReceivedAttribute attr : attributes)
		{
			attributeListLayout.addFormItem(getAttributeLabel(attr, container), attr.name)
					.getStyle().set("gap", "2em");
		}
		receivedAttributesLayout.add(attributeListLayout);
		return receivedAttributesLayout;
	}

	private static LabelWithTooltip getAttributeLabel(ReceivedAttribute attr, HtmlContainer container)
	{
		String value = attr.values == null ? ""
				: attr.values.stream().map(o -> (String) o).collect(Collectors.joining(", "));
		return new LabelWithTooltip(value, attr.description, container);
	}

	@Component
	public static class PolicyProcessingSummaryComponentFactory
	{
		private final ObjectFactory<PolicyProcessingSummaryComponent> factory;

		@Autowired
		public PolicyProcessingSummaryComponentFactory(ObjectFactory<PolicyProcessingSummaryComponent> factory)
		{
			this.factory = factory;
		}

		public PolicyProcessingSummaryComponent getInstance(AttrIntrospectionAttributePoliciesConfiguration config, Runnable resetUI)
		{
			return factory.getObject().configure(config, resetUI);
		}
	}
	
	private static class LabelWithTooltip extends HorizontalLayout
	{
		public LabelWithTooltip(String value, Optional<String> description, HtmlContainer container)
		{
			setWidthFull();
			Span valueLabel = new Span();
			valueLabel.setText(value);
			valueLabel.setWidthFull();
			valueLabel.getStyle().set("overflow-wrap", "anywhere");
			Icon icon = VaadinIcon.QUESTION_CIRCLE_O.create();
			if (description.isPresent())
				icon.setTooltipText(description.get());
			else
				icon.setVisible(false);
			add(valueLabel, icon);
		}
	}
}
