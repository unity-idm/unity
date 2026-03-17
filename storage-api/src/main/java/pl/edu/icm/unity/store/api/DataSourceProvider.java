package pl.edu.icm.unity.store.api;

import javax.sql.DataSource;

/**
 * Provides low level JDBC DataSource. Use with care: it skips all safety of persistence framework,
 * protection against SQL injection, transactions, etc.
 */
public interface DataSourceProvider
{
	DataSource getDataSource();
}
