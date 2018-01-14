drop table if exists "groups";
drop table if exists "timers";

create table "groups" (
  id SERIAL PRIMARY KEY,
  name VARCHAR(24) UNIQUE NOT NULL,
  admins Int4[] NOT NULL ,
  members Int4[] NOT NULL
);

create table "timers" (
  id SERIAL PRIMARY KEY,
  "user" Int4 NOT NULL,
  system VARCHAR(12) NOT NULL,
  planet VARCHAR(6) NOT NULL,
  moon Int4,
  owner VARCHAR(64) NOT NULL,
  time TIMESTAMP NOT NULL,
  visibility Int4[] NOT NULL,
  type VARCHAR(32) NOT NULL
);

select * from groups;
delete from groups where 1=1;

