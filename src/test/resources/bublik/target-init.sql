create extension hstore;
create schema if not exists test;
create type mood AS ENUM ('sad', 'ok', 'happy');
create type gender AS ENUM ('male', 'female', 'NA');
create table public.target (
    id int primary key generated always as identity,
    uuid uuid,
    "Primary" varchar(256),
    boolean boolean,
    int2 int2,
    int4 int4,
    int8 int8,
    smallint smallint,
    bigint bigint,
    num numeric,
    float8 float8,
    date date,
    timestamp timestamp,
    timestamptz timestamptz,
    rem text,
    image bytea,
    current_mood mood,
    time time,
    j json,
    ip inet,
    h hstore,
    ints _int8,
    gender gender
);
