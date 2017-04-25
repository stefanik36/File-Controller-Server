package pl.edu.agh.kis.florist.model;

import pl.edu.agh.kis.florist.db.tables.pojos.FileContents;

public class FileContent extends FileContents{

	public FileContent(FileContents value) {
		super(value);
	}

	public FileContent(Integer fileId, byte[] contents) {
		super(fileId, contents);
	}

}
