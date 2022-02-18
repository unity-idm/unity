/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.scheme;

import java.util.List;

import io.imunity.scim.config.AttributeDefinition;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.SchemaType;
import io.imunity.scim.config.SchemaWithMapping;

//TODO UY-1219 Fill User and Group schema with default mappings. Add this defaults to newly created SCIM endpoint config. 
public class DefaultSchemaProvider
{
	public static final String DEFAULT_USER_SCHEMA_ID = "urn:ietf:params:scim:schemas:core:2.0:User";
	public static final String DEFAULT_GROUP_SCHEMA_ID = "urn:ietf:params:scim:schemas:core:2.0:Group";

	public static SchemaWithMapping getBasicUserSchema()
	{
		return SchemaWithMapping.builder().withId(DEFAULT_USER_SCHEMA_ID).withType(SchemaType.USER_CORE).withName("User")
				.withDescription("User Schema").withEnable(true)
				.withAttributesWithMapping(List.of(
						AttributeDefinitionWithMapping.builder().withAttributeDefinition(AttributeDefinition.builder()
								.withName("userName").withMultiValued(false).withType(SCIMAttributeType.STRING)
								.withDescription(
										"Unique identifier for the User, typically used by the user to directly"
												+ " authenticate to the service provider. Each User MUST include a non-empty userName value."
												+ " This identifier MUST be unique across the service provider's entire set of Users.")
								.build()).withAttributeMapping(null).build(),
						AttributeDefinitionWithMapping.builder().withAttributeDefinition(AttributeDefinition.builder()
								.withName("name").withMultiValued(false).withType(SCIMAttributeType.COMPLEX)
								.withDescription(
										"The components of the user's real name. Providers MAY return just the full name as a single string in"
												+ " the formatted sub-attribute, or they MAY return just the individual component attributes using the "
												+ "other sub-attributes, or they MAY return both.  If both variants are returned, they SHOULD be describing"
												+ " the same name, with the formatted name indicating how the component attributes should be combined.")
								.withSubAttributesWithMapping(List.of(AttributeDefinitionWithMapping.builder()
										.withAttributeDefinition(AttributeDefinition.builder().withName("formatted")
												.withMultiValued(false).withType(SCIMAttributeType.STRING)
												.withDescription(
														"The full name, including all middle names, titles, and suffixes as appropriate, "
																+ "formatted for display (e.g., 'Ms. Barbara J Jensen, III').")
												.build())
										.withAttributeMapping(null).build(),
										AttributeDefinitionWithMapping.builder()
												.withAttributeDefinition(AttributeDefinition.builder()
														.withName("familyName").withMultiValued(false)
														.withType(SCIMAttributeType.STRING)
														.withDescription(
																"The family name of the User, or last name in most Western languages"
																		+ " (e.g., 'Jensen' given the full name 'Ms. Barbara J Jensen, III').")
														.build())
												.withAttributeMapping(null).build(),
										AttributeDefinitionWithMapping.builder()
												.withAttributeDefinition(AttributeDefinition.builder()
														.withName("givenName").withMultiValued(false)
														.withType(SCIMAttributeType.STRING)
														.withDescription(
																"The given name of the User, or first name in most Western languages (e.g., 'Barbara' given the full name 'Ms. Barbara J Jensen, III').")
														.build())
												.withAttributeMapping(null).build(),
										AttributeDefinitionWithMapping.builder()
												.withAttributeDefinition(AttributeDefinition.builder()
														.withName("middleName").withMultiValued(false)
														.withType(SCIMAttributeType.STRING)
														.withDescription(
																"The middle name(s) of the User (e.g., 'Jane' given the full name 'Ms. Barbara J Jensen, III').")
														.build())
												.withAttributeMapping(null).build(),
										AttributeDefinitionWithMapping.builder()
												.withAttributeDefinition(AttributeDefinition.builder()
														.withName("honorificPrefix").withMultiValued(false)
														.withType(SCIMAttributeType.STRING)
														.withDescription(
																"The honorific prefix(es) of the User, or title in most Western languages (e.g., 'Ms.' given the full name 'Ms. Barbara J Jensen, III').")
														.build())
												.withAttributeMapping(null).build(),
										AttributeDefinitionWithMapping.builder()
												.withAttributeDefinition(AttributeDefinition.builder()
														.withName("honorificSuffix").withMultiValued(false)
														.withType(SCIMAttributeType.STRING)
														.withDescription(
																"The honorific suffix(es) of the User, or suffix in most Western languages (e.g., 'III' given the full name 'Ms. Barbara J Jensen, III').")
														.build())
												.withAttributeMapping(null).build()))
								.build()).withAttributeMapping(null).build(),
						AttributeDefinitionWithMapping.builder().withAttributeDefinition(AttributeDefinition.builder()
								.withName("displayName").withMultiValued(false).withType(SCIMAttributeType.STRING)
								.withDescription(
										"The name of the User, suitable for display to end-users.  The name SHOULD be the full name of the User being described, if known.")
								.build()).withAttributeMapping(null).build(),
						AttributeDefinitionWithMapping.builder().withAttributeDefinition(AttributeDefinition.builder()
								.withName("nickName").withMultiValued(false).withType(SCIMAttributeType.STRING)
								.withDescription(
										"The casual way to address the user in real life, e.g., 'Bob' or 'Bobby' instead of 'Robert'.  This attribute SHOULD NOT be used to represent a User's username (e.g., 'bjensen' or 'mpepperidge'")
								.build()).withAttributeMapping(null).build(),
						AttributeDefinitionWithMapping.builder().withAttributeDefinition(AttributeDefinition.builder()
								.withName("profileUrl").withMultiValued(false).withType(SCIMAttributeType.REFERENCE)
								.withDescription(
										"A fully qualified URL pointing to a page representing the User's online profile.")
								.build()).withAttributeMapping(null).build(),
						AttributeDefinitionWithMapping.builder()
								.withAttributeDefinition(AttributeDefinition.builder().withName("title")
										.withMultiValued(false).withType(SCIMAttributeType.STRING)
										.withDescription("The user's title, such as 'Vice President.'").build())
								.withAttributeMapping(null).build(),
						AttributeDefinitionWithMapping.builder().withAttributeDefinition(AttributeDefinition.builder()
								.withName("userType").withMultiValued(false).withType(SCIMAttributeType.STRING)
								.withDescription(
										"Used to identify the relationship between the organization and the user. Typical values used might be 'Contractor', 'Employee', 'Intern', 'Temp', 'External', and 'Unknown', but any value may be used.")
								.build()).withAttributeMapping(null).build(),
						AttributeDefinitionWithMapping.builder().withAttributeDefinition(AttributeDefinition.builder()
								.withName("preferredLanguage").withMultiValued(false).withType(SCIMAttributeType.STRING)
								.withDescription(
										"Indicates the User's preferred written or spoken language.  Generally used for selecting a localized user interface; e.g., 'en_US' specifies the language English and country US.")
								.build()).withAttributeMapping(null).build(),
						AttributeDefinitionWithMapping.builder().withAttributeDefinition(AttributeDefinition.builder()
								.withName("locale").withMultiValued(false).withType(SCIMAttributeType.STRING)
								.withDescription(
										"Used to indicate the User's default location for purposes of localizing items such as currency, date time format, or numerical representations.")
								.build()).withAttributeMapping(null).build(),
						AttributeDefinitionWithMapping.builder().withAttributeDefinition(AttributeDefinition.builder()
								.withName("timezone").withMultiValued(false).withType(SCIMAttributeType.STRING)
								.withDescription(
										"The User's time zone in the 'Olson' time zone database format, e.g., 'America/Los_Angeles'.")
								.build()).withAttributeMapping(null).build(),
						AttributeDefinitionWithMapping.builder()
								.withAttributeDefinition(AttributeDefinition.builder().withName("active")
										.withMultiValued(false).withType(SCIMAttributeType.BOOLEAN)
										.withDescription("A Boolean value indicating the User's administrative status.")
										.build())
								.withAttributeMapping(null).build(),
						AttributeDefinitionWithMapping.builder().withAttributeDefinition(AttributeDefinition.builder()
								.withName("password").withMultiValued(false).withType(SCIMAttributeType.STRING)
								.withDescription(
										"The User's cleartext password. This attribute is intended to be used as a means to specify an initial password when creating a new User or to reset an existing User's password.")
								.build()).withAttributeMapping(null).build()

				)).build();
	}

	public static SchemaWithMapping getBasicGroupSchema()
	{
		return SchemaWithMapping.builder().withId(DEFAULT_GROUP_SCHEMA_ID).withType(SchemaType.GROUP_CORE)
				.withName("Group").withDescription("Group Schema").withEnable(true)
				.withAttributesWithMapping(List.of(
						AttributeDefinitionWithMapping.builder()
								.withAttributeDefinition(AttributeDefinition.builder().withName("displayName")
										.withMultiValued(false).withType(SCIMAttributeType.STRING)
										.withDescription("A human-readable name for the Group.").build())
								.withAttributeMapping(null).build(),

						AttributeDefinitionWithMapping.builder().withAttributeDefinition(AttributeDefinition.builder()
								.withName("groupType").withMultiValued(false).withType(SCIMAttributeType.STRING)
								.withDescription(
										"Used to identify the relationship between the organization and the group."
												+ " Typical values used might be 'Organization', 'Site', 'Team', but any value may be used.")
								.build()).withAttributeMapping(null).build(),

						AttributeDefinitionWithMapping.builder().withAttributeDefinition(AttributeDefinition.builder()
								.withName("members").withMultiValued(false).withType(SCIMAttributeType.COMPLEX)
								.withDescription("A list of members of the Group.")
								.withSubAttributesWithMapping(List.of(
										AttributeDefinitionWithMapping.builder().withAttributeDefinition(
												AttributeDefinition.builder().withName("value").withMultiValued(false)
														.withType(SCIMAttributeType.STRING).withDescription(
																"Identifier of the member of this Group.")
														.build())
												.withAttributeMapping(null).build(),
										AttributeDefinitionWithMapping.builder()
												.withAttributeDefinition(AttributeDefinition.builder().withName("$ref")
														.withMultiValued(false).withType(SCIMAttributeType.REFERENCE)
														.withDescription(
																"The URI corresponding to a SCIM resource that is a member of this Group.")
														.build())
												.withAttributeMapping(null).build(),
										AttributeDefinitionWithMapping.builder()
												.withAttributeDefinition(AttributeDefinition.builder().withName("type")
														.withMultiValued(false).withType(SCIMAttributeType.STRING)
														.withDescription(
																"A label indicating the type of resource, e.g., 'User' or 'Group'.")
														.build())
												.withAttributeMapping(null).build()))
								.build()).build()))
				.build();
	}

}
