/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Builder of {@link BaseForm}
 */
public class BaseFormBuilder<T extends BaseFormBuilder<?>>
{
	private BaseForm instance;

	protected BaseFormBuilder(BaseForm aInstance)
	{
		instance = aInstance;
	}

	protected BaseForm getInstance()
	{
		return instance;
	}

	@SuppressWarnings("unchecked")
	public T withByInvitationOnly(boolean aValue)
	{
		instance.setByInvitationOnly(aValue);
		return (T) this;
	}
	
	@SuppressWarnings("unchecked")
	public T withTranslationProfile(TranslationProfile profile)
	{
		instance.setTranslationProfile(profile);
		return (T) this;
	}
	
	@SuppressWarnings("unchecked")
	public T withIdentityParams(List<IdentityRegistrationParam> aValue)
	{
		instance.setIdentityParams(aValue);

		return (T) this;
	}
	
	@SuppressWarnings("unchecked")
	public T withAddedIdentityParam(IdentityRegistrationParam aValue)
	{
		if (instance.getIdentityParams() == null)
		{
			instance.setIdentityParams(new ArrayList<IdentityRegistrationParam>());
		}

		((ArrayList<IdentityRegistrationParam>) instance.getIdentityParams()).add(aValue);

		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public IdentityRegistrationParamBuilder withAddedIdentityParam()
	{
		IdentityRegistrationParam obj = new IdentityRegistrationParam();

		withAddedIdentityParam(obj);

		return new IdentityRegistrationParamBuilder(obj, (T) this);
	}

	@SuppressWarnings("unchecked")
	public T withAttributeParams(List<AttributeRegistrationParam> aValue)
	{
		instance.setAttributeParams(aValue);

		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T withAddedAttributeParam(AttributeRegistrationParam aValue)
	{
		if (instance.getAttributeParams() == null)
		{
			instance.setAttributeParams(new ArrayList<AttributeRegistrationParam>());
		}

		((ArrayList<AttributeRegistrationParam>) instance.getAttributeParams()).add(aValue);

		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public AttributeRegistrationParamBuilder withAddedAttributeParam()
	{
		AttributeRegistrationParam obj = new AttributeRegistrationParam();

		withAddedAttributeParam(obj);

		return new AttributeRegistrationParamBuilder(obj, (T) this);
	}

	@SuppressWarnings("unchecked")
	public T withGroupParams(List<GroupRegistrationParam> aValue)
	{
		instance.setGroupParams(aValue);

		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T withAddedGroupParam(GroupRegistrationParam aValue)
	{
		if (instance.getGroupParams() == null)
		{
			instance.setGroupParams(new ArrayList<GroupRegistrationParam>());
		}

		((ArrayList<GroupRegistrationParam>) instance.getGroupParams()).add(aValue);

		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public GroupRegistrationParamBuilder withAddedGroupParam()
	{
		GroupRegistrationParam obj = new GroupRegistrationParam();

		withAddedGroupParam(obj);

		return new GroupRegistrationParamBuilder(obj, (T) this);
	}

	@SuppressWarnings("unchecked")
	public T withCredentialParams(List<CredentialRegistrationParam> aValue)
	{
		instance.setCredentialParams(aValue);

		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T withAddedCredentialParam(CredentialRegistrationParam aValue)
	{
		if (instance.getCredentialParams() == null)
		{
			instance.setCredentialParams(new ArrayList<CredentialRegistrationParam>());
		}

		((ArrayList<CredentialRegistrationParam>) instance.getCredentialParams())
				.add(aValue);

		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T withAgreements(List<AgreementRegistrationParam> aValue)
	{
		instance.setAgreements(aValue);

		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T withAddedAgreement(AgreementRegistrationParam aValue)
	{
		if (instance.getAgreements() == null)
		{
			instance.setAgreements(new ArrayList<AgreementRegistrationParam>());
		}

		((ArrayList<AgreementRegistrationParam>) instance.getAgreements()).add(aValue);

		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T withCollectComments(boolean aValue)
	{
		instance.setCollectComments(aValue);

		return (T) this;
	}
	
	@SuppressWarnings("unchecked")
	public T withFormInformation(I18nString aValue)
	{
		instance.setFormInformation(aValue);

		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T withDisplayedName(I18nString aValue)
	{
		instance.setDisplayedName(aValue);

		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T withName(String aValue)
	{
		instance.setName(aValue);

		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T withDescription(String aValue)
	{
		instance.setDescription(aValue);

		return (T) this;
	}
	
	
	@SuppressWarnings("unchecked")
	public T withPageTitle(I18nString aValue)
	{
		instance.setPageTitle(aValue);
		return (T)this;
	}
	
	@SuppressWarnings("unchecked")
	public T withWrapUpConfig(List<RegistrationWrapUpConfig> config)
	{
		instance.setWrapUpConfig(config);
		return (T)this;
	}
	
	
	@SuppressWarnings("unchecked")
	public T withFormLayoutSettings(FormLayoutSettings settings)
	{
		instance.setLayoutSettings(settings);
		return (T)this;
	}
	
	public class GroupRegistrationParamBuilder
	{
		private GroupRegistrationParam instance;
		private T parent;

		protected GroupRegistrationParamBuilder(GroupRegistrationParam aInstance,
				T parent)
		{
			instance = aInstance;
			this.parent = parent;
		}

		protected GroupRegistrationParam getInstance()
		{
			return instance;
		}

		public GroupRegistrationParamBuilder withGroupPath(String aValue)
		{
			instance.setGroupPath(aValue);

			return this;
		}

		public GroupRegistrationParamBuilder withLabel(String aValue)
		{
			instance.setLabel(aValue);

			return this;
		}

		public GroupRegistrationParamBuilder withDescription(String aValue)
		{
			instance.setDescription(aValue);

			return this;
		}

		public GroupRegistrationParamBuilder withRetrievalSettings(ParameterRetrievalSettings aValue)
		{
			instance.setRetrievalSettings(aValue);

			return this;
		}

		public GroupRegistrationParamBuilder withMultiselect(boolean aValue)
		{
			instance.setMultiSelect(aValue);

			return this;
		}
		
		public T endGroupParam()
		{
			return parent;
		}
	}

	public class AttributeRegistrationParamBuilder
	{
		private AttributeRegistrationParam instance;
		private T parent;

		protected AttributeRegistrationParamBuilder(AttributeRegistrationParam aInstance,
				T parent)
		{
			instance = aInstance;
			this.parent = parent;
		}

		protected AttributeRegistrationParam getInstance()
		{
			return instance;
		}

		public AttributeRegistrationParamBuilder withAttributeType(String aValue)
		{
			instance.setAttributeType(aValue);
			return this;
		}

		public AttributeRegistrationParamBuilder withGroup(String aValue)
		{
			instance.setGroup(aValue);

			return this;
		}

		public AttributeRegistrationParamBuilder withShowGroups(boolean aValue)
		{
			instance.setShowGroups(aValue);

			return this;
		}

		public AttributeRegistrationParamBuilder withOptional(boolean aValue)
		{
			instance.setOptional(aValue);

			return this;
		}

		public AttributeRegistrationParamBuilder withLabel(String aValue)
		{
			instance.setLabel(aValue);

			return this;
		}

		public AttributeRegistrationParamBuilder withDescription(String aValue)
		{
			instance.setDescription(aValue);

			return this;
		}

		public AttributeRegistrationParamBuilder withRetrievalSettings(ParameterRetrievalSettings aValue)
		{
			instance.setRetrievalSettings(aValue);

			return this;
		}

		public AttributeRegistrationParamBuilder withConfirmationMode(ConfirmationMode confirmationMode)
		{
			instance.setConfirmationMode(confirmationMode);
			return this;
		}
		
		public T endAttributeParam()
		{
			return parent;
		}
	}

	public class IdentityRegistrationParamBuilder
	{
		private IdentityRegistrationParam instance;
		private T parent;

		protected IdentityRegistrationParamBuilder(IdentityRegistrationParam aInstance, T parent)
		{
			instance = aInstance;
			this.parent = parent;
		}

		protected IdentityRegistrationParam getInstance()
		{
			return instance;
		}

		public IdentityRegistrationParamBuilder withIdentityType(String aValue)
		{
			instance.setIdentityType(aValue);

			return this;
		}

		public IdentityRegistrationParamBuilder withOptional(boolean aValue)
		{
			instance.setOptional(aValue);

			return this;
		}

		public IdentityRegistrationParamBuilder withLabel(String aValue)
		{
			instance.setLabel(aValue);

			return this;
		}

		public IdentityRegistrationParamBuilder withDescription(String aValue)
		{
			instance.setDescription(aValue);

			return this;
		}

		public IdentityRegistrationParamBuilder withRetrievalSettings(ParameterRetrievalSettings aValue)
		{
			instance.setRetrievalSettings(aValue);

			return this;
		}
		
		public IdentityRegistrationParamBuilder withConfirmationMode(ConfirmationMode confirmationMode)
		{
			instance.setConfirmationMode(confirmationMode);
			return this;
		}

		public T endIdentityParam()
		{
			return parent;
		}
	}
}
