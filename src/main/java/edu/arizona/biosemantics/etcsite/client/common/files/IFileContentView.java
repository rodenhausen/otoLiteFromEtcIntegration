package edu.arizona.biosemantics.etcsite.client.common.files;

import com.google.gwt.user.client.ui.IsWidget;

import edu.arizona.biosemantics.etcsite.shared.file.FileInfo;

public interface IFileContentView extends IsWidget {

	public interface Presenter {

		void onClose();
		void onSave();
		void onFormatChange(String format);

		void show(FileInfo fileInfo);
		void onEdit();
		
	}

	String getFormat();

	void setText(String result);

	void setPresenter(Presenter presenter);

	void setEditable(boolean enabled);

	String getText();
	
	
	
}
