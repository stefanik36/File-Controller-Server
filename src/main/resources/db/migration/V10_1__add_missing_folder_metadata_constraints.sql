DROP TABLE folder_metadata;

CREATE TABLE folder_metadata (
    folder_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    path_lower TEXT,
    path_display TEXT,
    parent_folder_id INTEGER(8),
    server_created_at TEXT,
    owner_id INTEGER,
    
    FOREIGN KEY(parent_folder_id) REFERENCES folder_metadata(folder_id),
    FOREIGN KEY(owner_id) REFERENCES users(id)
);
