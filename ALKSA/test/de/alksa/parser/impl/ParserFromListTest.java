package de.alksa.parser.impl;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import de.alksa.token.ColumnNameToken;
import de.alksa.token.ComparisonFilterToken;
import de.alksa.token.FilterToken;
import de.alksa.token.FromListToken;
import de.alksa.token.JoinToken;
import de.alksa.token.SelectStatementToken;
import de.alksa.token.TableNameToken;
import de.alksa.token.Token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ParserFromListTest {

	private VisitorBasedParser parser;

	@Before
	public void setUp() throws Exception {
		parser = new VisitorBasedParser();
	}

	@Test
	public void testTableName() {
		String sql = "SELECT c1 FROM users, depart";
		Set<TableNameToken> expected = new HashSet<>();
		Set<? extends Token> actual;
		boolean tokenExists = false;

		expected.add(new TableNameToken("users"));
		expected.add(new TableNameToken("depart"));

		Set<Token> parsedTokens = parser.parse(sql);

		Set<? extends Token> tokens = ((SelectStatementToken) parsedTokens
				.iterator().next()).getChildren();

		// otherwise loop could be skipped
		assertTrue(tokens.size() > 0);

		for (Token token : tokens) {
			if (token instanceof FromListToken) {
				actual = ((FromListToken) token).getChildren();

				assertEquals(expected.size(), actual.size());
				assertTrue(actual.containsAll(expected));

				tokenExists = true;
			}
		}

		if (!tokenExists) {
			fail("No FromListToken found");
		}
	}

	@Test
	public void testTableNameAlias() {
		String sql = "SELECT c1 FROM users u, ignore1 CROSS JOIN ignore2";
		Set<TableNameToken> expected = new HashSet<>();
		Set<? extends Token> actual;
		boolean tokenExists = false;

		expected.add(new TableNameToken("users"));

		Set<Token> parsedTokens = parser.parse(sql);

		Set<? extends Token> tokens = ((SelectStatementToken) parsedTokens
				.iterator().next()).getChildren();

		// otherwise loop could be skipped
		assertTrue(tokens.size() > 0);

		for (Token token : tokens) {
			if (token instanceof FromListToken) {
				actual = ((FromListToken) token).getChildren();

				// minus 1 because of the ignored join
				assertEquals(expected.size(), actual.size() - 1);
				assertTrue(actual.containsAll(expected));

				tokenExists = true;
			}
		}

		if (!tokenExists) {
			fail("No FromListToken found");
		}
	}

	@Test
	public void testSimpleJoin() {
		FilterToken onClause = new ComparisonFilterToken(new ColumnNameToken(
				"col1"), ComparisonFilterToken.Type.EQUAL, new ColumnNameToken(
						"col2"));
		String onString = "on col1 = col2";
		testJoinType(JoinToken.Type.INNER, onClause, onString);
		testJoinType(JoinToken.Type.NATURAL, null, "");
		testJoinType(JoinToken.Type.LEFT_OUTER, onClause, onString);
		testJoinType(JoinToken.Type.RIGHT_OUTER, onClause, onString);
		testJoinType(JoinToken.Type.FULL_OUTER, onClause, onString);
	}

	private void testJoinType(JoinToken.Type joinType, FilterToken onClause,
			String onClauseString) {
		String sql = "SELECT c1 FROM users " + joinType + " JOIN departments";
		boolean tokenExists = false;

		if (onClause != null) {
			sql += " " + onClauseString;
		}

		JoinToken expected = new JoinToken(new TableNameToken("users"),
				joinType, new TableNameToken("departments"));

		expected.setOnClause(onClause);

		Set<Token> parsedTokens = parser.parse(sql);

		Set<? extends Token> tokens = ((SelectStatementToken) parsedTokens
				.iterator().next()).getChildren();

		// otherwise loop could be skipped
		assertTrue(tokens.size() > 0);

		for (Token token : tokens) {
			if (token instanceof FromListToken) {
				Token actual = ((FromListToken) token).getChildren().iterator()
						.next();

				assertEquals(expected, actual);

				tokenExists = true;
			}
		}

		if (!tokenExists) {
			fail("No FromListToken found");
		}
	}

	@Test
	public void testMultiJoin() {
		// (ll LEFT lr) RIGHT rr
		String sql = "SELECT c1 FROM ll LEFT OUTER JOIN lr on c1 = c2 RIGHT OUTER JOIN rr on c3 = c4";
		boolean tokenExists = false;

		JoinToken leftJoin = new JoinToken(new TableNameToken("ll"),
				JoinToken.Type.LEFT_OUTER, new TableNameToken("lr"));
		FilterToken leftJoinOnClause = new ComparisonFilterToken(
				new ColumnNameToken("c1"), ComparisonFilterToken.Type.EQUAL,
				new ColumnNameToken("c2"));
		leftJoin.setOnClause(leftJoinOnClause);

		// top level join
		JoinToken rightJoin = new JoinToken(leftJoin,
				JoinToken.Type.RIGHT_OUTER, new TableNameToken("rr"));
		FilterToken rightJoinOnClause = new ComparisonFilterToken(
				new ColumnNameToken("c3"), ComparisonFilterToken.Type.EQUAL,
				new ColumnNameToken("c4"));
		rightJoin.setOnClause(rightJoinOnClause);

		JoinToken expected = rightJoin;

		Set<Token> parsedTokens = parser.parse(sql);

		Set<? extends Token> tokens = ((SelectStatementToken) parsedTokens
				.iterator().next()).getChildren();

		// otherwise loop could be skipped
		assertTrue(tokens.size() > 0);

		for (Token token : tokens) {
			if (token instanceof FromListToken) {
				Token actual = ((FromListToken) token).getChildren().iterator()
						.next();

				assertEquals(expected, actual);

				tokenExists = true;
			}
		}

		if (!tokenExists) {
			fail("No FromListToken found");
		}
	}
}
