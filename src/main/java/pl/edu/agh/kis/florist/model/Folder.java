package pl.edu.agh.kis.florist.model;

import pl.edu.agh.kis.florist.db.tables.pojos.FolderMetadata;

public class Folder extends FolderMetadata {

	public Folder(FolderMetadata value) {
		super(value);
	}

	public Folder(Integer folderId, String name, String pathLower, String pathDisplay, Integer parentFolderId,
			String serverCreatedAt, Integer ownerId) {
		super(folderId, name, pathLower, pathDisplay, parentFolderId, serverCreatedAt, ownerId);
	}
	

	public Folder(String name, String pathLower, String pathDisplay, Integer parentFolderId,
			String serverCreatedAt, Integer ownerId) {
		super(null, name, pathLower, pathDisplay, parentFolderId, serverCreatedAt, ownerId);
	}
	
	public Folder withParentFolderId(Integer parentFolderId) {
		//WITHOUT EXCEPTION IllegalStateException
		return new Folder(getFolderId(),getName(),getPathLower(),getPathDisplay(),getParentFolderId(), getServerCreatedAt(), getOwnerId());
	}

}
