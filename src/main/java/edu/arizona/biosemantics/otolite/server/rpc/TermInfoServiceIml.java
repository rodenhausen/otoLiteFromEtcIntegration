package edu.arizona.biosemantics.otolite.server.rpc;

import java.util.ArrayList;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import edu.arizona.biosemantics.otolite.client.rpc.TermInfoService;
import edu.arizona.biosemantics.otolite.server.db.ContextDAO;
import edu.arizona.biosemantics.otolite.server.db.ToOntologiesDAO;
import edu.arizona.biosemantics.otolite.server.fileio.FileIO;
import edu.arizona.biosemantics.otolite.server.oto.QueryOTO;
import edu.arizona.biosemantics.otolite.server.utilities.Utilities;
import edu.arizona.biosemantics.otolite.shared.beans.terminfo.TermContext;
import edu.arizona.biosemantics.otolite.shared.beans.terminfo.TermDictionary;
import edu.arizona.biosemantics.otolite.shared.beans.terminfo.TermGlossary;

public class TermInfoServiceIml extends RemoteServiceServlet implements
		TermInfoService {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8348271874856917361L;

	@Override
	public ArrayList<TermContext> getTermContext(String term, String uploadID)
			throws Exception {
		return ContextDAO.getInstance().getTermContext(term,
				Integer.parseInt(uploadID));
	}

	@Override
	public ArrayList<TermGlossary> getTermGlossary(String term,
			String glossaryType) throws Exception {
		ArrayList<TermGlossary> glosses = ToOntologiesDAO.getInstance()
				.getOntologyMatchForTerm(term, Integer.parseInt(glossaryType));
		glosses.addAll(QueryOTO.getInstance().getGlossaryInfo(term,
				Utilities.getGlossaryNameByID(Integer.parseInt(glossaryType))));
		return glosses;
	}

	@Override
	public TermDictionary getTermDictionary(String term) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFileContent(String uploadID, String sourceName)
			throws Exception {
		return FileIO.getInstance().getContextFileContent(uploadID, sourceName);
	}

}
