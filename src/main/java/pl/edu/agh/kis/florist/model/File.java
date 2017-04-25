package pl.edu.agh.kis.florist.model;

import pl.edu.agh.kis.florist.db.tables.pojos.FileMetadata;

public class File extends FileMetadata {

	public File(FileMetadata value) {
		super(value);
	}

	public File(Integer fileId, String name, String pathLower, String pathDisplay, Integer enclosingFolderId,
			Integer size, String serverCreatedAt, String serverChangedAt, Integer OwnerId) {
		super(fileId, name, pathLower, pathDisplay, enclosingFolderId, size, serverCreatedAt, serverChangedAt, OwnerId);
	}

	public File(String name, String pathLower, String pathDisplay, Integer enclosingFolderId, Integer size,
			String serverCreatedAt, String serverChangedAt, Integer OwnerId) {
		super(null, name, pathLower, pathDisplay, enclosingFolderId, size, serverCreatedAt, serverChangedAt, OwnerId);
	}

}
