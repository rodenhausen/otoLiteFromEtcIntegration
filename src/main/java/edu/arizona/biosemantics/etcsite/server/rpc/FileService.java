package edu.arizona.biosemantics.etcsite.server.rpc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import edu.arizona.biosemantics.etcsite.server.Configuration;
import edu.arizona.biosemantics.etcsite.server.XmlNamespaceManager;
import edu.arizona.biosemantics.etcsite.shared.db.AbstractTaskConfiguration;
import edu.arizona.biosemantics.etcsite.shared.db.FilesInUseDAO;
import edu.arizona.biosemantics.etcsite.shared.db.Share;
import edu.arizona.biosemantics.etcsite.shared.db.ShareDAO;
import edu.arizona.biosemantics.etcsite.shared.db.ShortUser;
import edu.arizona.biosemantics.etcsite.shared.db.Task;
import edu.arizona.biosemantics.etcsite.shared.db.TasksOutputFilesDAO;
import edu.arizona.biosemantics.etcsite.shared.db.UserDAO;
import edu.arizona.biosemantics.etcsite.shared.file.FileFilter;
import edu.arizona.biosemantics.etcsite.shared.file.FileInfo;
import edu.arizona.biosemantics.etcsite.shared.file.FileTypeEnum;
import edu.arizona.biosemantics.etcsite.shared.rpc.AuthenticationToken;
import edu.arizona.biosemantics.etcsite.shared.rpc.IFileFormatService;
import edu.arizona.biosemantics.etcsite.shared.rpc.IFilePermissionService;
import edu.arizona.biosemantics.etcsite.shared.rpc.IFileService;
import edu.arizona.biosemantics.etcsite.shared.rpc.RPCResult;
import edu.arizona.biosemantics.etcsite.shared.rpc.Tree;

public class FileService extends RemoteServiceServlet implements IFileService {

	private static final long serialVersionUID = -9193602268703418530L;
	private IFilePermissionService filePermissionService = new FilePermissionService();
	private IFileFormatService fileFormatService = new FileFormatService();
	private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM-dd-yyyy");
	private	XmlNamespaceManager xmlNamespaceManager = new XmlNamespaceManager();
	
	@Override
	protected void doUnexpectedFailure(Throwable t) {
	    t.printStackTrace(System.err);
	    super.doUnexpectedFailure(t);
	}
	
	/**
	 * Cannot hand a fileFilter implementation of .filter(File file) to the method because
	 * file access is not available in the client code. Hence, translation from enum to implementation 
	 * becomes necessary.
	 */
	@Override
	public RPCResult<Tree<FileInfo>> getUsersFiles(AuthenticationToken authenticationToken, FileFilter fileFilter) {
		//will become part of 'target' and has effect on several things. therefore leave root as "" for now. Also will have to escape \\'s before it is inserted into SQL statements
		//sql injection..
		//Tree<FileInfo> result = new Tree<FileInfo>(new FileInfo(authenticationToken.getUsername() + "'s files", FileType.DIRECTORY));
		
		Tree<FileInfo> resultTree = new Tree<FileInfo>(new FileInfo("", "Root", "", FileTypeEnum.DIRECTORY, authenticationToken.getUserId(), true, false));
		Tree<FileInfo> ownedFiles = new Tree<FileInfo>(new FileInfo("Owned", Configuration.fileBase + File.separator + authenticationToken.getUserId(),
				"Owned", FileTypeEnum.DIRECTORY, authenticationToken.getUserId(), true, true));
		Tree<FileInfo> sharedFiles = new Tree<FileInfo>(new FileInfo("Shared", "Shared", "Shared", FileTypeEnum.DIRECTORY, authenticationToken.getUserId(), true, false));
		resultTree.addChild(ownedFiles);
		resultTree.addChild(sharedFiles);
		
		try {
			decorateOwnedTree(authenticationToken, ownedFiles, fileFilter, Configuration.fileBase + File.separator + authenticationToken.getUserId());
			decorateSharedTree(authenticationToken, sharedFiles, fileFilter);
			return new RPCResult<Tree<FileInfo>>(true, resultTree);
		} catch(Exception e) {
			e.printStackTrace();
			return new RPCResult<Tree<FileInfo>>(false, e.getMessage());
		}
		
	}
	
	private void decorateSharedTree(AuthenticationToken authenticationToken, Tree<FileInfo> sharedFiles, FileFilter fileFilter) throws Exception {
		ShortUser user = UserDAO.getInstance().getShortUser(authenticationToken.getUserId());
		List<Share> shares = ShareDAO.getInstance().getSharesOfInvitee(user);
		
		for(Share share : shares) {
			int shareOwnerUserId = share.getTask().getUser().getId();
			Tree<FileInfo> shareTree = new Tree<FileInfo>(new FileInfo(share.getTask().getName(), "Share." + share.getTask().getId(), 
					share.getTask().getName(), FileTypeEnum.DIRECTORY, shareOwnerUserId, false, false));
			sharedFiles.addChild(shareTree);
			AbstractTaskConfiguration taskConfiguration = share.getTask().getConfiguration();
			
			List<String> outputs = TasksOutputFilesDAO.getInstance().getOutputs(share.getTask());
			Tree<FileInfo> outputTree = new Tree<FileInfo>(new FileInfo("Output", "Share.Output" + share.getTask().getId(), 
					"Output", FileTypeEnum.DIRECTORY, shareOwnerUserId, false, false));
			shareTree.addChild(outputTree);
			for(String output : outputs) {
				File child = new File(output);
				if(child.exists()) {
					RPCResult<Boolean> permissionResult = filePermissionService.hasReadPermission(authenticationToken, output);
					if(!permissionResult.isSucceeded())
						throw new Exception("Couldn't check permission");
					if(permissionResult.getData()) {
						String displayPath = share.getTask().getName() + File.separator + "Output" + File.separator + child.getName();
						Tree<FileInfo> childTree = new Tree<FileInfo>(new FileInfo(child.getName(), child.getAbsolutePath(), displayPath, 
								getFileType(authenticationToken, output), shareOwnerUserId, false, false));
						outputTree.addChild(childTree);
						if(child.isDirectory()) {
							decorateOwnedTree(authenticationToken, childTree, fileFilter, child.getAbsolutePath());
						}
					}
				}
			}
			
			if(taskConfiguration != null) {
				List<String> inputFiles = taskConfiguration.getInputs();
				Tree<FileInfo> inputTree = new Tree<FileInfo>(new FileInfo("Input", "Share.Input" + share.getTask().getId(), "Input", 
						FileTypeEnum.DIRECTORY, shareOwnerUserId, false, false));
				shareTree.addChild(inputTree);
				for(String input : inputFiles) {
					File child = new File(input);
					if(child.exists()) {
						RPCResult<Boolean> permissionResult = filePermissionService.hasReadPermission(authenticationToken, input);
						if(!permissionResult.isSucceeded())
							throw new Exception("Couldn't check permission");
						if(permissionResult.getData()) {
							String displayPath = share.getTask().getName() + File.separator + "Input" + File.separator + child.getName();
							Tree<FileInfo> childTree = new Tree<FileInfo>(new FileInfo(child.getName(), child.getAbsolutePath(), displayPath, 
									getFileType(authenticationToken, input), shareOwnerUserId, false, false));
							inputTree.addChild(childTree);
							if(child.isDirectory()) {
								decorateOwnedTree(authenticationToken, childTree, fileFilter, child.getAbsolutePath());
							}
						}
					}
				}
			}
		}
	}

	private void decorateOwnedTree(AuthenticationToken authenticationToken, Tree<FileInfo> fileTree, FileFilter fileFilter, String filePath) throws Exception {
		RPCResult<Boolean> permissionResult = filePermissionService.hasReadPermission(authenticationToken, filePath);
		if(!permissionResult.isSucceeded())
			throw new Exception("Couldn't check permission");
		if(permissionResult.getData()) {
			File file = new File(filePath);
			File[] childFiles = file.listFiles();
			Arrays.sort(childFiles);
			for(File child : childFiles) {
				String childPath = child.getAbsolutePath();
				permissionResult = filePermissionService.hasReadPermission(authenticationToken, childPath);
				if(!permissionResult.isSucceeded())
					throw new Exception("Couldn't check permission");
				if(permissionResult.getData()) {
					String name = child.getName();
					FileTypeEnum fileType = getFileType(authenticationToken, child.getAbsolutePath());
					if(fileType != null && !filter(fileType, fileFilter)) {
						String displayPath = childPath.replace(Configuration.fileBase + File.separator + authenticationToken.getUserId(), "");
						Tree<FileInfo> childTree = new Tree<FileInfo>(new FileInfo(name, child.getAbsolutePath(), displayPath, fileType, 
								authenticationToken.getUserId(), false, child.isDirectory()));
						fileTree.addChild(childTree);
						if(child.isDirectory()) {
							decorateOwnedTree(authenticationToken, childTree, fileFilter, childPath);
						}
					}
				}
			}
		}
	}

	private boolean filter(FileTypeEnum fileType, FileFilter fileFilter) {
		switch(fileFilter) {
		case TAXON_DESCRIPTION:
			return !fileType.equals(FileTypeEnum.TAXON_DESCRIPTION);
		case MARKED_UP_TAXON_DESCRIPTION:
			return !fileType.equals(FileTypeEnum.MARKED_UP_TAXON_DESCRIPTION);	
		case MATRIX:
			return !fileType.equals(FileTypeEnum.MATRIX);	
		case ALL:
			return false;
		case FILE:
			return fileType.equals(FileTypeEnum.DIRECTORY);
		case DIRECTORY:
			return !fileType.equals(FileTypeEnum.DIRECTORY);
		}
		return true;
	}

	private FileTypeEnum getFileType(AuthenticationToken authenticationToken, String filePath) {
		File file = new File(filePath);
		if(file.isFile()) {
			if(file.getName().endsWith(".xml")) {
				try {
					return xmlNamespaceManager.getFileType(new File(filePath));
				} catch(Exception e) {
					e.printStackTrace();
					return null;
				}
			} else if(file.getName().endsWith(".mx") || file.getName().endsWith(".nxs") || file.getName().endsWith(".nex") || file.getName().endsWith(".sdd")) {
				return FileTypeEnum.MATRIX;
			} 
			/*RPCResult<Boolean> validationResult = fileFormatService.isValidMarkedupTaxonDescription(authenticationToken, filePath);
			if(validationResult.isSucceeded() && validationResult.getData())
				return FileTypeEnum.MARKED_UP_TAXON_DESCRIPTION;
			validationResult = fileFormatService.isValidTaxonDescription(authenticationToken, filePath);
			if(validationResult.isSucceeded() && validationResult.getData())
				return FileTypeEnum.TAXON_DESCRIPTION;
			*/
			//return FileTypeEnum.PLAIN_TEXT;
			/*validationResult = fileFormatService.isValidMatrix(authenticationToken, filePath);
			if(validationResult.isSucceeded() && validationResult.getData())
				return FileTypeEnum.MATRIX;*/
		} else if (file.isDirectory())
			return FileTypeEnum.DIRECTORY;
		return null;
	}

	/**
	 * if delete successfully, leave result message blank "".
	 * otherwise, pass the error in result message.
	 */
	@Override
	public RPCResult<Void> deleteFile(AuthenticationToken authenticationToken, String filePath) {
		RPCResult<Boolean> permissionResult = filePermissionService.hasWritePermission(authenticationToken, filePath);
		if(!permissionResult.isSucceeded())
			return new RPCResult<Void>(false, permissionResult.getMessage());
		if(permissionResult.getData()) {
			RPCResult<Boolean> inUseResult = this.isInUse(authenticationToken, filePath);
			if(inUseResult.isSucceeded() && !inUseResult.getData()) {
				File file = new File(filePath);
				try {
					boolean resultDelete = deleteRecursively(authenticationToken, file);
					return new RPCResult<Void>(resultDelete, resultDelete ? "" : "File delete failed");
				} catch(Exception e) {
					e.printStackTrace();
					return new RPCResult<Void>(false, e.getMessage());
				}
			} else {
				return new RPCResult<Void>(false, createMessageFileInUse(authenticationToken, filePath)+". File was not deleted.");
			}
		}
		return new RPCResult<Void>(false, "Permission denied");
	}
	
	private String createMessageFileInUse(AuthenticationToken authenticationToken, String filePath) {
		RPCResult<List<Task>> tasksResult = this.getUsingTasks(authenticationToken, filePath);
		if(!tasksResult.isSucceeded()) 
			return "Couldn't retrieve tasks using the file";
		List<Task> tasks = tasksResult.getData();
		StringBuilder messageBuilder = new StringBuilder("File is in use by task(s): ");
		for(Task task : tasks) 
			messageBuilder.append(task.getName() + ", ");
		String message = messageBuilder.toString();
		return message.substring(0, message.length() - 2);
	}

	private boolean deleteRecursively(AuthenticationToken authenticationToken, File file) throws Exception {
		boolean result = false;
		
		String filePath = file.getAbsolutePath();
		RPCResult<Boolean> permissionResult = filePermissionService.hasWritePermission(authenticationToken, filePath);
		if(!permissionResult.isSucceeded())
			throw new Exception("Permission denied");
		if(permissionResult.getData()) {
			if (file.isDirectory()) {
				if (file.list().length == 0) {
					result = file.delete();
				} else {
					String files[] = file.list();
					for (String child : files) {
						File fileDelete = new File(file, child);
						result |= deleteRecursively(authenticationToken, fileDelete);
					}
					if (file.list().length == 0) {
						result |= file.delete();
					}
				}
			} else {
				result = file.delete();
			}
		}
		return result;
	}
	
	/**
	 * create directory successfully: message="", result = folder created
	 * failed to create directory: message=error, result = "";
	 */
	@Override
	public RPCResult<String> createDirectory(AuthenticationToken authenticationToken, String filePath, String idealFolderName, boolean force) { 
		if(idealFolderName.trim().isEmpty()) 
			return new RPCResult<String>(false, "Directory name may not be empty. The directory was not created.", "");
		
		//System.err.println("Making directory: " + filePath + ", " + idealFolderName);
		//try to make the parent directories.
		try{
			File dir = new File(filePath);
			dir.mkdirs();
		} catch(Exception e){
			e.printStackTrace();
		}
		
		RPCResult<Boolean> permissionResult = filePermissionService.hasWritePermission(authenticationToken, filePath);
		if(!permissionResult.isSucceeded())
			return new RPCResult<String>(false, permissionResult.getMessage()+ ". The directory was not created.", "");
		if(permissionResult.getData()) {
			RPCResult<Boolean> inUseResult = this.isInUse(authenticationToken, filePath);
			if(!inUseResult.isSucceeded())
				return new RPCResult<String>(false, inUseResult.getMessage()+". The directory was not created.", "");
			if(!inUseResult.getData()) {
				File file = new File(filePath + File.separator + idealFolderName);
				boolean resultMkDir = file.mkdir();
				if(!resultMkDir && force) {
					String date = dateTimeFormat.format(new Date());
					idealFolderName = idealFolderName + "_" + date;
					file = new File(filePath + File.separator + idealFolderName);
					resultMkDir = file.mkdir();
					int i = 1;
					while(!resultMkDir) {
						file = new File(filePath + File.separator + idealFolderName + "_" + i++);
						resultMkDir = file.mkdir();
					}
				}
				if(!resultMkDir) 
					return new RPCResult<String>(false, "Internal Server Error. The directory was not created.", "");
				else
					return new RPCResult<String>(true, "", file.getAbsolutePath());
			} else {
				return new RPCResult<String>(false, createMessageFileInUse(authenticationToken, filePath)+" The directory was not created.", "");
			}
		}
		return new RPCResult<String>(false, "Permission denied. The directory was not created.", "");
	}
	
	/**
	 * create file successfully: message="", result = file created
	 * failed to create file: message=error, result = "";
	 */
	@Override
	public RPCResult<String> createFile(AuthenticationToken authenticationToken, String directory, String idealFileName, boolean force) {					
		RPCResult<Boolean> permissionResult = filePermissionService.hasWritePermission(authenticationToken, directory);
		if(!permissionResult.isSucceeded())
			return new RPCResult<String>(false, permissionResult.getMessage() +". The file was not created.", "");
		if(permissionResult.getData()) {
			File file = new File(directory + File.separator + idealFileName);
			try {
				boolean createResult = file.createNewFile();
				int i = 1;
				while(!createResult && force) {
					String namePart = idealFileName;
					if(idealFileName.lastIndexOf(".") != -1) {
						namePart = idealFileName.substring(0, idealFileName.lastIndexOf("."));
					}
					file = new File(directory + File.separator + namePart + "_" + i++ + "." + 
							idealFileName.substring(idealFileName.lastIndexOf(".") + 1, idealFileName.length()));
					createResult = file.createNewFile();
				}
				if(createResult)
					return new RPCResult<String>(true, "", file.getAbsolutePath());
				else
					return new RPCResult<String>(false, "Creating file failed. The file was not created.", "");
			} catch (IOException e) {
				e.printStackTrace();
				return new RPCResult<String>(false, "Creating file failed. The file was not created.", "");
			}
		}
		return new RPCResult<String>(false, "Permission denied. The file was not created.", "");
	}
	
	@Override
	public RPCResult<Boolean> isDirectory(AuthenticationToken authenticationToken, String filePath) {
		RPCResult<Boolean> permissionResult = filePermissionService.hasReadPermission(authenticationToken, filePath);
		if(!permissionResult.isSucceeded())
			return new RPCResult<Boolean>(false, permissionResult.getMessage());
		if(permissionResult.getData()) {
			if(filePath.startsWith("Share.") || filePath.equals("Root") || filePath.equals("Owned") || filePath.equals("Shared"))
				return new RPCResult<Boolean>(true, true);
			File file = new File(filePath);
			return new RPCResult<Boolean>(true, file.isDirectory());
		}
		return new RPCResult<Boolean>(false, "Permission denied");
	}

	@Override
	public RPCResult<Boolean> isFile(AuthenticationToken authenticationToken, String filePath) {
		RPCResult<Boolean> permissionResult = filePermissionService.hasReadPermission(authenticationToken, filePath);
		if(!permissionResult.isSucceeded())
			return new RPCResult<Boolean>(false, permissionResult.getMessage());
		if(permissionResult.getData()) {
			File file = new File(filePath);
			return new RPCResult<Boolean>(true, file.isFile());
		}
		return new RPCResult<Boolean>(false, "Permission denied");
	}

	@Override
	public RPCResult<List<String>> getDirectoriesFiles(AuthenticationToken authenticationToken, String filePath) {	
		RPCResult<Boolean> permissionResult = filePermissionService.hasReadPermission(authenticationToken, filePath);
		if(!permissionResult.isSucceeded())
			return new RPCResult<List<String>>(false, permissionResult.getMessage());
		if(permissionResult.getData()) {
			List<String> resultList = new LinkedList<String>();
			File file = new File(filePath);
			File[] childFiles = file.listFiles();
			Arrays.sort(childFiles);
			for(File child : childFiles)
				if(child.isFile())
					resultList.add(child.getName());
			return new RPCResult<List<String>>(true, resultList);
		}
		return new RPCResult<List<String>>(false, "Permission denied");
		
		
	}

	@Override
	public RPCResult<Integer> getDepth(AuthenticationToken authenticationToken, String filePath) {
		RPCResult<Boolean> permissionResult = filePermissionService.hasReadPermission(authenticationToken, filePath);
		if(!permissionResult.isSucceeded())
			return new RPCResult<Integer>(false, permissionResult.getMessage());
		if(permissionResult.getData()) {
			File file = new File(filePath);
			int depth = this.getDepth(file);
			return new RPCResult<Integer>(true, depth);
		}
		return new RPCResult<Integer>(false, "Permission denied");
	}
	
	private int getDepth(File file) {
		int maxDepth = 0;
		for(File child : file.listFiles()) {
			if(child.isDirectory()) {
				int depth = getDepth(child) + 1;
				if(depth > maxDepth)
					maxDepth = depth;
			}
		}
		return maxDepth;
	}

	@Override
	public RPCResult<String> zipDirectory(AuthenticationToken authenticationToken, String filePath) {		
		DirectoryDownload directoryDownload = new DirectoryDownload(authenticationToken, filePath);
		
		boolean result = directoryDownload.execute();
		
		if(result)
			return new RPCResult<String>(true, "", directoryDownload.getZipPath());
		return new RPCResult<String>(false, directoryDownload.getError(), "");
	}

	@Override
	public RPCResult<Void> setInUse(AuthenticationToken authenticationToken, boolean value, String filePath, Task task) {		
		RPCResult<Boolean> permissionResult = filePermissionService.hasWritePermission(authenticationToken, filePath);
		if(!permissionResult.isSucceeded())
			return new RPCResult<Void>(false, permissionResult.getMessage());
		if(permissionResult.getData()) {
			try {
				FilesInUseDAO.getInstance().setInUse(value, filePath, task);
				return new RPCResult<Void>(true);
			} catch(Exception e) {
				e.printStackTrace();
				return new RPCResult<Void>(false, "Zipping failed");
			}		
		}
		return new RPCResult<Void>(false, "Permission denied");
	}

	@Override
	public RPCResult<Boolean> isInUse(AuthenticationToken authenticationToken, String filePath) {		
		//not required, isInUse doesn't read any actual content
		//RPCResult<Boolean> permissionResult = filePermissionService.hasReadPermission(authenticationToken, filePath);
		//if(!permissionResult.isSucceeded())
		//	return new RPCResult<Boolean>(false, permissionResult.getMessage());
		//if(permissionResult.getData()) {
			try {
				return new RPCResult<Boolean>(true, FilesInUseDAO.getInstance().isInUse(filePath));
			} catch(Exception e) {
				e.printStackTrace();
				return new RPCResult<Boolean>(false, "Internal Server Error");
			}
		//}
		//return new RPCResult<Boolean>(false, "Permission denied");
	}

	@Override
	public RPCResult<List<Task>> getUsingTasks(AuthenticationToken authenticationToken, String filePath) {
		RPCResult<Boolean> permissionResult = filePermissionService.hasReadPermission(authenticationToken, filePath);
		if(!permissionResult.isSucceeded())
			return new RPCResult<List<Task>>(false, permissionResult.getMessage());
		if(permissionResult.getData()) {
			try {
				return new RPCResult<List<Task>>(true, FilesInUseDAO.getInstance().getUsingTasks(filePath));
			} catch(Exception e) {
				e.printStackTrace();
				return new RPCResult<List<Task>>(false, "Internal Server Error");
			}
		}
		return new RPCResult<List<Task>>(false, "Permission denied");
	}

	@Override
	public RPCResult<Void> renameFile(AuthenticationToken authenticationToken, String path, String newFileName) {
		RPCResult<Boolean> permissionResult = filePermissionService.hasWritePermission(authenticationToken, path);
		if(!permissionResult.isSucceeded())
			return new RPCResult<Void>(false, permissionResult.getMessage());
		if(permissionResult.getData()) {
			RPCResult<Boolean> inUseResult = this.isInUse(authenticationToken, path);
			if(inUseResult.isSucceeded() && !inUseResult.getData()) {
				File file = new File(path);
				File newFile = new File(file.getParent() + File.separator + newFileName);
				if(newFile.exists()) 
					return new RPCResult<Void>(false, "File exists already");
				inUseResult = this.isInUse(authenticationToken, newFile.getAbsolutePath());
				if(inUseResult.isSucceeded() && !inUseResult.getData()) {
					if(file.getAbsolutePath().equals(newFile.getAbsolutePath()))
						return new RPCResult<Void>(true);
					try {
						if(file.isFile())
							FileUtils.moveFile(file, newFile);
						else
							FileUtils.moveDirectory(file, newFile);
						//Files.move(file, newFile);
						return new RPCResult<Void>(true);
					} catch (IOException e) {
						e.printStackTrace();
						return new RPCResult<Void>(false, "Couldn't move a file");
					}
				} else {
					return new RPCResult<Void>(false, createMessageFileInUse(authenticationToken, path));
				}
			} else {
				return new RPCResult<Void>(false, createMessageFileInUse(authenticationToken, path));
			}
		} 
		return new RPCResult<Void>(false, "Permission denied");	
	}
	
	@Override
	public RPCResult<Void> moveFile(AuthenticationToken authenticationToken, String filePath, String newFilePath) { 
		RPCResult<Boolean> permissionResult = filePermissionService.hasWritePermission(authenticationToken, filePath);
		File newDirectory = new File(newFilePath).getParentFile();
		if(newDirectory == null) 
			return new RPCResult<Void>(false, "Invalid destination");
		RPCResult<Boolean> permissionResultNewDirectory = filePermissionService.hasWritePermission(authenticationToken, newDirectory.getAbsolutePath());
		if(!permissionResult.isSucceeded() || !permissionResultNewDirectory.isSucceeded())
			return new RPCResult<Void>(false, permissionResult.getMessage());
		if(permissionResult.getData() && permissionResultNewDirectory.getData()) {
			RPCResult<Boolean> inUseResult = this.isInUse(authenticationToken, filePath);
			if(inUseResult.isSucceeded() && !inUseResult.getData()) {
				inUseResult = this.isInUse(authenticationToken, newFilePath);
				if(inUseResult.isSucceeded() && !inUseResult.getData()) {
					File file = new File(filePath);
					File newFile = new File(newFilePath);
					if(file.getAbsolutePath().equals(newFile.getAbsolutePath()))
						return new RPCResult<Void>(true);
					if(!newFile.exists())
						return new RPCResult<Void>(false, "Destination directory does not exist");
					try {
						FileUtils.moveToDirectory(file, newFile, false);
						//Files.move(file, newFile);
						return new RPCResult<Void>(true);
					} catch (IOException e) {
						e.printStackTrace();
						return new RPCResult<Void>(false, "Couldn't move a file");
					}
				} else {
					return new RPCResult<Void>(false, createMessageFileInUse(authenticationToken, newFilePath));
				}
			} else {
				return new RPCResult<Void>(false, createMessageFileInUse(authenticationToken, filePath));
			}
		} 
		return new RPCResult<Void>(false, "Permission denied");		
	}

	@Override
	public RPCResult<String> getParent(AuthenticationToken authenticationToken, String filePath) {
		RPCResult<Boolean> permissionResult = filePermissionService.hasReadPermission(authenticationToken, filePath);
		if(!permissionResult.isSucceeded())
			return new RPCResult<String>(false, permissionResult.getMessage(), "");
		if(permissionResult.getData()) {
			File file = new File(filePath);
			File parentFile = file.getParentFile();
			return parentFile == null ? new RPCResult<String>(true, "", null) : new RPCResult<String>(true, "", parentFile.getAbsolutePath());
		}
		return new RPCResult<String>(false, "Permission denied", "");
	}

	@Override
	public RPCResult<String> getFileName(AuthenticationToken authenticationToken, String filePath) {
		RPCResult<Boolean> permissionResult = filePermissionService.hasReadPermission(authenticationToken, filePath);
		if(!permissionResult.isSucceeded())
			return new RPCResult<String>(false, permissionResult.getMessage(), "");
		if(permissionResult.getData()) {
			File file = new File(filePath);
			return new RPCResult<String>(true, "", file.getName());
		}
		return new RPCResult<String>(false, "Permission denied", "");
	}

	@Override
	public RPCResult<Void> copyFiles(AuthenticationToken authenticationToken, String source, String destination) {
		RPCResult<Boolean> permissionResultSource = filePermissionService.hasReadPermission(authenticationToken, source);
		RPCResult<Boolean> permissionResultDestination = filePermissionService.hasWritePermission(authenticationToken, destination);
		if(!permissionResultSource.isSucceeded() || !permissionResultDestination.isSucceeded())
			return new RPCResult<Void>(false, permissionResultSource.getMessage());
		if(permissionResultSource.getData() && permissionResultDestination.getData()) {
			try {
				FileUtils.copyDirectory(new File(source), new File(destination));
			} catch(Exception e) {
				e.printStackTrace();
				return new RPCResult<Void>(false, e.getMessage());
			} 
			/*File sourceDir = new File(source);
			for(File sourceFile : sourceDir.listFiles()) {
				File destinationFile = new File(destination, sourceFile.getName());
				try {
					
					Files.copy(sourceFile, destinationFile);
				} catch(Exception e) {
					e.printStackTrace();
					return new RPCResult<Void>(false, e.getMessage());
				}
			}*/
			return new RPCResult<Void>(true);
		}
		return new RPCResult<Void>(false, "Permission denied");
	}	

	@Override
	public RPCResult<String> getDownloadPath(AuthenticationToken authenticationToken, String filePath) {
		RPCResult<Boolean> isDirectory = this.isDirectory(authenticationToken, filePath);
		if(filePath.startsWith("Share.") || filePath.equals("Root") || filePath.equals("Owned") || filePath.equals("Shared") || 
				isDirectory.isSucceeded() && isDirectory.getData()) {
			RPCResult<String> zipResult = this.zipDirectory(authenticationToken, filePath);
			if(!zipResult.isSucceeded())
				return new RPCResult<String>(false, zipResult.getMessage(), "");
			return zipResult;
		} else if(!isDirectory.isSucceeded()) {
			return new RPCResult<String>(false, isDirectory.getMessage(), "");
		} else {
			return new RPCResult<String>(true, "", filePath);
		}
	}

}

