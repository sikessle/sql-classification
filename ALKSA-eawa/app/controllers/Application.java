package controllers;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.alksa.ALKSA;
import de.alksa.ALKSAInvalidQueryException;

public class Application extends Controller {

	private static ObjectMapper mapper = new ObjectMapper();

	private static ALKSA alksa;
	private static boolean enableALKSA;

	private static List<String> learnedQueries;
	private static List<String> productiveQueries;

	static {
		init();
	}

	private static void init() {
		alksa = ALKSASingleton.getInstance();
		enableALKSA = false;
		learnedQueries = new LinkedList<>();
		productiveQueries = new LinkedList<>();
	}

	public static Result reset() {
		init();
		return ok();
	}

	public static Result index() {
		return ok(index.render());
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result runQuery() {

		JsonNode request = request().body().asJson();

		if (request == null) {
			return badRequest("expecting application/json");
		}

		String columns = request.get("columns").asText();
		String where = request.get("where").asText();

		ObjectNode result = createResultFromQuery(buildQuery(columns, where));

		return ok(result);
	}

	private static String buildQuery(String columns, String where) {
		String query = "SELECT " + ("".equals(columns) ? "*" : columns);
		query += " FROM City";
		query += " LEFT OUTER JOIN Country ON City.CountryCode = Country.Code";
		query += " LEFT OUTER JOIN CountryLanguage ON CountryLanguage.CountryCode = Country.Code";

		if (!"".equals(where)) {
			query += " WHERE " + where;
		}

		return query;
	}

	private static ObjectNode createResultFromQuery(String query) {
		ObjectNode result = mapper.createObjectNode();
		ArrayNode resultHead = mapper.createArrayNode();
		ArrayNode resultBody = mapper.createArrayNode();

		// TODO
		boolean accepted = processQuery(query);

		result.put("learnedQueries", getQueriesAsJSON(learnedQueries));
		result.put("productiveQueries", getQueriesAsJSON(productiveQueries));
		result.put("accepted", accepted);
		result.put("resultHead", resultHead);
		result.put("resultBody", resultBody);
		return result;
	}

	private static boolean processQuery(String query) {
		if (enableALKSA) {
			return processALKSA(query);
		}

		// TODO process in database

		return true;
	}

	private static boolean processALKSA(String query) {
		boolean queryError = false;
		boolean queryAccepted = false;

		try {
			queryAccepted = alksa.accept(query, "eawa", "tester");
		} catch (ALKSAInvalidQueryException e) {
			queryError = true;
		}

		String prefix = queryError ? "ERROR: " : (queryAccepted ? "ACCEPTED: "
				: "REJECTED: ");

		if (alksa.isLearning()) {
			learnedQueries.add(prefix + query);
		} else {
			productiveQueries.add(prefix + query);
		}

		return !queryError && queryAccepted;
	}

	private static ArrayNode getQueriesAsJSON(List<String> queries) {
		ArrayNode result = mapper.createArrayNode();

		ListIterator<String> iterator = queries.listIterator(queries.size());

		while (iterator.hasPrevious()) {
			result.add(iterator.previous());
		}

		return result;
	}

	public static Result setLearning(boolean learning) {
		alksa.setLearning(learning);
		return ok();
	}

	public static Result enableALKSA(boolean enable) {
		enableALKSA = enable;
		return ok();
	}
}
