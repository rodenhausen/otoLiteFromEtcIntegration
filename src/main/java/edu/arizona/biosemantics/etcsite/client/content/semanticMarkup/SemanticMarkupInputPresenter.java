package edu.arizona.biosemantics.etcsite.client.content.semanticMarkup;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import edu.arizona.biosemantics.etcsite.client.common.Authentication;
import edu.arizona.biosemantics.etcsite.client.common.Configuration;
import edu.arizona.biosemantics.etcsite.client.common.LoadingPopup;
import edu.arizona.biosemantics.etcsite.client.common.MessagePresenter;
import edu.arizona.biosemantics.etcsite.client.common.files.FileImageLabelTreeItem;
import edu.arizona.biosemantics.etcsite.client.common.files.IFileTreeView;
import edu.arizona.biosemantics.etcsite.client.common.files.ISelectableFileTreeView;
import edu.arizona.biosemantics.etcsite.client.common.files.SelectableFileTreePresenter.ISelectListener;
import edu.arizona.biosemantics.etcsite.client.content.fileManager.IFileManagerDialogView;
import edu.arizona.biosemantics.etcsite.shared.db.Task;
import edu.arizona.biosemantics.etcsite.shared.file.FileFilter;
import edu.arizona.biosemantics.etcsite.shared.file.FilePathShortener;
import edu.arizona.biosemantics.etcsite.shared.rpc.RPCCallback;
import edu.arizona.biosemantics.etcsite.shared.rpc.semanticmarkup.ISemanticMarkupServiceAsync;
import edu.arizona.biosemantics.etcsite.shared.rpc.semanticmarkup.TaskStageEnum;

public class SemanticMarkupInputPresenter implements ISemanticMarkupInputView.Presenter {

	private ISemanticMarkupInputView view;
	private PlaceController placeController;
	private ISemanticMarkupServiceAsync semanticMarkupService;
	private edu.arizona.biosemantics.etcsite.client.common.files.ISelectableFileTreeView.Presenter selectableFileTreePresenter;
	private edu.arizona.biosemantics.etcsite.client.common.files.IFileTreeView.Presenter fileTreePresenter;
	private FilePathShortener filePathShortener;
	private String inputFile;
	private IFileManagerDialogView.Presenter fileManagerDialogPresenter;
	private LoadingPopup loadingPopup = new LoadingPopup();
	private MessagePresenter messagePresenter = new MessagePresenter();

	@Inject
	public SemanticMarkupInputPresenter(ISemanticMarkupInputView view, PlaceController 
			placeController, ISemanticMarkupServiceAsync semanticMarkupService, 
			ISelectableFileTreeView.Presenter selectableFileTreePresenter,
			FilePathShortener filePathShortener,
			IFileManagerDialogView.Presenter fileManagerDialogPresenter
			) {
		this.view = view;
		view.setPresenter(this);
		this.placeController = placeController;
		this.semanticMarkupService = semanticMarkupService;
		
		this.selectableFileTreePresenter = selectableFileTreePresenter;
		this.fileTreePresenter = selectableFileTreePresenter.getFileTreePresenter();
		this.filePathShortener = filePathShortener;
		this.fileManagerDialogPresenter = fileManagerDialogPresenter;
	}
	
	@Override
	public IsWidget getView() {
		view.resetFields();
		inputFile = null;
		return view;
	}

	@Override
	public void onNext() {
		//error checking.
		if (view.getTaskName().equals("")){
			messagePresenter.showOkBox("Error", "Enter a name for this task.");
			return;
		}
		if (inputFile == null){
			messagePresenter.showOkBox("Error", "Specify an input folder");
			return;
		}
		
		
		
		loadingPopup.start();
		semanticMarkupService.isValidInput(Authentication.getInstance().getToken(), inputFile, new RPCCallback<Boolean>() {
			@Override
			public void onResult(Boolean result) {
				if(!result) {
					messagePresenter.showOkBox("Input", "Input directory is invalid.");
					loadingPopup.stop();
				} else {
					semanticMarkupService.start(Authentication.getInstance().getToken(), 
							view.getTaskName(), inputFile, view.getGlossaryName(), new RPCCallback<Task>() {
								@Override
								public void onResult(Task result) {
									switch(TaskStageEnum.valueOf(result.getTaskStage().getTaskStage())) {
									case LEARN_TERMS:
										placeController.goTo(new SemanticMarkupLearnPlace(result));
										break;
									default:
										placeController.goTo(new SemanticMarkupPreprocessPlace(result));
										break;
									}
									loadingPopup.stop();
								}
					});
				}
			}
		});
	}

	@Override
	public void onInput() {
		selectableFileTreePresenter.show("Select input", FileFilter.DIRECTORY, new ISelectListener() {
			@Override
			public void onSelect() {
				FileImageLabelTreeItem selection = fileTreePresenter.getSelectedItem();
				if (selection != null) {
					inputFile = selection.getFileInfo().getFilePath();
					String shortendPath = filePathShortener.shorten(selection.getFileInfo(), Authentication.getInstance().getUserId());
					view.setInput(shortendPath);
					view.setEnabledNext(true);			
					if(selection.getFileInfo().getOwnerUserId() != Authentication.getInstance().getUserId()) {
						messagePresenter.showOkBox("Shared input", "The selected input is not owned. To start the task the files will be copied to your own space.");
						fileManagerDialogPresenter.hide();
					} else {
						fileManagerDialogPresenter.hide();
					}
				}
			}
		});
	}

	@Override
	public void onFileManager() {			
		fileManagerDialogPresenter.show();
	}

}
