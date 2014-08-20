package edu.arizona.biosemantics.etcsite.client.common;

import com.google.gwt.user.client.Window.Location;
import com.google.inject.Inject;
import com.sencha.gxt.widget.core.client.Dialog;

import edu.arizona.biosemantics.etcsite.client.common.ILoginView.ILoginListener;
import edu.arizona.biosemantics.etcsite.shared.rpc.AuthenticationResult;
import edu.arizona.biosemantics.etcsite.shared.rpc.IAuthenticationServiceAsync;
import edu.arizona.biosemantics.etcsite.shared.rpc.RPCCallback;

public class LoginPresenter implements ILoginView.Presenter {

	private IAuthenticationServiceAsync authenticationService;
	private ILoginView loginView;
	private ILoginListener currentListener;
	private Dialog dialog;

	@Inject
	public LoginPresenter(ILoginView loginView, IAuthenticationServiceAsync authenticationService) {
		this.loginView = loginView;
		loginView.setPresenter(this);
		this.authenticationService = authenticationService;
		
		dialog = new Dialog();
		dialog.setBodyBorder(false);
		dialog.setHeadingText("Login");
		dialog.setPixelSize(-1, -1);
		dialog.setMinWidth(0);
		dialog.setMinHeight(0);
	    dialog.setResizable(true);
	    dialog.setShadow(true);
		dialog.setHideOnButtonClick(true);
		dialog.setPredefinedButtons();

		dialog.add(loginView.asWidget());
	}

	@Override
	public void onLogin() {
		String password = loginView.getPassword();
		authenticationService.login(loginView.getUsername(), password, new RPCCallback<AuthenticationResult>() {
			@Override
			public void onResult(AuthenticationResult result) {
				if(result.getResult()) {
					Authentication auth = Authentication.getInstance();
					auth.setUserId(result.getUser().getId());
					auth.setSessionID(result.getSessionID());
					auth.setEmail(result.getUser().getEmail());
					auth.setFirstName(result.getUser().getFirstName());
					auth.setLastName(result.getUser().getLastName());
					auth.setAffiliation(result.getUser().getAffiliation());
					
					dialog.hide();
					if(currentListener != null)
						currentListener.onLogin();
				} else {
					if(currentListener != null)
						currentListener.onLoginFailure();
				}
			}
		});
	}
	

	@Override
	public void onCancel() {
		if (currentListener != null)
			currentListener.onCancel();
	}
	
	@Override
	public void show(ILoginListener listener) {
		loginView.clearPasswordTextBox();
		loginView.giveLoginFocus();
		this.currentListener = listener;
		dialog.show();
	}
	
	@Override
	public void setEmailField(String str){
		loginView.setEmail(str);
	}
	
	@Override
	public void setMessage(String str){
		loginView.setMessage(str);
	}

	@Override
	public void onRegisterRequest() {
		dialog.hide();
		currentListener.onRegisterRequest();
	}

	@Override
	public void onResetPasswordRequest() {
		dialog.hide();
		currentListener.onResetPasswordRequest();
	}
	
	@Override
	public void onSignInWithGoogle() {
		dialog.hide();
		String url = "https://accounts.google.com/o/oauth2/auth?scope=https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/userinfo.email&client_id=" + ServerSetup.getInstance().getSetup().getGoogleClientId() + "&redirect_uri=" + ServerSetup.getInstance().getSetup().getGoogleRedirectURI() + "&response_type=token";
		Location.replace(url);
	}

	@Override
	public String getEmailField() {
		return loginView.getUsername();
	}
}
