/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import static io.imunity.console.views.directory_browser.identities.IdentityEntryFixtures.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.imunity.console.views.directory_browser.EntityWithLabel;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Grid rows are kept in selection sets / tree data before their label and attribute values are lazily
 * resolved, so their identity must not depend on these.
 */
class IdentityEntryIdentityTest
{
	@Test
	void rowStaysInSetAfterLabelAndAttributeValuesArePopulated()
	{
		EntityWithLabel entityWithLabel = new EntityWithLabel(entity(1L), null);
		IdentityEntry row = new IdentityEntry(entityWithLabel, mock(MessageSource.class));
		Set<IdentityEntry> selection = new HashSet<>(Set.of(row));
		int hashBefore = row.hashCode();

		entityWithLabel.updateLabel("John ");
		row.putAttributeValue("a::current::email", "john@example.com");

		assertThat(row.hashCode()).isEqualTo(hashBefore);
		assertThat(selection).contains(row);
		assertThat(selection.remove(row)).isTrue();
		assertThat(selection).isEmpty();
	}

	@Test
	void rowsOfSameEntityAreEqualRegardlessOfResolvedValues()
	{
		Entity entity = entity(1L);
		EntityWithLabel resolved = new EntityWithLabel(entity, null);
		IdentityEntry resolvedRow = new IdentityEntry(resolved, mock(MessageSource.class));
		resolved.updateLabel("John ");
		resolvedRow.putAttributeValue("a::current::email", "john@example.com");
		IdentityEntry freshRow = new IdentityEntry(new EntityWithLabel(entity, null), mock(MessageSource.class));

		assertThat(freshRow).isEqualTo(resolvedRow);
		assertThat(freshRow.hashCode()).isEqualTo(resolvedRow.hashCode());
		assertThat(new HashSet<>(Set.of(resolvedRow))).contains(freshRow);
	}

	@Test
	void rowsOfDifferentEntitiesDiffer()
	{
		IdentityEntry row1 = new IdentityEntry(new EntityWithLabel(entity(1L), null), mock(MessageSource.class));
		IdentityEntry row2 = new IdentityEntry(new EntityWithLabel(entity(2L), null), mock(MessageSource.class));

		assertThat(row1).isNotEqualTo(row2);
	}
}
