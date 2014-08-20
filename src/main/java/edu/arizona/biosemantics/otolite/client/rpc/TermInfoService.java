package edu.arizona.biosemantics.otolite.client.rpc;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import edu.arizona.biosemantics.otolite.shared.beans.terminfo.TermContext;
import edu.arizona.biosemantics.otolite.shared.beans.terminfo.TermDictionary;
import edu.arizona.biosemantics.otolite.shared.beans.terminfo.TermGlossary;

@RemoteServiceRelativePath("termInfo")
public interface TermInfoService extends RemoteService {
	ArrayList<TermContext> getTermContext(String term, String uploadID)
			throws Exception;

	ArrayList<TermGlossary> getTermGlossary(String term, String glossaryType)
			throws Exception;

	TermDictionary getTermDictionary(String term) throws Exception;
	
	String getFileContent(String uploadID, String sourceName) throws Exception;
}
