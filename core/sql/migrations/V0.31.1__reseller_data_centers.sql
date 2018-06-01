UPDATE data_center SET description='iad2' where data_center_id=2;

CREATE TABLE reseller (
  reseller_id   VARCHAR(255) PRIMARY KEY,
  description	VARCHAR(255)
);

INSERT INTO reseller (reseller_id, description) VALUES
  ('525848', 'Heart Internet'),
  ('525847', 'Host Europe GmbH'),
  ('525845', 'Domain Factory GmbH'),
  ('525844', '123 Reg'),
  ('495469', 'Media Temple');


CREATE TABLE reseller_data_centers (
  id                SERIAL PRIMARY KEY,
  reseller_id       VARCHAR(255) NOT NULL REFERENCES reseller(reseller_id),
  data_center_id    INT NOT NULL REFERENCES data_center(data_center_id)
);

INSERT INTO reseller_data_centers (reseller_id, data_center_id) VALUES
  ('525848', 4),
  ('525847', 4),
  ('525845', 4),
  ('525844', 4),
  ('495469', 1);

