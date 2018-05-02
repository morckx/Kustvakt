CREATE TABLE IF NOT EXISTS role (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name VARCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX role_index on role(name);

CREATE TABLE IF NOT EXISTS privilege (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name VARCHAR(20) NOT NULL,
  role_id INTEGER NOT NULL,
  FOREIGN KEY (role_id) 
  	REFERENCES role (id)
  	ON DELETE CASCADE
);

CREATE UNIQUE INDEX privilege_index on privilege(name, role_id);


CREATE TABLE IF NOT EXISTS user_group (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name VARCHAR(100) NOT NULL,
  status VARCHAR(100) NOT NULL,
  created_by VARCHAR(100) NOT NULL,
  deleted_by VARCHAR(100) DEFAULT NULL
);

CREATE INDEX user_group_index ON user_group(status);


CREATE TABLE IF NOT EXISTS user_group_member (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id VARCHAR(100) NOT NULL,
  group_id INTEGER NOT NULL,
  status VARCHAR(100) NOT NULL,
  created_by VARCHAR(100) NOT NULL,
  deleted_by VARCHAR(100) DEFAULT NULL,
-- interprets now as localtime and save it as UTC
  status_date timestamp DEFAULT (datetime('now','localtime')),
  FOREIGN KEY (group_id) 
  	REFERENCES user_group (id)
  	ON DELETE CASCADE
); 

CREATE UNIQUE INDEX  user_group_member_index 
	ON user_group_member(user_id,group_id);
CREATE INDEX user_group_member_status_index 
	ON user_group_member(status);

CREATE TABLE IF NOT EXISTS group_member_role (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  group_member_id INTEGER NOT NULL,
  role_id INTEGER NOT NULL,
  FOREIGN KEY (group_member_id)
  	REFERENCES user_group_member (id)
  	ON DELETE CASCADE,
  FOREIGN KEY (role_id) 
  	REFERENCES role (id)
  	ON DELETE CASCADE
);

CREATE UNIQUE INDEX group_member_role_index 
	ON group_member_role(group_member_id,role_id);


CREATE TABLE IF NOT EXISTS virtual_corpus (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name VARCHAR(255) NOT NULL,
  type VARCHAR(100) NOT NULL,
  required_access VARCHAR(100) NOT NULL,
  created_by VARCHAR(100) NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  status VARCHAR(100) DEFAULT NULL,
  corpus_query TEXT NOT NULL,
  definition VARCHAR(255) DEFAULT NULL
);

CREATE INDEX virtual_corpus_owner_index ON virtual_corpus(created_by);
CREATE INDEX virtual_corpus_type_index ON virtual_corpus(type);

CREATE TABLE IF NOT EXISTS virtual_corpus_access (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  virtual_corpus_id INTEGER NOT NULL,
  user_group_id INTEGER NOT NULL,
  status VARCHAR(100) NOT NULL,
  created_by VARCHAR(100) NOT NULL,
  approved_by VARCHAR(100) DEFAULT NULL,
  deleted_by VARCHAR(100) DEFAULT NULL,
  FOREIGN KEY (user_group_id) 
  	REFERENCES user_group (id)
  	ON DELETE CASCADE,
  FOREIGN KEY (virtual_corpus_id) 
  	REFERENCES virtual_corpus (id)
  	ON DELETE CASCADE
);

CREATE INDEX virtual_corpus_status_index 
	ON virtual_corpus_access(status);
CREATE INDEX virtual_corpus_access_unique_index 
	ON virtual_corpus_access(virtual_corpus_id,user_group_id);

