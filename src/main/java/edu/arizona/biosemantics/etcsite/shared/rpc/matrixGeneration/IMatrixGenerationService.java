package edu.arizona.biosemantics.etcsite.shared.rpc.matrixGeneration;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import edu.arizona.biosemantics.etcsite.shared.db.Task;
import edu.arizona.biosemantics.etcsite.shared.rpc.AuthenticationToken;
import edu.arizona.biosemantics.etcsite.shared.rpc.IHasTasksService;
import edu.arizona.biosemantics.etcsite.shared.rpc.RPCResult;
import edu.arizona.biosemantics.matrixreview.shared.model.TaxonMatrix;

@RemoteServiceRelativePath("matrixGeneration")
public interface IMatrixGenerationService extends RemoteService, IHasTasksService {

	public RPCResult<Task> start(AuthenticationToken authenticationToken, String taskName, String input);
	
	public RPCResult<Task> process(AuthenticationToken authenticationToken, Task task);
	
	public RPCResult<TaxonMatrix> review(AuthenticationToken authenticationToken, Task task);
	
	public RPCResult<Task> completeReview(AuthenticationToken authenticationToken, Task task);
	
	public RPCResult<Task> output(AuthenticationToken authenticationToken, Task task);
	
	public RPCResult<Void> save(AuthenticationToken authenticationToken, TaxonMatrix matrix, Task task);
	
	public RPCResult<Task> goToTaskStage(AuthenticationToken authenticationToken, Task task, TaskStageEnum review);

	public RPCResult<Boolean> isValidInput(AuthenticationToken authenticationToken,
			String filePath);

}
