/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;


/**
 * Builder of {@link BaseRegistrationInput}.
 */
public class BaseRegistrationInputBuilder<T extends BaseRegistrationInput, 
	GeneratorT extends BaseRegistrationInputBuilder<?, ?>>
{
	protected T instance;

	protected BaseRegistrationInputBuilder(T aInstance)
	{
		instance = aInstance;
	}

	public T build()
	{
		instance.validate();
		return instance;
	}

	@SuppressWarnings("unchecked")
	public GeneratorT withFormId(String aValue)
	{
		instance.setFormId(aValue);

		return (GeneratorT) this;
	}

	@SuppressWarnings("unchecked")
	public GeneratorT withIdentities(List<IdentityParam> aValue)
	{
		instance.setIdentities(aValue);

		return (GeneratorT) this;
	}
	
	@SuppressWarnings("unchecked")
	public GeneratorT withAddedIdentity(IdentityParam aValue)
	{
		if (instance.getIdentities() == null)
		{
			instance.setIdentities(new ArrayList<IdentityParam>());
		}

		((ArrayList<IdentityParam>) instance.getIdentities()).add(aValue);

		return (GeneratorT) this;
	}

	public AddedIdentityIdentityParamBuilder withAddedIdentity(String type, String value)
	{
		IdentityParam obj = new IdentityParam(type, value);

		withAddedIdentity(obj);

		return new AddedIdentityIdentityParamBuilder(obj);
	}

	@SuppressWarnings("unchecked")
	public GeneratorT withAttributes(List<Attribute> aValue)
	{
		instance.setAttributes(aValue);

		return (GeneratorT) this;
	}

	@SuppressWarnings("unchecked")
	public GeneratorT withAddedAttribute(Attribute aValue)
	{
		if (instance.getAttributes() == null)
		{
			instance.setAttributes(new ArrayList<Attribute>());
		}

		((ArrayList<Attribute>) instance.getAttributes()).add(aValue);

		return (GeneratorT) this;
	}

	@SuppressWarnings("unchecked")
	public GeneratorT withCredentials(List<CredentialParamValue> aValue)
	{
		instance.setCredentials(aValue);

		return (GeneratorT) this;
	}

	@SuppressWarnings("unchecked")
	public GeneratorT withAddedCredential(CredentialParamValue aValue)
	{
		if (instance.getCredentials() == null)
		{
			instance.setCredentials(new ArrayList<CredentialParamValue>());
		}

		((ArrayList<CredentialParamValue>) instance.getCredentials()).add(aValue);

		return (GeneratorT) this;
	}

	public AddedCredentialCredentialParamValueBuilder withAddedCredential()
	{
		CredentialParamValue obj = new CredentialParamValue();

		withAddedCredential(obj);

		return new AddedCredentialCredentialParamValueBuilder(obj);
	}

	@SuppressWarnings("unchecked")
	public GeneratorT withGroupSelections(List<GroupSelection> aValue)
	{
		instance.setGroupSelections(aValue);

		return (GeneratorT) this;
	}

	@SuppressWarnings("unchecked")
	public GeneratorT withAddedGroupSelection(GroupSelection aValue)
	{
		if (instance.getGroupSelections() == null)
			instance.setGroupSelections(new ArrayList<>());

		instance.addGroupSelection(aValue);

		return (GeneratorT) this;
	}

	public AddedGroupSelectionSelectionBuilder withAddedGroupSelection()
	{
		GroupSelection obj = new GroupSelection();

		withAddedGroupSelection(obj);

		return new AddedGroupSelectionSelectionBuilder(obj);
	}
	
	@SuppressWarnings("unchecked")
	public GeneratorT withAgreements(List<Selection> aValue)
	{
		instance.setAgreements(aValue);

		return (GeneratorT) this;
	}

	@SuppressWarnings("unchecked")
	public GeneratorT withAddedAgreement(Selection aValue)
	{
		if (instance.getAgreements() == null)
		{
			instance.setAgreements(new ArrayList<Selection>());
		}

		((ArrayList<Selection>) instance.getAgreements()).add(aValue);

		return (GeneratorT) this;
	}

	public AddedAgreementSelectionBuilder withAddedAgreement()
	{
		Selection obj = new Selection();

		withAddedAgreement(obj);

		return new AddedAgreementSelectionBuilder(obj);
	}

	@SuppressWarnings("unchecked")
	public GeneratorT withComments(String aValue)
	{
		instance.setComments(aValue);

		return (GeneratorT) this;
	}

	@SuppressWarnings("unchecked")
	public GeneratorT withUserLocale(String aValue)
	{
		instance.setUserLocale(aValue);

		return (GeneratorT) this;
	}
	
	@SuppressWarnings("unchecked")
	public GeneratorT withRegistrationCode(String aValue)
	{
		instance.setRegistrationCode(aValue);
		
		return (GeneratorT) this;
	}

	public class AddedIdentityIdentityParamBuilder extends
			IdentityParamBuilderBase<AddedIdentityIdentityParamBuilder>
	{
		public AddedIdentityIdentityParamBuilder(IdentityParam aInstance)
		{
			super(aInstance);
		}

		@SuppressWarnings("unchecked")
		public GeneratorT endIdentity()
		{
			return (GeneratorT) BaseRegistrationInputBuilder.this;
		}
	}

	public class AddedCredentialCredentialParamValueBuilder extends
			CredentialParamValueBuilderBase<AddedCredentialCredentialParamValueBuilder>
	{
		public AddedCredentialCredentialParamValueBuilder(CredentialParamValue aInstance)
		{
			super(aInstance);
		}

		@SuppressWarnings("unchecked")
		public GeneratorT endCredential()
		{
			return (GeneratorT) BaseRegistrationInputBuilder.this;
		}
	}

	public class AddedGroupSelectionSelectionBuilder
	{
		private GroupSelection instance;

		protected AddedGroupSelectionSelectionBuilder(GroupSelection aInstance)
		{
			instance = aInstance;
		}

		protected GroupSelection getInstance()
		{
			return instance;
		}

		public AddedGroupSelectionSelectionBuilder withGroup(String group)
		{
			List<String> groups = instance.getSelectedGroups();
			groups.add(group);
			instance.setSelectedGroups(groups);
			return this;
		}
		
		@SuppressWarnings("unchecked")
		public GeneratorT endGroupSelection()
		{
			return (GeneratorT) BaseRegistrationInputBuilder.this;
		}
	}

	public class AddedAgreementSelectionBuilder extends
			SelectionBuilderBase<AddedAgreementSelectionBuilder>
	{
		public AddedAgreementSelectionBuilder(Selection aInstance)
		{
			super(aInstance);
		}

		@SuppressWarnings("unchecked")
		public GeneratorT endAgreement()
		{
			return (GeneratorT) BaseRegistrationInputBuilder.this;
		}
	}

	public static class IdentityParamBuilderBase<GeneratorT extends IdentityParamBuilderBase<GeneratorT>>
	{
		private IdentityParam instance;

		protected IdentityParamBuilderBase(IdentityParam aInstance)
		{
			instance = aInstance;
		}

		protected IdentityParam getInstance()
		{
			return instance;
		}

		@SuppressWarnings("unchecked")
		public GeneratorT withTranslationProfile(String aValue)
		{
			instance.setTranslationProfile(aValue);

			return (GeneratorT) this;
		}

		@SuppressWarnings("unchecked")
		public GeneratorT withRemoteIdp(String aValue)
		{
			instance.setRemoteIdp(aValue);

			return (GeneratorT) this;
		}

		@SuppressWarnings("unchecked")
		public GeneratorT withConfirmationInfo(ConfirmationInfo aValue)
		{
			instance.setConfirmationInfo(aValue);

			return (GeneratorT) this;
		}

		@SuppressWarnings("unchecked")
		public GeneratorT withTypeId(String aValue)
		{
			instance.setTypeId(aValue);

			return (GeneratorT) this;
		}

		@SuppressWarnings("unchecked")
		public GeneratorT withValue(String aValue)
		{
			instance.setValue(aValue);

			return (GeneratorT) this;
		}

		@SuppressWarnings("unchecked")
		public GeneratorT withTarget(String aValue)
		{
			instance.setTarget(aValue);

			return (GeneratorT) this;
		}

		@SuppressWarnings("unchecked")
		public GeneratorT withRealm(String aValue)
		{
			instance.setRealm(aValue);

			return (GeneratorT) this;
		}
	}

	public static class SelectionBuilderBase<GeneratorT extends SelectionBuilderBase<GeneratorT>>
	{
		private Selection instance;

		protected SelectionBuilderBase(Selection aInstance)
		{
			instance = aInstance;
		}

		protected Selection getInstance()
		{
			return instance;
		}

		@SuppressWarnings("unchecked")
		public GeneratorT withSelected(boolean aValue)
		{
			instance.setSelected(aValue);

			return (GeneratorT) this;
		}

		@SuppressWarnings("unchecked")
		public GeneratorT withExternalIdp(String aValue)
		{
			instance.setExternalIdp(aValue);

			return (GeneratorT) this;
		}
	}

	public static class CredentialParamValueBuilderBase<GeneratorT extends CredentialParamValueBuilderBase<GeneratorT>>
	{
		private CredentialParamValue instance;

		protected CredentialParamValueBuilderBase(CredentialParamValue aInstance)
		{
			instance = aInstance;
		}

		protected CredentialParamValue getInstance()
		{
			return instance;
		}

		@SuppressWarnings("unchecked")
		public GeneratorT withCredentialId(String aValue)
		{
			instance.setCredentialId(aValue);

			return (GeneratorT) this;
		}

		@SuppressWarnings("unchecked")
		public GeneratorT withSecrets(String aValue)
		{
			instance.setSecrets(aValue);

			return (GeneratorT) this;
		}
	}
}
