package edu.arizona.biosemantics.etcsite.client.top;

import com.google.gwt.activity.shared.MyAbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

import edu.arizona.biosemantics.etcsite.client.common.Authentication;
import edu.arizona.biosemantics.etcsite.client.common.ILoginView;
import edu.arizona.biosemantics.etcsite.client.common.IRegisterView;
import edu.arizona.biosemantics.etcsite.client.common.IResetPasswordView;
import edu.arizona.biosemantics.etcsite.client.content.about.AboutPlace;
import edu.arizona.biosemantics.etcsite.client.content.help.HelpPlace;
import edu.arizona.biosemantics.etcsite.client.content.home.HomePlace;
import edu.arizona.biosemantics.etcsite.client.content.news.NewsPlace;
import edu.arizona.biosemantics.etcsite.client.top.ILoginTopView.Presenter;
import edu.arizona.biosemantics.etcsite.shared.rpc.AuthenticationResult;
import edu.arizona.biosemantics.etcsite.shared.rpc.IAuthenticationServiceAsync;
import edu.arizona.biosemantics.etcsite.shared.rpc.RPCCallback;

public class LoggedOutActivity extends MyAbstractActivity implements Presenter {
	
	private ILoginTopView loginTopView;

	@Inject
	public LoggedOutActivity(ILoginTopView loginTopView, 
			PlaceController placeController, 
			IAuthenticationServiceAsync authenticationService, 
			ILoginView.Presenter loginPresenter, 
			IRegisterView.Presenter registerPresenter, 
			IResetPasswordView.Presenter resetPasswordPresenter) {
		super(placeController, authenticationService, loginPresenter, registerPresenter, resetPasswordPresenter);
		this.loginTopView = loginTopView;
	}
	
	@Override
	public void start(final AcceptsOneWidget panel, EventBus eventBus) {
		Authentication authentication = Authentication.getInstance();
		if(authentication.isSet()) {
			authenticationService.isValidSession(authentication.getToken(), new RPCCallback<AuthenticationResult>() {
				@Override
				public void onResult(AuthenticationResult result) {
					if(result.getResult()) {
						placeController.goTo(new LoggedInPlace());
					} else {
						Authentication.getInstance().destroy(); //if this session is not valid, clear the authentication cookies.
						setLoginView(panel);
					}
				}
			});
		} else if (authentication.getExternalAccessToken() != null){ //check to see if this is a redirect from Google
			String accessToken = authentication.getExternalAccessToken();
			authentication.setExternalAccessToken(null); //don't need this anymore. 
			authenticationService.loginWithGoogle(accessToken, new RPCCallback<AuthenticationResult>(){
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
						placeController.goTo(new LoggedInPlace());
					} else {
						messagePresenter.showOkBox("Sign-out", "An error occurred. Please try signing in again later.");
					}
				}
			});
		} else {
			setLoginView(panel);
		}
	}

	private void setLoginView(AcceptsOneWidget panel) {
		loginTopView.setPresenter(this);
		panel.setWidget(loginTopView.asWidget());
	}

	@Override
	public void onHome() {
		placeController.goTo(new HomePlace());
	}
	
	@Override
	public void onAbout() {
		placeController.goTo(new AboutPlace());
	}
	
	@Override
	public void onHelp() {
		placeController.goTo(new HelpPlace());
	}
	
	@Override
	public void onNews() {
		placeController.goTo(new NewsPlace());
	}

	@Override
	public void onLogin() {
		if(Authentication.getInstance().isSet()) {
			placeController.goTo(new LoggedInPlace());
		} else {
			showLoginWindow();
		}
	}

	@Override
	public void update() {}

	
	
	/*private void doGotoPlace(final HasTaskPlace gotoPlace, IHasTasksServiceAsync tasksService) {
		tasksService.getLatestResumable(Authentication.getInstance().getToken(),
				new RPCCallback<Task>() {
			@Override
			public void onResult(final Task task) {
				if(task != null) 
					messageConfirmPresenter.show( _ags
						"Resumable Task", "You have a resumable task of this type", "Start new", "Resume", new IConfirmListener() {
							public void onConfirm() {
								gotoPlace.setTask(task);
								placeController.goTo(gotoPlace);
							}
							public void onCancel() {
								placeController.goTo(gotoPlace);
							}
						});
				else 
					placeController.goTo(gotoPlace);
			}
		});
	}*/
}
