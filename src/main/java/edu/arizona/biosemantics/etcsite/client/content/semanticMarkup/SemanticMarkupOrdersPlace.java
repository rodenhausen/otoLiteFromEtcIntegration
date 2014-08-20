package edu.arizona.biosemantics.etcsite.client.content.semanticMarkup;

import com.google.gwt.place.shared.PlaceTokenizer;

import edu.arizona.biosemantics.etcsite.shared.db.Task;

public class SemanticMarkupOrdersPlace extends SemanticMarkupPlace {

	public SemanticMarkupOrdersPlace() {
		super(null);
	}
	
	public SemanticMarkupOrdersPlace(Task task) {
		super(task);
	}

	public static class Tokenizer implements PlaceTokenizer<SemanticMarkupOrdersPlace> {

		@Override
		public SemanticMarkupOrdersPlace getPlace(String token) {
			Task task = new Task();
			try {
				int taskId = Integer.parseInt(token.split("task=")[1]);
				task.setId(taskId);
			} catch(Exception e) {
				return new SemanticMarkupOrdersPlace(null);
			}
			return new SemanticMarkupOrdersPlace(task);
		}

		@Override
		public String getToken(SemanticMarkupOrdersPlace place) {
			return "task=" + place.getTask().getId();
		}

	}
}
