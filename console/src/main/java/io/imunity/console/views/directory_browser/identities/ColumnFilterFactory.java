/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import java.util.Set;

import io.imunity.console.views.directory_browser.identities.AddFilterDialog.Operand;
import pl.edu.icm.unity.engine.api.attributes.AttributeSearchService;

/**
 * Creates filters of the identities grid columns, as configured in {@link AddFilterDialog}.
 */
class ColumnFilterFactory
{
	@FunctionalInterface
	interface AttributeValueResolver
	{
		/**
		 * @return display value of the given attribute column for the entity, regardless whether its row
		 * was already rendered
		 */
		String resolve(long entityId, String columnId);
	}

	private final AttributeSearchService attributeSearchService;
	private final AttributeValueResolver valueResolver;

	ColumnFilterFactory(AttributeSearchService attributeSearchService, AttributeValueResolver valueResolver)
	{
		this.attributeSearchService = attributeSearchService;
		this.valueResolver = valueResolver;
	}

	EntityFilter create(String columnId, Operand op, String argument, String currentGroupPath)
	{
		return columnId.startsWith(IdentitiesGridColumnConstants.ATTR_COL_PREFIX)
				? new AttributeColumnFilter(attributeSearchService, valueResolver, columnId, op, argument,
						currentGroupPath)
				: baseColumnFilter(columnId, op, argument);
	}

	private static EntityFilter baseColumnFilter(String columnId, Operand op, String argument)
	{
		String argumentLower = argument.toLowerCase();
		EntityFilter baseFilter = isExact(op)
				? ie -> argument.equals(ie.getAnyValue(columnId))
				: ie -> containsIgnoringCase(ie.getAnyValue(columnId), argumentLower);
		return isNegated(op) ? ie -> !baseFilter.test(ie) : baseFilter;
	}

	private static boolean containsIgnoringCase(String value, String searchedLower)
	{
		return value != null && value.toLowerCase().contains(searchedLower);
	}

	private static boolean isExact(Operand op)
	{
		return op == Operand.equal || op == Operand.notEqual;
	}

	private static boolean isNegated(Operand op)
	{
		return op == Operand.notEqual || op == Operand.notContain;
	}

	/*
	 * Attribute values are not bulk pre-loaded (UY-1483): a row not yet rendered has no in-memory value to
	 * test against, so filtering goes through the DB-backed AttributeSearchService, scoped to the column's
	 * attribute - "contain"/"notContain" map directly to its substring search; "equal"/"notEqual" narrow that
	 * (necessarily superset) match down with an exact check, resolved on demand for just the DB candidates.
	 * The DB matches are recomputed on each grid reload, as the data or the browsed group could change.
	 */
	private static class AttributeColumnFilter implements EntityFilter
	{
		private final AttributeSearchService attributeSearchService;
		private final AttributeValueResolver valueResolver;
		private final String columnId;
		private final String attributeName;
		private final boolean rootColumn;
		private final Operand op;
		private final String argument;
		private volatile Set<Long> dbMatches;

		AttributeColumnFilter(AttributeSearchService attributeSearchService, AttributeValueResolver valueResolver,
				String columnId, Operand op, String argument, String currentGroupPath)
		{
			this.attributeSearchService = attributeSearchService;
			this.valueResolver = valueResolver;
			this.columnId = columnId;
			this.attributeName = IdentitiesGridColumnConstants.getAttributeName(columnId);
			this.rootColumn = IdentitiesGridColumnConstants.isRootAttributeColumn(columnId);
			this.op = op;
			this.argument = argument;
			refresh(currentGroupPath);
		}

		@Override
		public void refresh(String groupPath)
		{
			dbMatches = attributeSearchService.searchEntities(rootColumn ? "/" : groupPath, attributeName,
					argument);
		}

		@Override
		public boolean test(IdentityEntry ie)
		{
			return isNegated(op) != matches(ie.getSourceEntity().getEntity().getId());
		}

		private boolean matches(long entityId)
		{
			if (!dbMatches.contains(entityId))
				return false;
			return !isExact(op) || argument.equals(valueResolver.resolve(entityId, columnId));
		}
	}
}
