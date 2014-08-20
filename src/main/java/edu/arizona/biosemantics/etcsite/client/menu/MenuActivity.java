package edu.arizona.biosemantics.etcsite.client.menu;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

import edu.arizona.biosemantics.etcsite.client.common.Authentication;
import edu.arizona.biosemantics.etcsite.client.common.MessagePresenter;
import edu.arizona.biosemantics.etcsite.client.content.matrixGeneration.MatrixGenerationInputPlace;
import edu.arizona.biosemantics.etcsite.client.content.semanticMarkup.SemanticMarkupInputPlace;
import edu.arizona.biosemantics.etcsite.client.content.taskManager.ResumeTaskPlaceMapper;
import edu.arizona.biosemantics.etcsite.client.content.taxonomyComparison.TaxonomyComparisonPlace;
import edu.arizona.biosemantics.etcsite.client.content.treeGeneration.TreeGenerationPlace;
import edu.arizona.biosemantics.etcsite.client.content.visualization.VisualizationPlace;
import edu.arizona.biosemantics.etcsite.client.menu.IMenuView.Presenter;
import edu.arizona.biosemantics.etcsite.shared.db.Task;
import edu.arizona.biosemantics.etcsite.shared.rpc.RPCCallback;
import edu.arizona.biosemantics.etcsite.shared.rpc.matrixGeneration.IMatrixGenerationServiceAsync;
import edu.arizona.biosemantics.etcsite.shared.rpc.semanticmarkup.ISemanticMarkupServiceAsync;

public class MenuActivity extends AbstractActivity implements Presenter {

	private IMenuView menuView;
	private PlaceController placeController;
	private ISemanticMarkupServiceAsync semanticMarkupService;
	private IMatrixGenerationServiceAsync matrixGenerationService;
	private ResumeTaskPlaceMapper resumeTaskPlaceMapper;
	private MessagePresenter messagePresenter = new MessagePresenter();

	@Inject
	public MenuActivity(IMenuView menuView, PlaceController placeController, 
			ISemanticMarkupServiceAsync semanticMarkupService,
			IMatrixGenerationServiceAsync matrixGenerationService, 
			ResumeTaskPlaceMapper resumeTaskPlaceMapper) {
		this.menuView = menuView;
		this.placeController = placeController;
		this.semanticMarkupService = semanticMarkupService;
		this.matrixGenerationService = matrixGenerationService;
		this.resumeTaskPlaceMapper = resumeTaskPlaceMapper;
	}
	
	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		menuView.setPresenter(this);
		panel.setWidget(menuView.asWidget());
		menuView.initNativeJavascriptAnimations();
	}

	@Override
	public void onSemanticMarkup() {		
		semanticMarkupService.getLatestResumable(Authentication.getInstance().getToken(),
				new RPCCallback<Task>() {
			@Override
			public void onResult(final Task task) {
				if(task != null)
					messagePresenter.showOkCandelBox(
						"Resumable Task", "You have a resumable task of this type",  "Start new", "Resume", new MessagePresenter.IConfirmListener() {
							public void onConfirm() {
								placeController.goTo(resumeTaskPlaceMapper.getPlace(task));
							}
							public void onCancel() {
								placeController.goTo(new SemanticMarkupInputPlace());
							}
						});
				else 
					placeController.goTo(new SemanticMarkupInputPlace());
			}
		});
	}

	@Override
	public void onMatrixGeneration() {
		matrixGenerationService.getLatestResumable(Authentication.getInstance().getToken(),
				new RPCCallback<Task>() {
			@Override
			public void onResult(final Task task) {
				if(task != null) 
					messagePresenter.showOkCandelBox(
						"Resumable Task", "You have a resumable task of this type", "Start new", "Resume",  new MessagePresenter.IConfirmListener() {
							public void onConfirm() {
								placeController.goTo(resumeTaskPlaceMapper.getPlace(task));
							}
							public void onCancel() {
								placeController.goTo(new MatrixGenerationInputPlace());
							}
						});
				else 
					placeController.goTo(new MatrixGenerationInputPlace());
			}
		});
	}

	@Override
	public void onTreeGeneration() {
		placeController.goTo(new TreeGenerationPlace());
	}

	@Override
	public void onTaxonomyComparison() {
		placeController.goTo(new TaxonomyComparisonPlace());
	}

	@Override
	public void onVisualization() {
		placeController.goTo(new VisualizationPlace());
	}

}
