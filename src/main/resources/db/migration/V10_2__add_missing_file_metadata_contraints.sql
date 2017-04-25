DROP TABLE file_metadata;

CREATE TABLE file_metadata (
    file_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    path_lower TEXT,
    path_display TEXT,
    enclosing_folder_id INTEGER(8),
    size INTEGER(8),
    server_created_at TEXT,
    server_changed_at TEXT,
    owner_id INTEGER,
    
    FOREIGN KEY(enclosing_folder_id) REFERENCES folder_metadata(folder_id),
    FOREIGN KEY(owner_id) REFERENCES users(id)
);
