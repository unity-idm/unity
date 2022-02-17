/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.scheme;

import java.util.List;

import io.imunity.scim.config.AttributeDefinition;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.SchemaWithMapping;

//TODO Fill User and Group schema with default mappings. Add this defaults to newly created SCIM endpoint config. 
public class DefaultSchemaProvider
{
	public static final String DEFAULT_USER_SCHEMA_ID = "urn:ietf:params:scim:schemas:core:2.0:User";
	public static final String DEFAULT_GROUP_SCHEMA_ID = "urn:ietf:params:scim:schemas:core:2.0:Group";

	public static boolean isDefaultSchema(String schema)
	{
		return schema != null && (schema.equals(DefaultSchemaProvider.DEFAULT_GROUP_SCHEMA_ID)
				|| schema.equals(DefaultSchemaProvider.DEFAULT_USER_SCHEMA_ID));
	}

	public static SchemaWithMapping getBasicUserSchema()
	{
		return SchemaWithMapping.builder().withId(DEFAULT_USER_SCHEMA_ID).withName("User")
				.withDescription("User Schema")
				.withAttributesWithMapping(List.of(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("userName")
								.withMultiValued(false).withType(SCIMAttributeType.STRING).withDescription(
										"Unique identifier for the User, typically used by the user to directly"
												+ " authenticate to the service provider. Each User MUST include a non-empty userName value."
												+ " This identifier MUST be unique across the service provider's entire set of Users.")
								.build())
						.withAttributeMapping(null).build(),
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
										.withAttributeMapping(null).build()))
								.build()).withAttributeMapping(null).build()))
				.build();
	}

	public static SchemaWithMapping getBasicGroupSchema()
	{
		return SchemaWithMapping.builder().withId(DEFAULT_GROUP_SCHEMA_ID).withName("Group")
				.withDescription("Group Schema")
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
