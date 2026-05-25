alter table if exists public.user_profiles
    add column if not exists password_hash varchar(255);