package de.alksa.classifier.impl;

import java.util.Objects;
import java.util.Set;

import de.alksa.checker.QueryChecker;
import de.alksa.log.LogEntry;
import de.alksa.log.Logger;
import de.alksa.querystorage.Query;
import de.alksa.querystorage.QueryStorage;
import de.alksa.token.SelectStatementToken;
import de.alksa.util.TypeUtil;

class ProductiveClassifier extends ClassifierState {

	private QueryStorage queryStorage;
	private Logger logger;
	private QueryChecker masterChecker;

	public ProductiveClassifier(Set<QueryChecker> queryCheckers,
			QueryStorage queryStorage, Logger logger) {
		Objects.requireNonNull(queryCheckers);
		Objects.requireNonNull(queryStorage);
		Objects.requireNonNull(logger);

		masterChecker = createDummyChecker();

		for (QueryChecker checker : queryCheckers) {
			masterChecker.appendMatcher(checker);
		}

		this.queryStorage = queryStorage;
		this.logger = logger;
	}

	@Override
	protected boolean acceptSingleSelectStatementQuery(Query query) {
		Objects.requireNonNull(query);

		LogEntry log = checkSingleSelectQuery(query);

		if (log != null) {
			logger.write(log);
			return false;
		}

		return true;
	}

	/**
	 * Checks the subject against the learned queries and decides, if it is
	 * allowed or not.
	 *
	 * @return null if the query is allowed. If it is disallowed a LogEntry is
	 *         created and returned.
	 */
	private LogEntry checkSingleSelectQuery(Query subject) {
		Set<Query> learnedQueries = queryStorage.read();
		LogEntry log = null;

		// quick check for equal queries
		if (learnedQueries.contains(subject)) {
			return null;
		}

		SelectStatementToken subjectSelect = TypeUtil.getFirstTokenOfType(
				subject.getQuery(), SelectStatementToken.class);
		SelectStatementToken learnedSelect;

		for (Query learned : learnedQueries) {
			// TODO better performance: Use Map structure for storing the
			// queries
			if (isNotSameDatabaseAndUser(subject, learned)) {
				continue;
			}

			learnedSelect = TypeUtil.getFirstTokenOfType(learned.getQuery(),
					SelectStatementToken.class);

			log = masterChecker.checkSubjectAgainstLearned(subjectSelect,
					subject, learnedSelect);

			if (log == null) {
				// subject has found one learned query in database which is
				// similar.
				break;
			}
		}

		return log;
	}

	private boolean isNotSameDatabaseAndUser(Query subject, Query learned) {
		return !(subject.getDatabase().equals(learned.getDatabase()) && subject
				.getDatabaseUser().equals(learned.getDatabaseUser()));
	}

	private QueryChecker createDummyChecker() {
		return new QueryChecker() {

			@Override
			protected LogEntry check(SelectStatementToken subject,
					SelectStatementToken learned) {
				return null;
			}
		};
	}
}
