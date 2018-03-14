CREATE TABLE IF NOT EXISTS admin(
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	user_id varchar(100) NOT NULL
);

CREATE UNIQUE INDEX admin_index on admin(user_id);