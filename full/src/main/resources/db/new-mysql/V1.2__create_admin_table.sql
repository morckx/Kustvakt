CREATE TABLE IF NOT EXISTS admin (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	user_id varchar(100) NOT NULL,
	UNIQUE INDEX unique_index (user_id)
);