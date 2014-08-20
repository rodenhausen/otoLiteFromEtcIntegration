package edu.arizona.biosemantics.etcsite.client.common;

import com.google.gwt.user.client.ui.CaptchaPanel;
import com.google.gwt.user.client.ui.IsWidget;

public interface IRegisterView extends IsWidget {

	public interface Presenter {

		void onCancel();
		
		void onCreate();

		void show(IRegisterListener listener);
		
		String getEmail();
		
		void clearAllBoxes();
	}
	
	public interface IRegisterListener {
		void onRegister(String message);
		void onRegisterFailure(String message);
		void onCancel();
	}
	
	void setPresenter(Presenter presenter);
	String getFirstName();
	String getLastName();
	String getEmail();
	String getPassword();
	String getConfirmPassword();
	CaptchaPanel getCaptchaPanel();
	
	void setErrorMessage(String str);
	void clearPasswordBoxes();
	void clearAllBoxes();
	void giveFocus();
}
