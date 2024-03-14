/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services.idp;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.elements.NotEmptyComboBox;
import io.imunity.vaadin.auth.services.ServiceEditorBase;
import io.imunity.vaadin.auth.services.ServiceEditorComponent;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.engine.api.idp.IdpPolicyAgreementsConfiguration;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

/**
 * Common Idp service editor policy agreements tab. Produces
 * {@link IdpPolicyAgreementsConfiguration}. Can be used directly with the
 * vaadin binder
 * 
 * @author P.Piernik
 *
 */
public class PolicyAgreementsTab extends CustomField<IdpPolicyAgreementsConfiguration> implements ServiceEditorBase.EditorTab
{
	private final MessageSource msg;
	private final Collection<PolicyDocumentWithRevision> policyDocuments;
	private Binder<IdpPolicyAgreementsConfigurationVaadinBean> binder;

	public PolicyAgreementsTab(MessageSource msg, Collection<PolicyDocumentWithRevision> policyDocuments)
	{
		this.msg = msg;
		this.policyDocuments = policyDocuments;
		init();
	}

	@Override
	public VaadinIcon getIcon()
	{
		return VaadinIcon.CHECK_SQUARE_O;
	}

	@Override
	public String getType()
	{
		return ServiceEditorComponent.ServiceEditorTab.POLICY_AGREEMENTS.toString();
	}

	private void init()
	{
		PolicyAgreementConfigurationList list = new PolicyAgreementConfigurationList(msg,
				() -> new PolicyAgreementConfigurationEditor(msg, policyDocuments));

		binder = new Binder<>(IdpPolicyAgreementsConfigurationVaadinBean.class);
		LocalizedTextFieldDetails title = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		title.setWidth(TEXT_FIELD_BIG.value());
		binder.forField(title).withValidator((v, c) -> {

			if (!list.getValue().isEmpty() && (v == null || v.isEmpty()))
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}

			return ValidationResult.ok();

		})
				.withConverter(I18nString::new, I18nString::getLocalizedMap)
				.bind(IdpPolicyAgreementsConfigurationVaadinBean::getTitle, IdpPolicyAgreementsConfigurationVaadinBean::setTitle);

		LocalizedTextFieldDetails info = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		info.setWidth(TEXT_FIELD_BIG.value());
		binder.forField(info)
				.withConverter(I18nString::new, I18nString::getLocalizedMap)
				.bind(IdpPolicyAgreementsConfigurationVaadinBean::getInformation, IdpPolicyAgreementsConfigurationVaadinBean::setInformation);

		IntegerField width = new IntegerField();
		width.setStepButtonsVisible(true);
		width.setMin(1);
		
		ComboBox<String> widthUnit = new NotEmptyComboBox<>();
		widthUnit.setItems(Stream.of(Unit.values()).map(Unit::toString).toList());
		binder.forField(width)
				.bind(IdpPolicyAgreementsConfigurationVaadinBean::getWidth, IdpPolicyAgreementsConfigurationVaadinBean::setWidth);
		binder.forField(widthUnit)
				.bind(IdpPolicyAgreementsConfigurationVaadinBean::getWidthUnit, IdpPolicyAgreementsConfigurationVaadinBean::setWidthUnit);

		binder.forField(list).withValidator((v, c) -> {
			for (PolicyAgreementConfiguration con : v)
			{
				if (con == null)
					return ValidationResult.error("");
			}
			return ValidationResult.ok();
		}).bind(IdpPolicyAgreementsConfigurationVaadinBean::getAgreements, IdpPolicyAgreementsConfigurationVaadinBean::setAgreements);
		VerticalLayout main = new VerticalLayout();
		main.setPadding(false);
		FormLayout header = new FormLayout();
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		header.addFormItem(title, msg.getMessage("PolicyAgreementTab.title"));
		header.addFormItem(info, msg.getMessage("PolicyAgreementTab.info"));
		
		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setMargin(false);
		wrapper.add(width);
		wrapper.add(widthUnit);
		header.addFormItem(wrapper, msg.getMessage("PolicyAgreementTab.width"));
		main.add(header);
		main.add(list);
		add(main);
		binder.addValueChangeListener(e -> fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), true)));
	}

	@Override
	public IdpPolicyAgreementsConfiguration getValue()
	{
		if (binder.validate().hasErrors())
			return null;
		IdpPolicyAgreementsConfigurationVaadinBean bean = binder.getBean();
		if (bean == null)
			return null;
		return new IdpPolicyAgreementsConfiguration(bean.getTitle(), bean.getInformation(), bean.getWidth(),
				bean.getWidthUnit(), bean.getAgreements());
	}

	@Override
	public void setValue(IdpPolicyAgreementsConfiguration value)
	{
		binder.setBean(new IdpPolicyAgreementsConfigurationVaadinBean(value));
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("IdpServiceEditorBase.policyAgreements");
	}

	@Override
	protected IdpPolicyAgreementsConfiguration generateModelValue()
	{
		return getValue();
	}

	@Override
	protected void setPresentationValue(IdpPolicyAgreementsConfiguration idpPolicyAgreementsConfiguration)
	{
		setValue(idpPolicyAgreementsConfiguration);
	}

	public static class IdpPolicyAgreementsConfigurationVaadinBean
	{
		private I18nString title = new I18nString();
		private I18nString information = new I18nString();
		private List<PolicyAgreementConfiguration> agreements;
		private int width;
		private String widthUnit;

		public IdpPolicyAgreementsConfigurationVaadinBean(IdpPolicyAgreementsConfiguration source)
		{
			this.agreements = new ArrayList<>();
			if (source == null)
				return;
			this.agreements.addAll(source.agreements);
			this.title = source.title == null ? new I18nString() : source.title;
			this.information = source.information == null ? new I18nString() : source.information;
			this.width = source.width;
			this.widthUnit = source.widthUnit;
		}

		public I18nString getTitle()
		{
			return title;
		}

		public void setTitle(I18nString title)
		{
			this.title = title;
		}

		public List<PolicyAgreementConfiguration> getAgreements()
		{
			return agreements;
		}

		public void setAgreements(List<PolicyAgreementConfiguration> agreements)
		{
			this.agreements = agreements;
		}

		public I18nString getInformation()
		{
			return information;
		}

		public void setInformation(I18nString information)
		{
			this.information = information;
		}

		public int getWidth()
		{
			return width;
		}

		public void setWidth(int width)
		{
			this.width = width;
		}

		public String getWidthUnit()
		{
			return widthUnit;
		}

		public void setWidthUnit(String widthUnit)
		{
			this.widthUnit = widthUnit;
		}
	}
}
